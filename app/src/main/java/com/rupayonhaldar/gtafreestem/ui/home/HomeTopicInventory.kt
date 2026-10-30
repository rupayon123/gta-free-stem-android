package com.rupayonhaldar.gtafreestem.ui.home

/** Use only available categories, in the shipped iOS preference order; never fabricate topics. */
internal fun availableHomeTopics(categories: Collection<String>): List<String> {
    val available = categories.filter(String::isNotBlank).toSet()
    val preferred = listOf("Science & Engineering", "Coding & Robotics", "Makerspace & Fabrication",
        "Arts & Media", "Family Learning", "Career & Mentorship")
    return (preferred.filter(available::contains) + available.filterNot(preferred::contains).sorted()).take(6)
}
