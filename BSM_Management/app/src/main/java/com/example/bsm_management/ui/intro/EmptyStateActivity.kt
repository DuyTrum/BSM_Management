package com.example.bsm_management.ui.intro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import com.example.bsm_management.R
import com.example.bsm_management.ui.hostel.AddHostelActivity
import com.google.android.material.button.MaterialButton

class EmptyStateActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_state)

        val createTop = findViewById<MaterialButton>(R.id.btnCreateTop)
        val createBottom = findViewById<MaterialButton>(R.id.btnCreateBottom)
        val rowCall = findViewById<LinearLayout>(R.id.rowCall)
        val rowZalo = findViewById<LinearLayout>(R.id.rowZalo)
        val guide = findViewById<MaterialButton>(R.id.btnGuide)
        val hero = findViewById<MotionLayout>(R.id.heroMotion)
        hero.post {
            hero.progress = 0f
            hero.transitionToEnd()
        }
        hero.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionCompleted(layout: MotionLayout, currentId: Int) {
                // lặp mượt: lên xong thì về, về xong thì lên
                if (currentId == R.id.end) layout.postDelayed({ layout.transitionToStart() }, 600)
                else if (currentId == R.id.start) layout.postDelayed({ layout.transitionToEnd() }, 200)
            }
            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}
            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
        })
        val goCreate = {
            startActivity(Intent(this, AddHostelActivity::class.java))
        }

        createTop.setOnClickListener { goCreate() }
        createBottom.setOnClickListener { goCreate() }
        guide.setOnClickListener {
            // mở trang/FAQ hoặc hiển thị dialog – tạm mở web demo
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/huong-dan")))
        }

        rowCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0900000000")))
        }
        rowZalo.setOnClickListener {
            // mở Zalo chat theo link OA/phone, tạm mở web
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/")))
        }
    }
}
