package com.spexco.nfcmifareclassiclibrary.helper

import com.spexco.nfcmifareclassiclibrary.model.SectorStatusModel


interface NfcMifareListener {
     fun onNfcIOState(status: Short)
     fun onNfcIOState(errState: Short, ex: Exception?)
     fun onReadNfcSectorStatus(sectorCount: Int, sectors: MutableList<SectorStatusModel>)
}