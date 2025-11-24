package com.example.bsm_management.ui.hostel

import android.Manifest
import android.animation.ValueAnimator
import android.text.TextWatcher
import android.text.Editable
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
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
    private lateinit var ddlMaxPeople: AutoCompleteTextView
    private lateinit var btnClose: MaterialButton
    private lateinit var btnNext: MaterialButton

    // progress + containers
    private lateinit var progress: LinearProgressIndicator
    private lateinit var step1: View
    private lateinit var step2: View

    private val REQ_LOC = 1001
    private var isOnStep2 = false
    private var step2Wired = false

    // ✅ Lưu trạng thái dịch vụ
    private val serviceStates = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hostel)
        val topBar = findViewById<MaterialToolbar>(R.id.topBar)
        topBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        // ===== FIND VIEW HERE FIRST =====
        edtSampleRoom = findViewById(R.id.edtSampleRoom)
        edtArea       = findViewById(R.id.edtArea)
        edtPrice      = findViewById(R.id.edtPrice)
        ddlMaxPeople  = findViewById(R.id.ddlMaxPeople)
        btnClose      = findViewById(R.id.btnClose)
        btnNext       = findViewById(R.id.btnNext)
        progress      = findViewById(R.id.progress)
        step1         = findViewById(R.id.step1Container)
        step2         = findViewById(R.id.step2Container)

        // ===== ADD LISTENER AFTER FINDVIEW =====
        edtArea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val area = s?.toString()?.toIntOrNull() ?: return
                edtPrice.setText(suggestRent(area).toString())
            }
        })

        // ===== SETUP DROPDOWN =====
        ddlMaxPeople.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                listOf("1","2","3","4","5","6","7","8")
            )
        )
        ddlMaxPeople.setText("2", false)

        // ===== BUTTON EVENTS =====
        btnClose.isEnabled = false
        btnClose.setOnClickListener {
            if (isOnStep2) {
                animateProgress(100, 40)
                crossfade(step2, step1)
                isOnStep2 = false
                btnClose.isEnabled = false
                btnNext.text = "Tiếp theo"
            } else finish()
        }

        btnNext.setOnClickListener { onNextClicked() }
    }

    private fun suggestRent(area: Int): Int {
        return when {
            area < 15 -> 1500000
            area < 20 -> 2000000
            area < 25 -> 2600000
            area < 30 -> 3400000
            else -> 4500000
        }
    }


    // ================== FLOW BƯỚC 1 -> 2 ==================
    private fun onNextClicked() {
        val maxPeople = ddlMaxPeople.text.toString().toIntOrNull()
            ?: run {
                toast("Vui lòng chọn số người tối đa")
                return
            }
        intent.putExtra("maxPeople", maxPeople)

        if (!isOnStep2) {
            val count = edtSampleRoom.text?.toString()?.toIntOrNull() ?: 0
            val price = edtPrice.text?.toString()?.toIntOrNull() ?: 0
            if (count <= 0) { toast("Nhập số lượng phòng mẫu > 0"); return }
            if (price <= 0) { toast("Nhập giá thuê mẫu > 0"); return }
            animateProgress(40, 100)
            crossfade(step1, step2)
            isOnStep2 = true
            btnClose.isEnabled = true
            btnNext.text = "Lưu thông tin"
            wireStep2()
        } else {
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

    // ================== BƯỚC 2 ==================
    private fun wireStep2() {
        if (step2Wired) return
        step2Wired = true
        setupStep2Section()
    }

    private fun setupStep2Section() {
        // ==== Dịch vụ ====
        setupService(R.id.svcElectric, "Dịch vụ điện", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcWater, "Dịch vụ nước", "Tính theo đồng hồ (phổ biến)")
        setupService(R.id.svcTrash, "Dịch vụ rác", "Miễn phí / không sử dụng")
        setupService(R.id.svcInternet, "Dịch vụ internet/mạng", "Miễn phí / không sử dụng")

        // ==== Địa chỉ & vị trí ====
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

    private fun setupService(rootId: Int, title: String, desc: String) {
        val root = findViewById<View>(rootId)
        val tvTitle = root.findViewById<TextView>(R.id.tvServiceTitle)
        val tvDesc = root.findViewById<TextView>(R.id.tvServiceDesc)
        val sw = root.findViewById<MaterialSwitch>(R.id.swService)

        tvTitle.text = title
        tvDesc.text = desc

        val defaultChecked = title.contains("điện") || title.contains("nước")
        sw.isChecked = defaultChecked
        serviceStates[title] = defaultChecked

        sw.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            serviceStates[title] = isChecked
            tvDesc.text = if (isChecked) "Đang sử dụng" else "Miễn phí / không sử dụng"
        }
    }

    private fun setupFeature(rootId: Int, icon: Int, title: String, desc: String) {
        val root = step2.findViewById<View?>(rootId) ?: return
        root.findViewById<ImageView?>(R.id.imgFeatureIcon)?.setImageResource(icon)
        root.findViewById<TextView?>(R.id.tvFeatureTitle)?.text = title
        root.findViewById<TextView?>(R.id.tvFeatureDesc)?.text  = desc
        root.findViewById<MaterialSwitch?>(R.id.swFeature)?.isChecked = true
    }

    // ================== LƯU ROOMS & DỊCH VỤ ==================
    private fun saveRoomsOnlyAndFinish() {

        val count = edtSampleRoom.text?.toString()?.toIntOrNull() ?: 0
        val price = edtPrice.text?.toString()?.toIntOrNull() ?: 0
        if (count <= 0 || price <= 0) { toast("Thiếu số phòng/giá thuê"); return }

        // --- TÊN NHÀ TRỌ ---
        val hostelName = findViewById<TextInputEditText>(R.id.edtHostelName)
            ?.text?.toString()?.trim()
            ?: "Nhà trọ của bạn"

        // --- ĐỊA CHỈ ---
        val edtAddress = step2.findViewById<TextInputEditText>(R.id.edtAddress)
        val address = edtAddress?.text?.toString()?.trim().orEmpty()

        // --- SỐ NGƯỜI TỐI ĐA ---
        val maxPeople = ddlMaxPeople.text.toString().toIntOrNull()
            ?: run {
                toast("Vui lòng chọn số người tối đa")
                return
            }

        // Lưu tên nhà trọ
        val prefs = getSharedPreferences("hostel_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("hostel_name", hostelName)
            putString("hostel_address", address)
            apply()
        }

        val db = DatabaseHelper(this).writableDatabase
        db.beginTransaction()
        try {

            // ❗ XÓA PHÒNG CŨ TRƯỚC
            db.execSQL("DELETE FROM rooms")

            // ❗ XÓA BẢNG SERVICE CŨ TRƯỚC (nếu có bảng tổng)
            db.execSQL("DELETE FROM services")

            val insertRoom = db.compileStatement(
                "INSERT INTO rooms (name, floor, status, baseRent, maxPeople) VALUES (?,1,'EMPTY',?,?)"
            )

            val insertSvc = db.compileStatement(
                "INSERT INTO services (roomId, serviceName, enabled, price) VALUES (?, ?, ?, ?)"
            )

            val allServices = listOf(
                "Dịch vụ điện",
                "Dịch vụ nước",
                "Dịch vụ rác",
                "Dịch vụ internet/mạng"
            )
            val defaultPrices = mapOf(
                "Dịch vụ điện" to 3500,
                "Dịch vụ nước" to 12000,
                "Dịch vụ rác" to 20000,
                "Dịch vụ internet/mạng" to 100000
            )


            for (i in 1..count) {

                val roomName = "P%03d".format(i)

                insertRoom.bindString(1, roomName)
                insertRoom.bindLong(2, price.toLong())
                insertRoom.bindLong(3, maxPeople.toLong())

                val roomId = insertRoom.executeInsert()

                // tạo dịch vụ
                allServices.forEach { svc ->
                    insertSvc.bindLong(1, roomId)
                    insertSvc.bindString(2, svc)
                    insertSvc.bindLong(3, if (serviceStates[svc] == true) 1 else 0)
                    insertSvc.bindLong(4, defaultPrices[svc]?.toLong() ?: 0L)
                    insertSvc.executeInsert()
                }

            }

            db.setTransactionSuccessful()
            toast("Đã tạo $count phòng & dịch vụ!")

        } catch (e: Exception) {
            toast("Lỗi lưu: ${e.message}")
        } finally {
            db.endTransaction()
        }
        // ghi thông báo vào bảng messages
        val now = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale("vi", "VN"))
            .format(java.util.Date())

        DatabaseHelper(this).insertMessage(
            sender = "Hệ thống",
            message = "Đã tạo nhà trọ mới: $hostelName với $count phòng.",
            time = now,
            isRead = false,
            hostelName = hostelName
        )

        goDashboard()
    }



    private fun goDashboard() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    // ================== LẤY VỊ TRÍ ==================
    private fun getMyLocationAddress(callback: (String?) -> Unit) {
        val okFine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val okCoarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!okFine && !okCoarse) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOC)
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
