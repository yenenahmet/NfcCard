package com.yenen.ahmet.nfcmifareclassiclibrary.helper.isodesp

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.yenen.ahmet.nfcmifareclassiclibrary.model.TransceiveModel
import java.lang.Exception
import kotlin.experimental.and

class IsoReader(private val tag: Tag) : AutoCloseable {

    private var isoDep: IsoDep? = IsoDep.get(tag)

    override fun close() {
        isoDep?.close()
    }

    fun connect() {
        isoDep?.connect()
    }

    fun setTimeOut(timeOut: Int) {
        isoDep?.timeout = timeOut
    }

    fun transceive(command: ByteArray): TransceiveModel {
        if (isoDep != null) {
            return try {
                val results = isoDep?.transceive(command)
                val size = results!!.size
                if (results[size - 2] == 0x90.toByte() && results[size - 1] == 0x00.toByte()) {
                    val response = ByteArray(results.size - 2)
                    System.arraycopy(results, 0, response, 0, results.size - 2)
                    TransceiveModel(true, response, null, "", null)
                } else {
                    val errCode = ByteArray(2)
                    errCode[0] = results[size - 2]
                    errCode[1] = results[size - 1]
                    TransceiveModel(false, null, errCode, "Transceive Result Error", null)
                }
            } catch (ex: Exception) {
                TransceiveModel(false, null, null, ex.toString(), ex)
            }
        } else {
            return TransceiveModel(false, null, null, "TECHNOLOGY_NOT_FOUND", null)
        }
    }

    fun selectFile(keyId: ByteArray, p1: Byte, p2: Byte): TransceiveModel {
        val command = ByteArray(5 + keyId.size)
        command[0] = 0x00.toByte()
        command[1] = 0xA4.toByte()
        command[2] = p1
        command[3] = p2
        command[4] = keyId.size.toByte()
        System.arraycopy(keyId, 0, command, 5, keyId.size)
        return transceive(command)
    }

    fun getChallange(): TransceiveModel {
        val command = ByteArray(5)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0x84.toByte() // INS Instruction
        command[2] = 0x00.toByte() // P1  Parameter 1
        command[3] = 0x00.toByte() // P2  Parameter 2
        command[4] = 0x08.toByte() // Length
        return transceive(command)
    }

    fun externalAuthenticate(keyId: Byte, pin: ByteArray): TransceiveModel {
        val command = ByteArray(13)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0x82.toByte() // INS Instruction
        command[2] = 0x00.toByte() // P1  Parameter 1
        command[3] = keyId
        command[4] = pin.size.toByte() // Length
        System.arraycopy(pin, 0, command, 5, 8)
        return transceive(command)
    }

    fun verifyPin(keyId: Byte, password: ByteArray): TransceiveModel {
        val command = ByteArray(13)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0X20.toByte() // INS Instruction
        command[2] = 0x00.toByte() // P1  Parameter 1
        command[3] = keyId
        command[4] = password.size.toByte() // Length
        System.arraycopy(password, 0, command, 5, 8)
        return transceive(command)
    }

    fun updateBinary(offset: Short, data: ByteArray): TransceiveModel {
        val command = ByteArray(5 + data.size)
        val offsets = getOffset(offset)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0XD6.toByte() // INS Instruction
        command[2] = offsets[1]
        command[3] = offsets[0]
        command[4] = data.size.toByte()
        System.arraycopy(data, 0, command, 5, data.size)
        return transceive(command)
    }

    fun readBinary(offset: Short, le: Byte): TransceiveModel {
        val command = ByteArray(5)
        val offsets = getOffset(offset)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0XB0.toByte() // INS Instruction
        command[2] = offsets[1]
        command[3] = offsets[0]
        command[4] = le
        return transceive(command)
    }

    fun changeReferenceData(
        p1Auth: Byte,
        p2KeyId: Byte,
        dataPinValues: ByteArray
    ): TransceiveModel {
        val command = ByteArray(5 + dataPinValues.size)
        command[0] = 0x00.toByte() // CLA Class
        command[1] = 0X24.toByte() // INS Instruction
        command[2] = p1Auth
        command[3] = p2KeyId
        command[4] = dataPinValues.size.toByte()
        System.arraycopy(dataPinValues, 0, command, 5, dataPinValues.size)
        return transceive(command)
    }

    fun getOffset(offsetU: Short): ByteArray {
        val retVal = ByteArray(2)
        retVal[0] = (offsetU and 0xFF).toByte()
        retVal[1] = (offsetU and 0xFF00.ushr(8)).toByte()
        return retVal
    }


    fun selectEfReadBinary(
        dfFileKey: ByteArray,
        dfP1: Byte,
        dfP2: Byte,
        autKey: Byte,
        autCrypticPin: ByteArray,
        verifyKey: Byte,
        verifyPin: ByteArray,
        efFileKey: ByteArray,
        efP1: Byte,
        efP2: Byte,
        offset: Short,
        le: Byte
    ): TransceiveModel? {
        isoDep?.use {
            it.connect()
            if (it.isConnected) {
                val selectDfFile = selectFile(dfFileKey, dfP1, dfP2)
                if (!selectDfFile.status) {
                    return selectDfFile
                }
                val aut = externalAuthenticate(autKey, autCrypticPin)
                if (!aut.status) {
                    return aut
                }
                val verify = verifyPin(verifyKey, verifyPin)
                if (!verify.status) {
                    return verify
                }
                val selectEfFile = selectFile(efFileKey, efP1, efP2)
                if (!selectEfFile.status) {
                    return selectEfFile
                }
                return readBinary(offset, le)
            }
        }
        return null
    }
}