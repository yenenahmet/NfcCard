package com.yenen.ahmet.nfccard

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.yenen.ahmet.nfcmifareclassiclibrary.base.BaseNfcMifareClassicActivity
import com.yenen.ahmet.nfcmifareclassiclibrary.helper.NfcMifareClassicIO
import com.yenen.ahmet.nfcmifareclassiclibrary.helper.NfcMifareListener
import com.yenen.ahmet.nfcmifareclassiclibrary.model.SectorStatusModel
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

class MainActivity : BaseNfcMifareClassicActivity(), NfcMifareListener {


    private var nfcMifareClassicIO: NfcMifareClassicIO? = null

    override fun onNfcStatus(status: Boolean?) {
        status?.let {

            Log.e("nfcStatus", status.toString())
        }
    }

    override fun onReadTag(techList: List<String>, tag: Tag) {
        for (value in techList) {
            Log.e("value", value)
        }
        Toast.makeText(this,"Okundu",Toast.LENGTH_LONG).show()
        nfcMifareClassicIO = NfcMifareClassicIO(this, Charset.forName("US-ASCII"), tag)
    }

    override fun onNfcIOState(status: Short) {

    }

    override fun onNfcIOState(errState: Short, ex: Exception?) {
        Toast.makeText(this,"Err Message : $errState",Toast.LENGTH_LONG).show()
        Log.e("Error", errState.toString())
        if (ex != null) {
            Log.e("Error ex", ex.toString())
        }
    }

    override fun onReadNfcSectorStatus(sectorCount: Int, sectors: MutableList<SectorStatusModel>) {
        text.text = ""
        sectors.forEach {
            val message = it.message
            val index = it.sectorToBlockIndexList
            val hex = nfcMifareClassicIO?.toHex(it.byte)
            text.append(" Block Index = {$index} \n Message = {$message}  \n Hex = {$hex} \n\n ")
        }
    }

    override fun onReadNdefMessage(text: String) {
        Log.e("ReadText = ", "Text = $text")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnWrite.setOnClickListener { v ->
            nfcMifareClassicIO?.writeTag(sectorIndex.text.toString().toInt(),blockIndex.text.toString().toInt(), Edittext.text.toString())
        }

        readbutton.setOnClickListener { v -> nfcMifareClassicIO?.readTag() }

        reset.setOnClickListener { nfcMifareClassicIO?.resetAllSector()  }
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcMifareClassicIO?.unBind()
        nfcMifareClassicIO = null
    }
}
