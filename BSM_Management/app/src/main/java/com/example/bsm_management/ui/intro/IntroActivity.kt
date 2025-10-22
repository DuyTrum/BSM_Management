package com.example.bsm_management.ui.intro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bsm_management.R
import com.example.bsm_management.ui.auth.LoginActivity
import com.google.android.material.button.MaterialButton

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra nếu đã xem intro -> bỏ qua luôn
        val prefs = getSharedPreferences("intro", MODE_PRIVATE)
        if (prefs.getBoolean("done", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        val items = listOf(
            IntroItem("Quản lý phòng trọ dễ dàng", "Tạo, sửa, xóa và xem tình trạng phòng chỉ trong vài giây.", R.drawable.ic_bsm_logo),
            IntroItem("Theo dõi hợp đồng", "Xem chi tiết hợp đồng và thông tin khách thuê mọi lúc.", R.drawable.ic_contract),
            IntroItem("Lập và nhắc hóa đơn", "Tự động nhắc hạn thanh toán giúp bạn không bỏ lỡ doanh thu.", R.drawable.ic_invoice)
        )
        viewPager.adapter = IntroAdapter(items)

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < items.size) {
                viewPager.currentItem++
            } else {
                finishIntro()
            }
        }

        btnSkip.setOnClickListener {
            finishIntro()
        }
    }

    private fun finishIntro() {
        getSharedPreferences("intro", MODE_PRIVATE)
            .edit().putBoolean("done", true).apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
