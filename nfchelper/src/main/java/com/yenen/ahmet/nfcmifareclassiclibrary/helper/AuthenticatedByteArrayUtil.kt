package com.yenen.ahmet.nfcmifareclassiclibrary.helper

import android.nfc.tech.MifareClassic
import java.util.ArrayList

object AuthenticatedByteArrayUtil {
    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    private val KEY_1 = byteArrayOfInts(0x33, 0x33, 0x33, 0x33, 0x33, 0x33)


    private val KEY_LIST = ArrayList<ByteArray>()

    val keyList: List<ByteArray>
        get() {
            KEY_LIST.add(MifareClassic.KEY_DEFAULT)
            KEY_LIST.add(MifareClassic.KEY_NFC_FORUM)
            KEY_LIST.add(MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY)
            KEY_LIST.add(KEY_1)
            return KEY_LIST
        }

}