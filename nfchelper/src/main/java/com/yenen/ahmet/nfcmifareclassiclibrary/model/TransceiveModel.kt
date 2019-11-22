package com.yenen.ahmet.nfcmifareclassiclibrary.model

import java.lang.Exception

data class TransceiveModel (
    val status: Boolean,
    var response: ByteArray? = null,
    var errorCode: ByteArray? = null,
    var errorReason: String? = null,
    var exception: Exception? = null
)