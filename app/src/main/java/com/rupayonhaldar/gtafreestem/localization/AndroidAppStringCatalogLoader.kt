package com.rupayonhaldar.gtafreestem.localization

import android.content.Context
import com.rupayonhaldar.gtafreestem.R
import java.nio.charset.StandardCharsets

object AndroidAppStringCatalogLoader {
    fun load(context: Context): AppStringCatalog = context.resources
        .openRawResource(R.raw.app_strings)
        .bufferedReader(StandardCharsets.UTF_8)
        .use { reader -> AppStringCatalog.decode(reader.readText()) }
}
