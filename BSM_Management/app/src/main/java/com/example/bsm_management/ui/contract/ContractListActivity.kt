package com.example.bsm_management.ui.contract

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import database.DatabaseHelper
import database.dao.ContractDAO
import java.text.SimpleDateFormat
import java.util.*

class ContractListActivity : AppCompatActivity() {

    private lateinit var dao: ContractDAO
    private lateinit var rvContracts: RecyclerView
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var selectedRoom: String? = null
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contract_list)

        // Padding cho status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        dao = ContractDAO(this)
        rvContracts = findViewById(R.id.rvContracts)
        rvContracts.layoutManager = LinearLayoutManager(this)

        setupHeader()
        setupFilters()
        selectedStatus = "Đang hiệu lực"
        findViewById<TextView>(R.id.tvStatusValue).text = "Đang hiệu lực"
        selectedRoom = null
        findViewById<TextView>(R.id.tvRoomValue).text = "Tất cả"

        applyFilters()
    }

    /** ---------------- HEADER ---------------- */
    private fun setupHeader() {
        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)
        val ivBack = findViewById<ImageView>(R.id.ivBack)

        tvTitle.text = "Danh sách hợp đồng"
        tvSubtitle.text = "Quản lý, gia hạn và kết thúc hợp đồng"

        ivBack.setOnClickListener { finish() }
    }

    /** ---------------- FILTER ---------------- */
    private fun setupFilters() {
        val tvRoomValue = findViewById<TextView>(R.id.tvRoomValue)
        val tvStatusValue = findViewById<TextView>(R.id.tvStatusValue)
        val btnFilterRoom = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.btnFilterRoom)
        val btnFilterStatus = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.btnFilterStatus)

        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery("SELECT name FROM rooms", null)

        val roomList = mutableListOf<String>()
        roomList.add("Tất cả phòng")

        while (cursor.moveToNext()) roomList.add(cursor.getString(0))
        cursor.close()

        // --- Lọc theo phòng ---
        btnFilterRoom.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Chọn phòng")
                .setItems(roomList.toTypedArray()) { _, which ->
                    if (which == 0) {
                        selectedRoom = null
                        tvRoomValue.text = "Tất cả"
                    } else {
                        selectedRoom = roomList[which]
                        tvRoomValue.text = selectedRoom
                    }
                    applyFilters()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // --- Lọc theo trạng thái ---
        val statuses = listOf("Tất cả", "Đang hiệu lực", "Đã hết hạn", "Đã hủy")
        btnFilterStatus.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Trạng thái hợp đồng")
                .setItems(statuses.toTypedArray()) { _, which ->
                    selectedStatus = if (which == 0) null else statuses[which]
                    tvStatusValue.text = statuses[which]
                    applyFilters()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    /** ---------------- LOAD DANH SÁCH ---------------- */
    private fun loadContracts() {
        val contracts = dao.getAll()
        val db = DatabaseHelper(this)

        val listItems = contracts.map { c ->
            var roomName = "Phòng ${c.roomId}"
            var baseRent = 0

            val cursor = db.readableDatabase.rawQuery(
                "SELECT name, baseRent FROM rooms WHERE id=?",
                arrayOf(c.roomId.toString())
            )
            if (cursor.moveToFirst()) {
                roomName = cursor.getString(0)
                baseRent = cursor.getInt(1)
            }
            cursor.close()

            val statusText = when {
                c.active == 0 -> "🔴 Đã hủy"
                c.endDate != null && c.endDate!! < System.currentTimeMillis() -> "🟡 Đã hết hạn"
                else -> "🟢 Đang hiệu lực"
            }

            ContractListItem(
                id = c.id,
                roomName = roomName,
                status = statusText,
                rent = "%,d ₫/tháng".format(baseRent),
                deposit = "%,d ₫".format(c.deposit),
                createdDate = df.format(Date(c.startDate)),
                endDate = if (c.endDate != null) df.format(Date(c.endDate!!)) else "Vô thời hạn"
            )
        }

        rvContracts.adapter = ContractListAdapter(listItems) { item ->
            showContractOptionsDialog(item)
        }
    }

    /** ---------------- ÁP DỤNG FILTER ---------------- */
    private fun applyFilters() {
        val db = DatabaseHelper(this)
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        selectedRoom?.let {
            whereClauses.add("roomId IN (SELECT id FROM rooms WHERE name=?)")
            args.add(it)
        }

        selectedStatus?.let {
            when (it) {
                "Đang hiệu lực" ->
                    whereClauses.add("active = 1 AND (endDate IS NULL OR endDate >= ${System.currentTimeMillis()})")

                "Đã hết hạn" ->
                    whereClauses.add("active = 1 AND endDate < ${System.currentTimeMillis()}")

                "Đã hủy" ->
                    whereClauses.add("active = 0")
            }
        }

        val where = if (whereClauses.isNotEmpty()) "WHERE ${whereClauses.joinToString(" AND ")}" else ""
        val query = "SELECT * FROM contracts $where"
        val cursor = db.readableDatabase.rawQuery(query, args.toTypedArray())

        val list = mutableListOf<ContractListItem>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val roomId = cursor.getInt(cursor.getColumnIndexOrThrow("roomId"))
            val startDate = cursor.getLong(cursor.getColumnIndexOrThrow("startDate"))
            val endDate = cursor.getLong(cursor.getColumnIndexOrThrow("endDate"))
            val deposit = cursor.getInt(cursor.getColumnIndexOrThrow("deposit"))
            val active = cursor.getInt(cursor.getColumnIndexOrThrow("active"))

            var roomName = "Phòng $roomId"
            var baseRent = 0
            val rCur = db.readableDatabase.rawQuery(
                "SELECT name, baseRent FROM rooms WHERE id=?",
                arrayOf(roomId.toString())
            )
            if (rCur.moveToFirst()) {
                roomName = rCur.getString(0)
                baseRent = rCur.getInt(1)
            }
            rCur.close()

            val statusText = when {
                active == 0 -> "🔴 Đã hủy"
                endDate > 0 && endDate < System.currentTimeMillis() -> "🟡 Đã hết hạn"
                else -> "🟢 Đang hiệu lực"
            }

            list.add(
                ContractListItem(
                    id = id,
                    roomName = roomName,
                    status = statusText,
                    rent = "%,d ₫/tháng".format(baseRent),
                    deposit = "%,d ₫".format(deposit),
                    createdDate = df.format(Date(startDate)),
                    endDate = if (endDate > 0) df.format(Date(endDate)) else "Vô thời hạn"
                )
            )
        }
        cursor.close()

        rvContracts.adapter = ContractListAdapter(list) { item ->
            showContractOptionsDialog(item)
        }
    }

    /** ---------------- OPTIONS (CHI TIẾT, GIA HẠN, KẾT THÚC) ---------------- */
    private fun showContractOptionsDialog(contract: ContractListItem) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_contract_options, null)
        dialog.setContentView(view)

        val btnDetail = view.findViewById<LinearLayout>(R.id.btnDetail)
        val btnRenew = view.findViewById<LinearLayout>(R.id.btnRenew)
        // Ẩn nút Gia hạn nếu hợp đồng đã hủy
        if (contract.status.contains("Đã hủy")) {
            btnRenew.visibility = View.GONE
        } else {
            btnRenew.visibility = View.VISIBLE
        }

        val btnEnd = view.findViewById<LinearLayout>(R.id.btnEnd)
        val btnRestore = view.findViewById<LinearLayout>(R.id.btnRestore)

        btnRestore.visibility = if (contract.status.contains("Đã hủy")) View.VISIBLE else View.GONE

        btnDetail.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ContractDetailActivity::class.java)
            intent.putExtra("contractId", contract.id)
            startActivity(intent)
        }

        btnRestore.setOnClickListener {
            dialog.dismiss()
            restoreContract(contract)
        }

        btnRenew.setOnClickListener {
            dialog.dismiss()

            if (contract.status.contains("Đã hủy")) {
                Toast.makeText(this, "Hợp đồng đã hủy – không thể gia hạn", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            openRenewContract(contract)
        }


        btnEnd.setOnClickListener {
            dialog.dismiss()
            confirmDeleteContract(contract)
        }

        dialog.show()
    }

    private fun restoreContract(contract: ContractListItem) {
        val db = DatabaseHelper(this).readableDatabase

        // 1) Kiểm tra phòng phải TRỐNG
        val cursor = db.rawQuery(
            "SELECT status FROM rooms WHERE name=? LIMIT 1",
            arrayOf(contract.roomName)
        )

        var isEmpty = false
        if (cursor.moveToFirst()) {
            isEmpty = cursor.getString(0) == "EMPTY"
        }
        cursor.close()

        if (!isEmpty) {
            Toast.makeText(this, "Không thể khôi phục! Phòng hiện đang được thuê.", Toast.LENGTH_LONG).show()
            return
        }

        // Xác nhận
        AlertDialog.Builder(this)
            .setTitle("Khôi phục hợp đồng")
            .setMessage("Khôi phục hợp đồng này? Phòng sẽ được đánh dấu là đang thuê.")
            .setPositiveButton("Khôi phục") { _, _ ->

                val wdb = DatabaseHelper(this).writableDatabase

                // 2) Khôi phục hợp đồng
                val cv = android.content.ContentValues().apply {
                    put("active", 1)
                }
                wdb.update("contracts", cv, "id=?", arrayOf(contract.id.toString()))

                // 3) Cập nhật phòng về RENTED
                val cvRoom = android.content.ContentValues().apply {
                    put("status", "RENTED")
                }
                wdb.update("rooms", cvRoom, "name=?", arrayOf(contract.roomName))

                Toast.makeText(this, "Đã khôi phục hợp đồng", Toast.LENGTH_SHORT).show()
                loadContracts()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /** ---------------- GIA HẠN HỢP ĐỒNG ---------------- */
    private fun openRenewContract(contract: ContractListItem) {
        if (contract.endDate == "Vô thời hạn") {
            Toast.makeText(this, "Hợp đồng vô thời hạn không thể gia hạn", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_renew_contract, null)
        dialog.setContentView(view)

        val tvStart = view.findViewById<TextView>(R.id.tvStartDate)
        val tvEnd = view.findViewById<TextView>(R.id.tvEndDate)
        val spnTerm = view.findViewById<Spinner>(R.id.spnTerm)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // Format
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Lấy endDate cũ làm start mới
        val oldEndTimestamp = df.parse(contract.endDate)?.time ?: System.currentTimeMillis()
        tvStart.text = contract.endDate   // ví dụ: 19/02/2025

        var newEndTimestamp = 0L

        // Spinner: 1 năm, 2 năm, tùy chỉnh
        val options = listOf("1 năm", "2 năm", "Tùy chỉnh")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnTerm.adapter = adapter

        fun recalcEnd(term: Int?) {
            if (term != null) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = oldEndTimestamp
                    add(Calendar.MONTH, term)
                }
                newEndTimestamp = cal.timeInMillis
                tvEnd.text = df.format(cal.time)
            }
        }

        // Mặc định 1 năm
        recalcEnd(12)

        spnTerm.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                when (pos) {
                    0 -> recalcEnd(12)
                    1 -> recalcEnd(24)
                    2 -> {
                        // Tùy chỉnh → cho người dùng chọn
                        showDatePicker { picked ->
                            tvEnd.text = picked
                            newEndTimestamp = df.parse(picked)?.time ?: 0L
                        }
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Xác nhận gia hạn
        btnConfirm.setOnClickListener {
            renewContractInDB(contract.id, oldEndTimestamp, newEndTimestamp)
            dialog.dismiss()
            loadContracts()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDatePicker(onPicked: (String) -> Unit) {
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d)
                onPicked(df.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    private fun renewContractInDB(oldContractId: Int, newStart: Long, newEnd: Long) {
        val db = DatabaseHelper(this)

        // --- Lấy hợp đồng cũ ---
        val cursor = db.readableDatabase.rawQuery(
            "SELECT roomId, tenantName, tenantPhone, deposit, endDate FROM contracts WHERE id=?",
            arrayOf(oldContractId.toString())
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            Toast.makeText(this, "Không tìm thấy hợp đồng cũ!", Toast.LENGTH_SHORT).show()
            return
        }

        val roomId = cursor.getInt(cursor.getColumnIndexOrThrow("roomId"))
        val tenantName = cursor.getString(cursor.getColumnIndexOrThrow("tenantName"))
        val tenantPhone = cursor.getString(cursor.getColumnIndexOrThrow("tenantPhone"))
        val deposit = cursor.getInt(cursor.getColumnIndexOrThrow("deposit"))
        val oldEndDate = cursor.getLong(cursor.getColumnIndexOrThrow("endDate"))
        cursor.close()

        // --- CHẶN hợp đồng vô thời hạn ---
        if (oldEndDate <= 0) {
            Toast.makeText(this, "Hợp đồng vô thời hạn không thể gia hạn!", Toast.LENGTH_LONG).show()
            return
        }

        // --- Set hợp đồng cũ thành hết hiệu lực ---
        db.writableDatabase.execSQL(
            "UPDATE contracts SET active=0 WHERE id=$oldContractId"
        )

        // --- Tạo hợp đồng mới ---
        val sqlInsert = """
        INSERT INTO contracts (roomId, tenantName, tenantPhone, startDate, endDate, deposit, active)
        VALUES (?, ?, ?, ?, ?, ?, 1)
    """

        db.writableDatabase.execSQL(
            sqlInsert,
            arrayOf(
                roomId,
                tenantName,
                tenantPhone,
                newStart,
                newEnd,
                deposit
            )
        )

        // --- Cập nhật trạng thái phòng ---
        db.writableDatabase.execSQL(
            "UPDATE rooms SET status='RENTED' WHERE id=$roomId"
        )

        Toast.makeText(this, "Gia hạn hợp đồng thành công!", Toast.LENGTH_SHORT).show()
    }



    /** ---------------- KẾT THÚC HỢP ĐỒNG ---------------- */
    private fun confirmDeleteContract(contract: ContractListItem) {
        AlertDialog.Builder(this)
            .setTitle("Xóa hợp đồng")
            .setMessage("Bạn có chắc muốn XÓA hợp đồng của ${contract.roomName}?")
            .setPositiveButton("Xóa") { _, _ ->
                val db = DatabaseHelper(this)
                db.writableDatabase.delete("contracts", "id = ?", arrayOf(contract.id.toString()))
                db.writableDatabase.execSQL(
                    "UPDATE rooms SET status='EMPTY' WHERE name='${contract.roomName}'"
                )
                Toast.makeText(this, "Đã kết thúc hợp đồng!", Toast.LENGTH_SHORT).show()
                loadContracts()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        applyFilters()
    }
}
