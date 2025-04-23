package io.fallcare.data

import io.fallcare.util.appTimeStamp

data class FallModel(
    val timeStamp: Long = appTimeStamp,
    val x: Float,
    val y: Float,
    val z: Float,
)
