package com.spexco.nfcmifareclassiclibrary.helper

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import com.spexco.nfcmifareclassiclibrary.model.SectorStatusModel
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.charset.Charset

class NfcMifareClassicIO constructor(
    private var listener: NfcMifareListener? = null,
    private val charset: Charset,
    private val tag: Tag
) {

    // Connection status
    val MIFARE_CONNECTION_CONNECTED: Short = 10
    val MIFARE_CONNECTION_NOT_CONNECTED: Short = -10
    // Error Message Type //
    val MIFARE_TAG_TECHNOLOGY_NOT_FOUND: Short = 1
    val MIFARE_CONNECT_IO_EXCEPTION: Short = 2
    val MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_READ: Short = 3
    val MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE: Short = 4
    val MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE_FALSE: Short = 5
    val MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE_BYTE_SIZE_LARGE_FROM_16: Short = 6

    private fun getControlMifare(): MifareClassic? {
        val mifareClassic = MifareClassic.get(tag)

        if (mifareClassic == null) {
            listener?.onNfcIOState(MIFARE_TAG_TECHNOLOGY_NOT_FOUND, null)
            return null
        }
        return mifareClassic
    }

    fun writeTag(sectorIndex: Int, text: String) {
        getControlMifare()?.use {
            try {
                it.connect()
            } catch (ex: IOException) {
                listener?.onNfcIOState(MIFARE_CONNECT_IO_EXCEPTION, ex)
            }

            if (it.isConnected) {
                listener?.onNfcIOState(MIFARE_CONNECTION_CONNECTED)
                try {
                    writeMifare(it, sectorIndex, text)
                } catch (ex: IOException) {
                    listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE, ex)
                }
            } else {
                listener?.onNfcIOState(MIFARE_CONNECTION_NOT_CONNECTED)
            }
        }
    }

    private fun writeMifare(mifareClassic: MifareClassic, sectorIndex: Int, text: String) {
        var isAuthenticated = false

        if (mifareClassic.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_DEFAULT)) {
            isAuthenticated = true
        } else if (mifareClassic.authenticateSectorWithKeyA(sectorIndex, MifareClassic.KEY_NFC_FORUM)) {
            isAuthenticated = true
        }
        if (isAuthenticated) {
            charset.also { charset ->
                val data = text.toByteArray(charset)
                val block = mifareClassic.sectorToBlock(sectorIndex)
                try {
                    mifareClassic.writeBlock(block, data)
                } catch (ex: IllegalArgumentException) {
                    listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE_BYTE_SIZE_LARGE_FROM_16, ex)
                }
            }
        } else {
            listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE_FALSE, null)
        }
    }

    fun readTag() {
        getControlMifare()?.use {
            try {
                it.connect()
            } catch (ex: IOException) {
                listener?.onNfcIOState(MIFARE_CONNECT_IO_EXCEPTION, ex)
            }

            if (it.isConnected) {
                listener?.onNfcIOState(MIFARE_CONNECTION_CONNECTED)
                try {
                    readMifare(it)
                } catch (ex: IOException) {
                    listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_READ, ex)
                }
            } else {
                listener?.onNfcIOState(MIFARE_CONNECTION_NOT_CONNECTED)
            }
        }
    }

    private fun readMifare(mifare: MifareClassic) {
        val lengthSector = mifare.getSectorCount()
        val statusOfSectors = mutableListOf<SectorStatusModel>()

        for (i in 0 until lengthSector) {
            var isAuthenticated = false

            if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)) {
                isAuthenticated = true
            } else if (mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_NFC_FORUM)) {
                isAuthenticated = true
            } else {
                Log.e("NfcMifareClassicIO", "autkey not found ")
            }

            if (isAuthenticated) {
                val index = mifare.sectorToBlock(i)
                val readBlock = mifare.readBlock(index)
                val text = String(readBlock, charset)
                val sectorStatusModel = SectorStatusModel(index,text)
                statusOfSectors.add(sectorStatusModel)
            }
        }
        listener?.onReadNfcSectorStatus(lengthSector, statusOfSectors)
    }

    fun unBind() {
        listener = null
    }

}