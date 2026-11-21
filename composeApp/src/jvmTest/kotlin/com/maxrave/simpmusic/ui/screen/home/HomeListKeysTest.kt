package com.maxrave.simpmusic.ui.screen.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeListKeysTest {
    @Test
    fun dedupeListKeysPreservesUniqueKeysAndDisambiguatesDuplicatesAndEmpties() {
        val input = listOf("id_a", "id_b", "id_a", "", "", "id_a", "empty_3")
        val result = dedupeListKeys(input)
        assertEquals(
            listOf("id_a", "id_b", "id_a#1", "empty_3", "empty_4", "id_a#2", "empty_3#1"),
            result,
        )
        assertEquals(result.size, result.toSet().size)
    }
}
