package com.yenen.ahmet.nfccard

import android.nfc.Tag
import android.os.Bundle
import android.util.Log
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
        nfcMifareClassicIO = NfcMifareClassicIO(this, Charset.forName("US-ASCII"), tag)
    }

    override fun onNfcIOState(status: Short) {

    }

    override fun onNfcIOState(errState: Short, ex: Exception?) {
        Log.e("Error", errState.toString())
        if (ex != null) {
            Log.e("Error ex", ex.toString())
        }
    }

    override fun onReadNfcSectorStatus(sectorCount: Int, sectors: MutableList<SectorStatusModel>) {
        sectors.forEach {
            val message = it.message
            val index = it.sectorToBlockIndexList
            Log.e("ReadText = ", "Text = {$message} Sector Index = $index")
        }
    }

    override fun onReadNdefMessage(text: String) {
        Log.e("ReadText = ", "Text = $text")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnWrite.setOnClickListener({ v ->
            nfcMifareClassicIO?.writeTag(10, Edittext.getText().toString())
        })

        readbutton.setOnClickListener({ v -> nfcMifareClassicIO?.readTag() })
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcMifareClassicIO?.unBind()
        nfcMifareClassicIO = null
    }
}
