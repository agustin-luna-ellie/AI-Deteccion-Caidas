package io.fallcare.util

import android.content.Context

object Constants {

    inline val Context.ACTION_UPDATE_SETTINGS: String
        get() = "$packageName.ACTION_UPDATE_SETTINGS"

    const val NOTIFICATION_ID = 101
    const val FALL_DETECTED_DATA = "FALL_DETECTED"

    // Config Keys
    const val SETTINGS_KEY = "settings"
    const val PROB_FALL_KEY = "prob_fall"
    const val SAMPLING_PERIOD_KEY = "sampling_period"
    const val SEQUENCE_LENGTH_KEY = "sequence_length"


}

