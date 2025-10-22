package com.example.bsm_management.ui.hostel

import android.Manifest
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.example.bsm_management.R
import com.example.bsm_management.ui.main.MainActivity
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import database.DatabaseHelper
import java.util.Locale

class AddHostelActivity : AppCompatActivity() {

    // ======= Views bước 1 =======
    private lateinit var edtSampleRoom: TextInputEditText
    private lateinit var edtArea: TextInputEditText
    private lateinit var edtPrice: TextInputEditText
    private lateinit var edtInvoiceDay: TextInputEditText
    private lateinit var edtDueDays: TextInputEditText
    private lateinit var ddlMaxPeople: android.widget.AutoCompleteTextView
    private lateinit var switchAuto: MaterialSwitch
    private lateinit var btnClose: MaterialButton
    private lateinit var btnNext: MaterialButton

    // progress + containers
    private lateinit var progress: LinearProgressIndicator
    private lateinit var step1: View
    private lateinit var step2: View

    private val REQ_LOC = 1001
    private var isOnStep2 = false
    private var step2Wired = false // <-- chỉ wire step2 1 lần khi đã hiện

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hostel)

        // Top bar back
        findViewById<MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Bước 1
        edtSampleRoom = findViewById(R.id.edtSampleRoom)
        edtArea       = findViewById(R.id.edtArea)
        edtPrice      = findViewById(R.id.edtPrice)
        edtInvoiceDay = findViewById(R.id.edtInvoiceDay)
        edtDueDays    = findViewById(R.id.edtDueDays)
        ddlMaxPeople  = findViewById(R.id.ddlMaxPeople)
        switchAuto    = findViewById(R.id.switchAuto)

        // Bottom bar
        btnClose      = findViewById(R.id.btnClose)
        btnNext       = findViewById(R.id.btnNext)
        progress      = findViewById(R.id.progress)

        // Containers
        step1 = findViewById(R.id.step1Container)
        step2 = findViewById(R.id.step2Container)

        // dropdown “Tối đa người ở / phòng”
        ddlMaxPeople.setAdapter(
            ArrayAdapter(
                this, android.R.layout.simple_list_item_1,
                listOf("Không giới hạn","1","2","3","4","5","6","7","8")
            )
        )

        btnClose.isEnabled = false  // giống ảnh mẫu bước 1
        btnClose.setOnClickListener {
            if (isOnStep2) {
                // quay lại bước 1
                animateProgress(from = 100, to = 40)
                crossfade(step2, step1)
                isOnStep2 = false
                btnClose.isEnabled = false
                btnNext.text = "Tiếp theo"
            } else {
                finish()
            }
        }
        btnNext.setOnClickListener { onNextClicked() }

        // KHÔNG wire step2 ở đây để tránh NPE khi layout (hoặc ID) chưa khớp
    }

    // ================== FLOW BƯỚC 1 -> 2 ==================
    private fun onNextClicked() {
        if (!isOnStep2) {
            // Validate nhanh nếu tạo tự động
            if (switchAuto.isChecked) {
                val count = edtSampleRoom.text?.toString()?.toIntOrNull() ?: 0
                val price = edtPrice.text?.toString()?.toIntOrNull() ?: 0
                if (count <= 0) { toast("Nhập số lượng phòng mẫu > 0"); return }
                if (price <= 0) { toast("Nhập giá thuê mẫu > 0"); return }
            }
            // progress trượt 40 -> 100 + chuyển layout
            animateProgress(from = 40, to = 100)
            crossfade(step1, step2)
            isOnStep2 = true
            btnClose.isEnabled = true
            btnNext.text = "Lưu thông tin"

            wireStep2() // <-- chỉ wire lúc này, tránh crash
        } else {
            // Bước 2: lưu rooms-only rồi về Dashboard
            saveRoomsOnlyAndFinish()
        }
    }

    private fun animateProgress(from: Int, to: Int) {
        progress.setProgress(from, false)
        ValueAnimator.ofInt(from, to).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener { progress.setProgressCompat(it.animatedValue as Int, true) }
        }.start()
    }

    private fun crossfade(hide: View, show: View) {
        show.alpha = 0f
        show.isVisible = true
        hide.animate().alpha(0f).setDuration(180).withEndAction {
            hide.isGone = true
            show.animate().alpha(1f).setDuration(220).start()
        }.start()
    }

    // ================== BƯỚC 2: wire an toàn ==================
    private fun wireStep2() {
        if (step2Wired) return
        step2Wired = true
        setupStep2SectionSafe()
    }

    private fun setupStep2SectionSafe() {
        // Dịch vụ (4 dòng với nút X)
        setupServiceSafe(R.id.svcElectric, "Dịch vụ điện", "Tính theo đồng hồ (phổ biến)")
        setupServiceSafe(R.id.svcWater,   "Dịch vụ nước", "Tính theo đồng hồ (phổ biến)")
        setupServiceSafe(R.id.svcTrash,   "Dịch vụ rác",  "Miễn phí / không sử dụng")
        setupServiceSafe(R.id.svcInternet,"Dịch vụ internet/mạng", "Miễn phí / không sử dụng")

        // 3 tính năng (App/Zalo/File)
        setupFeatureSafe(
            rootId = R.id.featApp,
            icon   = R.drawable.ic_app,
            title  = "APP dành riêng cho khách thuê",
            desc   = "Tạo & kết nối dễ dàng, hoá đơn tự động…"
        )
        setupFeatureSafe(
            rootId = R.id.featZalo,
            icon   = R.drawable.ic_zalo,
            title  = "Gửi hoá đơn tự động qua ZALO",
            desc   = "Gửi hoá đơn hàng loạt qua ZALO"
        )
        setupFeatureSafe(
            rootId = R.id.featImage,
            icon   = R.drawable.ic_file,
            title  = "Hình ảnh, File chứng từ hợp đồng",
            desc   = "Lưu CCCD, hợp đồng giấy…"
        )

        // Địa chỉ & vị trí
        val edtAddress = step2.findViewById<TextInputEditText?>(R.id.edtAddress)
        step2.findViewById<MaterialButton?>(R.id.btnMyLocation)?.setOnClickListener {
            getMyLocationAddress { addr ->
                if (addr != null) edtAddress?.setText(addr) else toast("Không lấy được vị trí.")
            }
        }
        step2.findViewById<MaterialButton?>(R.id.btnPickOnMap)?.setOnClickListener {
            val q = Uri.encode(edtAddress?.text?.toString().orEmpty())
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }
    }

    private fun setupServiceSafe(rootId: Int, title: String, desc: String) {
        val root = step2.findViewById<View?>(rootId) ?: return
        val tvTitle = root.findViewById<TextView?>(R.id.tvServiceTitle) ?: return
        val tvDesc  = root.findViewById<TextView?>(R.id.tvServiceDesc)  ?: return
        val btnClear= root.findViewById<ImageButton?>(R.id.btnClear)

        tvTitle.text = title
        tvDesc.text  = desc
        btnClear?.setOnClickListener {
            tvDesc.text = "Miễn phí / không sử dụng"
            toast("$title: đã đặt miễn phí")
        }
    }

    private fun setupFeatureSafe(rootId: Int, icon: Int, title: String, desc: String) {
        val root = step2.findViewById<View?>(rootId) ?: return
        root.findViewById<ImageView?>(R.id.imgFeatureIcon)?.setImageResource(icon)
        root.findViewById<TextView?>(R.id.tvFeatureTitle)?.text = title
        root.findViewById<TextView?>(R.id.tvFeatureDesc)?.text  = desc
        root.findViewById<MaterialSwitch?>(R.id.swFeature)?.isChecked = true
    }

    // ================== LƯU ROOMS-ONLY ==================
    private fun saveRoomsOnlyAndFinish() {
        val auto = switchAuto.isChecked
        if (!auto) { goDashboard(); return }

        val count = edtSampleRoom.text?.toString()?.toIntOrNull() ?: 0
        val price = edtPrice.text?.toString()?.toIntOrNull() ?: 0
        if (count <= 0 || price <= 0) { toast("Thiếu số phòng/giá thuê"); return }
        val name = findViewById<TextInputEditText>(R.id.edtName)?.text?.toString()?.trim().orEmpty()
        val address = step2.findViewById<TextInputEditText>(R.id.edtAddress)?.text?.toString()?.trim().orEmpty()
        val prefs = getSharedPreferences("hostel_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("hostel_name", name)
            putString("hostel_address", address)
            apply()
        }
        val db = DatabaseHelper(this).writableDatabase
        db.beginTransaction()
        try {
            for (i in 1..count) {
                val cv = ContentValues().apply {
                    put("name", "P%03d".format(i))
                    put("floor", 1)
                    put("status", "EMPTY")
                    put("baseRent", price)
                }
                db.insertOrThrow("rooms", null, cv)
            }
            db.setTransactionSuccessful()
            toast("Đã tạo $count phòng.")
        } catch (e: Exception) {
            toast("Lỗi lưu: ${e.message}")
        } finally {
            db.endTransaction()
        }
        goDashboard()
    }

    private fun goDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    // ================== VỊ TRÍ HIỆN TẠI (API 33+ OK) ==================
    private fun getMyLocationAddress(callback: (String?) -> Unit) {
        val okFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val okCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!okFine && !okCoarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOC
            )
            callback(null)
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc == null) { callback(null); return@addOnSuccessListener }
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(
                            loc.latitude, loc.longitude, 1,
                            object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    val line = addresses.firstOrNull()?.getAddressLine(0)
                                    callback(line ?: "${loc.latitude}, ${loc.longitude}")
                                }
                                override fun onError(errorMessage: String?) {
                                    callback("${loc.latitude}, ${loc.longitude}")
                                }
                            }
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        val line = list?.firstOrNull()?.getAddressLine(0)
                        callback(line ?: "${loc.latitude}, ${loc.longitude}")
                    }
                } catch (_: Exception) {
                    callback("${loc.latitude}, ${loc.longitude}")
                }
            }
            .addOnFailureListener { callback(null) }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
