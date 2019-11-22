package com.yenen.ahmet.nfcmifareclassiclibrary.base

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.nio.charset.Charset

abstract class BaseNfcActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var nfcStateChangeBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            onNfcStatus(null)
            return
        }
        createBroadCast()
        if (nfcAdapter!!.isEnabled) {
            onNfcStatus(true)
            onReadNdefMessage(readFromIntent(intent))
        } else {
            onNfcStatus(false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)?.let {
            onReadTag(it.techList.toList(), it)
            onReadNdefMessage(readFromIntent(intent))
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException(e)
            }

            val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT)

            val intentFilters = arrayOf(ndef, tagDetected)
            it.enableForegroundDispatch(this, pendingIntent, intentFilters, null)

            val filterChanged = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            registerReceiver(nfcStateChangeBroadcastReceiver, filterChanged)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.let {
            nfcAdapter?.disableForegroundDispatch(this)
            unregisterReceiver(nfcStateChangeBroadcastReceiver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nfcAdapter = null
        nfcStateChangeBroadcastReceiver = null
    }

    private fun readFromIntent(intent: Intent): String {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action
            || NfcAdapter.ACTION_TECH_DISCOVERED == action
            || NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessage ->
                val message: List<NdefMessage> = rawMessage.map { it as NdefMessage }
                message[0].records[0].payload.let {
                    return String(it, Charset.forName("US-ASCII"))
                }
            }
        }
        return ""
    }

    private fun createBroadCast() {
        nfcStateChangeBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, -1)
                if (state == NfcAdapter.STATE_OFF)
                    onNfcStatus(false)
                else if (state == NfcAdapter.STATE_ON)
                    onNfcStatus(true)
            }
        }
    }

    protected abstract fun onReadTag(techList: List<String>, tag: Tag)

    protected abstract fun onReadNdefMessage(text: String) // "" -> bilinmeyen veya bulunamayan etiket

    protected abstract fun onNfcStatus(status: Boolean?) // Null Nfc Mecvut değil  // Aktif - Pasif

}
