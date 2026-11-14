package com.myiptv.clean.data.repository

/** Retain all real provider categories, including secondary Kids/Family labels. */
internal fun sourceChannelCategories(categories: List<String>?): List<String> =
    categories.orEmpty().map(String::trim).filter(String::isNotBlank).distinct()
