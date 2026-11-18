package com.maxrave.simpmusic.utils

import androidx.compose.runtime.Composable
import com.kmpalette.loader.NetworkLoader
import com.kmpalette.loader.rememberNetworkLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * Shared process-wide [HttpClient] for `kmpalette` thumbnail colour extraction.
 *
 * Constructing `HttpClient(CIO)` inline in a composable leaks a selector thread pool on every
 * recomposition and resets `rememberNetworkLoader` / `rememberDominantColorState` keys.
 */
val sharedPaletteHttpClient: HttpClient by lazy {
    HttpClient(CIO)
}

@Composable
fun rememberSharedPaletteNetworkLoader(): NetworkLoader = rememberNetworkLoader(sharedPaletteHttpClient)
