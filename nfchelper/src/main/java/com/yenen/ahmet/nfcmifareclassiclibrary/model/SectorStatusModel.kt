package com.yenen.ahmet.nfcmifareclassiclibrary.model

data class  SectorStatusModel(
    val sectorIndex:Int,
    val sectorToBlockIndexList :Int,
    val message:String,
    val byte: ByteArray
)