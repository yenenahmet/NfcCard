package com.yenen.ahmet.nfcmifareclassiclibrary.helper

import com.yenen.ahmet.nfcmifareclassiclibrary.model.SectorStatusModel


interface NfcMifareListener {
     fun onNfcIOState(status: Short)
     fun onNfcIOState(errState: Short, ex: Exception?)
     fun onReadNfcSectorStatus(sectorCount: Int, sectors: MutableList<SectorStatusModel>)
}