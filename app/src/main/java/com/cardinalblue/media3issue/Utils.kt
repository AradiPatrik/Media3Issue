package com.cardinalblue.media3issue

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.core.text.inSpans

inline fun SpannableStringBuilder.font(typeface: Typeface, builderAction: SpannableStringBuilder.() -> Unit) =
    inSpans(TypefaceSpan(typeface), builderAction = builderAction)