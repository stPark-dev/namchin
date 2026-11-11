package dev.stpark.namchin.ui.common

import androidx.annotation.StringRes
import dev.stpark.namchin.R
import dev.stpark.namchin.core.color.Filter
import dev.stpark.namchin.review.Look

/** 화면에 보이는 색감 이름. null = 원본. */
@StringRes
fun lookLabel(look: Look?): Int = when (look) {
    null -> R.string.look_original
    is Look.Reference -> R.string.color_match
    is Look.Preset -> filterLabel(look.filter)
}

@StringRes
fun filterLabel(filter: Filter): Int = when (filter) {
    Filter.CLEAR -> R.string.filter_clear
    Filter.COZY -> R.string.filter_cozy
    Filter.FILM -> R.string.filter_film
    Filter.SUNSET -> R.string.filter_sunset
    Filter.FRESH -> R.string.filter_fresh
    Filter.NIGHT -> R.string.filter_night
    Filter.MONO -> R.string.filter_mono
    Filter.VINTAGE -> R.string.filter_vintage
}
