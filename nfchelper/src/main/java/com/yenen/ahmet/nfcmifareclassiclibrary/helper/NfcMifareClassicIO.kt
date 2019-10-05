package com.yenen.ahmet.nfcmifareclassiclibrary.helper

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.yenen.ahmet.nfcmifareclassiclibrary.model.SectorStatusModel
import java.io.IOException
import java.nio.charset.Charset

// For 1k Classic //
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
    val MIFARE_AUTHENTICATE_WRITE_FALSE: Short = 5
    val MIFARE_WRITE_BYTE_SIZE_LARGE_FROM_16: Short = 6
    val MIFARE_WRITE_SECTOR_NOT_FOUND: Short = 7
    val MIFARE_WRITE_SECTOR_TRAILER_CANNOT_BE_WRITTEN_WITH_A_WRITE_TAG: Short = 8

    private fun getControlMifare(): MifareClassic? {
        val mifareClassic = MifareClassic.get(tag)

        if (mifareClassic == null) {
            listener?.onNfcIOState(MIFARE_TAG_TECHNOLOGY_NOT_FOUND, null)
            return null
        }
        return mifareClassic
    }

    fun writeTag(sectorIndex: Int, blockIndex: Int, text: String) {
        getControlMifare()?.use {
            try {
                it.connect()
            } catch (ex: IOException) {
                listener?.onNfcIOState(MIFARE_CONNECT_IO_EXCEPTION, ex)
            }

            if (it.isConnected) {
                listener?.onNfcIOState(MIFARE_CONNECTION_CONNECTED)
                try {
                    writeMifare(it, sectorIndex, blockIndex, text)
                } catch (ex: IOException) {
                    listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE, ex)
                }
            } else {
                listener?.onNfcIOState(MIFARE_CONNECTION_NOT_CONNECTED)
            }
        }
    }

    private fun writeMifare(
        mifareClassic: MifareClassic,
        sectorIndex: Int,
        blockIndex: Int,
        text: String
    ) {
        if (sectorIndex > mifareClassic.sectorCount) {
            listener?.onNfcIOState(MIFARE_WRITE_SECTOR_NOT_FOUND, null)
            return
        }

        if (isAuthenticated(mifareClassic, sectorIndex)) {
            val data = text.toByteArray(charset)
            val index = mifareClassic.sectorToBlock(sectorIndex)
            if (blockIndex == index + 3) {
                listener?.onNfcIOState(
                    MIFARE_WRITE_SECTOR_TRAILER_CANNOT_BE_WRITTEN_WITH_A_WRITE_TAG,
                    null
                )
            } else {
                try {

                    mifareClassic.writeBlock(blockIndex, data)
                } catch (ex: IllegalArgumentException) {
                    listener?.onNfcIOState(MIFARE_WRITE_BYTE_SIZE_LARGE_FROM_16, ex)
                }
            }
        } else {
            listener?.onNfcIOState(MIFARE_AUTHENTICATE_WRITE_FALSE, null)
        }
    }


    private fun writeSectorTrailer(
        sectorIndex: Int,
        byteData: ByteArray
    ) {
        getControlMifare()?.use {
            try {
                it.connect()
            } catch (ex: IOException) {
                listener?.onNfcIOState(MIFARE_CONNECT_IO_EXCEPTION, ex)
            }

            if (it.isConnected) {
                listener?.onNfcIOState(MIFARE_CONNECTION_CONNECTED)
                try {
                    writeSectorTrailerControl(it, sectorIndex, byteData)
                } catch (ex: IOException) {
                    listener?.onNfcIOState(MIFARE_AUTHENTICATE_SECTOR_WINTH_KEY_A_WRITE, ex)
                }
            } else {
                listener?.onNfcIOState(MIFARE_CONNECTION_NOT_CONNECTED)
            }
        }
    }


    private fun writeSectorTrailerControl(
        mifareClassic: MifareClassic,
        sectorIndex: Int,
        byteData: ByteArray
    ) {
        if (sectorIndex > mifareClassic.sectorCount) {
            listener?.onNfcIOState(MIFARE_WRITE_SECTOR_NOT_FOUND, null)
            return
        }

        if (isAuthenticated(mifareClassic, sectorIndex)) {
            val index = mifareClassic.sectorToBlock(sectorIndex)
            try {
                mifareClassic.writeBlock(index + 3, byteData)
            } catch (ex: IllegalArgumentException) {
                listener?.onNfcIOState(MIFARE_WRITE_BYTE_SIZE_LARGE_FROM_16, ex)
            }

        } else {
            listener?.onNfcIOState(MIFARE_AUTHENTICATE_WRITE_FALSE, null)
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
                readMifare(it)

            } else {
                listener?.onNfcIOState(MIFARE_CONNECTION_NOT_CONNECTED)
            }
        }
    }

    private fun readMifare(mifare: MifareClassic) {
        val lengthSector = mifare.sectorCount
        val statusOfSectors = mutableListOf<SectorStatusModel>()

        for (i in 0 until lengthSector) {

            if (isAuthenticated(mifare, i)) {
                val index = mifare.sectorToBlock(i)
                val blockCount = mifare.getBlockCountInSector(i)
                for (j in 0 until blockCount) {
                    var readBlock :ByteArray = byteArrayOf()
                    try {
                        readBlock = mifare.readBlock(index + j)
                    }catch (ex :Exception){
                        // last Block With b extra kontrol //
                    }

                    val text = String(readBlock, charset)
                    val sectorStatusModel = SectorStatusModel(i, index + j, text, readBlock)
                    statusOfSectors.add(sectorStatusModel)
                }

            }
        }
        listener?.onReadNfcSectorStatus(lengthSector, statusOfSectors)
    }

    fun unBind() {
        listener = null
    }


    private fun isAuthenticated(mifareClassic: MifareClassic, sectorIndex: Int): Boolean {
        val list = AuthenticatedByteArrayUtil.keyList
        list.forEach {
            try {
                if (mifareClassic.authenticateSectorWithKeyA(sectorIndex, it)) {
                    return true
                }
            } catch (ex: Exception) {
            }
        }
        list.forEach {
            try {
                if (mifareClassic.authenticateSectorWithKeyB(sectorIndex, it)) {
                    return true
                }
            } catch (ex: Exception) {
            }
        }
        return false
    }


    fun toHex(byteArray: ByteArray): String {
        val result = StringBuffer()
        val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()
        byteArray.forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS_ARRAY[firstIndex])
            result.append(HEX_CHARS_ARRAY[secondIndex])
        }
        return result.toString()
    }


    // 1 K //
    fun resetAllSector() {
        for (i in 0 until 16) {
            for (j in 0 until 3) {
                writeTag(i, (i * 4) + j, "0000000000000000")
            }
        }
    }

}