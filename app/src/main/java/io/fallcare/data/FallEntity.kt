package io.fallcare.data

import io.fallcare.util.appTimeStamp

data class FallEntity(
    val data: List<FallModel>,
    val sequenceLength: Int,
    val samplingPeriod: Int,
    val probFall: Float
)
