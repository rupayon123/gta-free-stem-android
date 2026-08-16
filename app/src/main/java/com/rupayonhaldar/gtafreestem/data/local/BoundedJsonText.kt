package com.rupayonhaldar.gtafreestem.data.local

import java.nio.charset.StandardCharsets

/** Lightweight size/depth gate used before recursive JSON decoding or persistence. */
internal fun String.isSafeBoundedJson(
    maximumBytes: Int,
    maximumDepth: Int = 64,
): Boolean {
    if (maximumBytes <= 0 || maximumDepth <= 0 || length > maximumBytes) return false
    if (toByteArray(StandardCharsets.UTF_8).size > maximumBytes) return false

    var depth = 0
    var quoted = false
    var escaped = false
    forEach { character ->
        if (quoted) {
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == '"' -> quoted = false
            }
        } else {
            when (character) {
                '"' -> quoted = true
                '{', '[' -> {
                    depth += 1
                    if (depth > maximumDepth) return false
                }
                '}', ']' -> {
                    depth -= 1
                    if (depth < 0) return false
                }
            }
        }
    }
    return depth == 0 && !quoted && !escaped
}
