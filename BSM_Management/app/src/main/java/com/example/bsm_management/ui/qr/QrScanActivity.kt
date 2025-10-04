package com.example.bsm_management.ui.qr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bsm_management.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrScanActivity : AppCompatActivity() {

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Hiển thị kết quả
            findViewById<TextView>(R.id.tvResult).text = "Kết quả: ${result.contents}"
        } else {
            findViewById<TextView>(R.id.tvResult).text = "Không quét được mã QR"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scan)

        // Bắt đầu quét QR ngay khi mở Activity
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Quét mã QR")
        options.setCameraId(0) // dùng camera sau
        options.setBeepEnabled(true)
        options.setOrientationLocked(false)
        barcodeLauncher.launch(options)
    }
}
