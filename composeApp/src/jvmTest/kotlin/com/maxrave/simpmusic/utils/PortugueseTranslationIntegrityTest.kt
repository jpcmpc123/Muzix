package com.maxrave.simpmusic.utils

import com.maxrave.common.SUPPORTED_LANGUAGE
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guardrail ensuring Portuguese (pt-BR) translations stay 100% complete, non-empty,
 * format-specifier-compatible, and properly resolved by SUPPORTED_LANGUAGE.
 */
class PortugueseTranslationIntegrityTest {
    private val formatRegex = Regex("""%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]""")

    private fun resolveExistingFile(vararg candidates: String): File =
        candidates.map(::File).firstOrNull { it.isFile }
            ?: error("Could not locate any of: ${candidates.joinToString()}")

    private data class ParsedResources(
        val strings: Map<String, Pair<String, Boolean>>,
        val plurals: Map<String, Map<String, String>>,
    )

    private fun parseXml(file: File): ParsedResources {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement
        val strings = linkedMapOf<String, Pair<String, Boolean>>()
        val plurals = linkedMapOf<String, Map<String, String>>()
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            when (node.tagName) {
                "string" -> {
                    val translatable = node.getAttribute("translatable") != "false"
                    strings[name] = node.textContent.orEmpty() to translatable
                }
                "plurals" -> {
                    val items = linkedMapOf<String, String>()
                    val children = node.childNodes
                    for (j in 0 until children.length) {
                        val item = children.item(j) as? Element ?: continue
                        if (item.tagName == "item") {
                            items[item.getAttribute("quantity")] = item.textContent.orEmpty()
                        }
                    }
                    plurals[name] = items
                }
            }
        }
        return ParsedResources(strings, plurals)
    }

    private fun formatSpecifiers(text: String): List<String> =
        formatRegex.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun composeAppPortugueseStringsAreCompleteAndValid() {
        val baseFile =
            resolveExistingFile(
                "src/commonMain/composeResources/values/strings.xml",
                "composeApp/src/commonMain/composeResources/values/strings.xml",
            )
        val ptFile =
            resolveExistingFile(
                "src/commonMain/composeResources/values-pt/strings.xml",
                "composeApp/src/commonMain/composeResources/values-pt/strings.xml",
            )

        val base = parseXml(baseFile)
        val pt = parseXml(ptFile)

        val missing =
            base.strings
                .filterValues { (_, translatable) -> translatable }
                .keys
                .filter { it !in pt.strings }
        assertTrue(missing.isEmpty(), "Missing Portuguese strings: $missing")

        val empty =
            pt.strings
                .filterValues { (value, _) -> value.isBlank() }
                .keys
                .toList()
        assertTrue(empty.isEmpty(), "Blank Portuguese strings: $empty")

        val formatMismatches =
            base.strings
                .filterValues { (_, translatable) -> translatable }
                .mapNotNull { (key, pair) ->
                    val ptVal = pt.strings[key]?.first ?: return@mapNotNull null
                    val baseSpecs = formatSpecifiers(pair.first)
                    val ptSpecs = formatSpecifiers(ptVal)
                    if (baseSpecs != ptSpecs) "$key: base=$baseSpecs pt=$ptSpecs" else null
                }
        assertTrue(formatMismatches.isEmpty(), "Format specifier mismatches: $formatMismatches")

        val missingPlurals = base.plurals.keys.filter { it !in pt.plurals }
        assertTrue(missingPlurals.isEmpty(), "Missing Portuguese plurals: $missingPlurals")

        val pluralIssues =
            base.plurals.flatMap { (key, baseItems) ->
                val ptItems = pt.plurals[key].orEmpty()
                listOf("one", "other").mapNotNull { qty ->
                    val ptText = ptItems[qty]
                    when {
                        ptText.isNullOrBlank() -> "$key[$qty] is blank"
                        formatSpecifiers(baseItems[qty].orEmpty()) != formatSpecifiers(ptText) ->
                            "$key[$qty] format mismatch"
                        else -> null
                    }
                }
            }
        assertTrue(pluralIssues.isEmpty(), "Portuguese plural issues: $pluralIssues")
    }

    @Test
    fun androidWidgetAndNotificationStringsHavePortugueseTranslations() {
        val baseFile =
            resolveExistingFile(
                "../androidApp/src/main/res/values/widget_strings.xml",
                "androidApp/src/main/res/values/widget_strings.xml",
            )
        val ptFile =
            resolveExistingFile(
                "../androidApp/src/main/res/values-pt/widget_strings.xml",
                "androidApp/src/main/res/values-pt/widget_strings.xml",
            )

        val base = parseXml(baseFile)
        val pt = parseXml(ptFile)

        val missing =
            base.strings
                .filterValues { (_, translatable) -> translatable }
                .keys
                .filter { key -> pt.strings[key]?.first.isNullOrBlank() }
        assertTrue(missing.isEmpty(), "Missing or blank Portuguese widget/notification strings: $missing")
    }

    @Test
    fun supportedLanguageResolvesPortugueseVariants() {
        assertEquals("pt-BR", SUPPORTED_LANGUAGE.resolveCode("pt-BR"))
        assertEquals("pt-BR", SUPPORTED_LANGUAGE.resolveCode("pt_BR"))
        assertEquals("pt-BR", SUPPORTED_LANGUAGE.resolveCode("pt-PT"))
        assertEquals("pt-BR", SUPPORTED_LANGUAGE.resolveCode("pt"))
        assertEquals("Português", SUPPORTED_LANGUAGE.getLanguageFromCode("pt-BR"))
        assertEquals("Português", SUPPORTED_LANGUAGE.getLanguageFromCode("pt-PT"))
        assertEquals("Português", SUPPORTED_LANGUAGE.getLanguageFromCode("pt"))
        assertEquals("iw-IL", SUPPORTED_LANGUAGE.resolveCode("he-IL"))
        assertEquals("id-ID", SUPPORTED_LANGUAGE.resolveCode("in-ID"))
        assertNull(SUPPORTED_LANGUAGE.resolveCode("zz-ZZ"))
    }
}
