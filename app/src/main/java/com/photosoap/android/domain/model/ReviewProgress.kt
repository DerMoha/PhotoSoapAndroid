package com.photosoap.android.domain.model

data class ReviewProgress(val reviewed: Int = 0, val total: Int = 0) {
    val isComplete: Boolean get() = total > 0 && reviewed == total
    val fraction: Float get() = if (total > 0) reviewed.toFloat() / total else 0f
    val displayFraction: Float get() = if (isComplete) 1f else fraction.coerceAtMost(0.99f)
}
