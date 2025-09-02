package com.sstek.jaoa.core

import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri

fun encodeUri(uri: Uri): String {
    return Base64.encodeToString(uri.toString().toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
}

fun decodeUri(encoded: String): Uri {
    return String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP)).toUri()
}
