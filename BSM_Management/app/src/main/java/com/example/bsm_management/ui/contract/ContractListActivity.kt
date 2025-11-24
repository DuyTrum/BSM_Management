package com.example.bsm_management.ui.contract

import android.content.Intent
import android.os.Bundle
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
        loadContracts()
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
        while (cursor.moveToNext()) roomList.add(cursor.getString(0))
        cursor.close()

        // --- Lọc theo phòng ---
        btnFilterRoom.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Chọn phòng")
                .setItems(roomList.toTypedArray()) { _, which ->
                    selectedRoom = roomList[which]
                    tvRoomValue.text = selectedRoom
                    applyFilters()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // --- Lọc theo trạng thái ---
        val statuses = listOf("Tất cả", "Đang hiệu lực", "Đã hết hạn")
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

            ContractListItem(
                id = c.id,
                roomName = roomName,
                status = if (c.active == 1) "Đang hiệu lực" else "Đã hết hạn",
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
                "Đang hiệu lực" -> whereClauses.add("active=1")
                "Đã hết hạn" -> whereClauses.add("active=0")
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

            list.add(
                ContractListItem(
                    id,
                    roomName,
                    if (active == 1) "Đang hiệu lực" else "Đã hết hạn",
                    "%,d ₫/tháng".format(baseRent),
                    "%,d ₫".format(deposit),
                    df.format(Date(startDate)),
                    if (endDate > 0) df.format(Date(endDate)) else "Vô thời hạn"
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
        val btnEnd = view.findViewById<LinearLayout>(R.id.btnEnd)

        btnDetail.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ContractDetailActivity::class.java)
            intent.putExtra("contractId", contract.id)
            startActivity(intent)
        }

        btnRenew.setOnClickListener {
            dialog.dismiss()
            openRenewContract(contract)
        }

        btnEnd.setOnClickListener {
            dialog.dismiss()
            confirmEndContract(contract)
        }

        dialog.show()
    }

    /** ---------------- GIA HẠN HỢP ĐỒNG ---------------- */
    private fun openRenewContract(contract: ContractListItem) {
        val db = DatabaseHelper(this)
        val cursor = db.readableDatabase.rawQuery(
            "SELECT id FROM rooms WHERE name=?",
            arrayOf(contract.roomName)
        )
        var roomId = 0
        if (cursor.moveToFirst()) {
            roomId = cursor.getInt(0)
        }
        cursor.close()

        val intent = Intent(this, AddContractActivity::class.java)
        intent.putExtra("mode", "renew")
        intent.putExtra("contractId", contract.id)
        intent.putExtra("roomId", roomId)
        intent.putExtra("roomName", contract.roomName)
        startActivity(intent)
    }

    /** ---------------- KẾT THÚC HỢP ĐỒNG ---------------- */
    private fun confirmEndContract(contract: ContractListItem) {
        AlertDialog.Builder(this)
            .setTitle("Kết thúc hợp đồng")
            .setMessage("Bạn có chắc muốn kết thúc hợp đồng của ${contract.roomName}?")
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
        loadContracts()
    }
}
