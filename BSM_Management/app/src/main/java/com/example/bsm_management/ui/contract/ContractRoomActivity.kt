package com.example.bsm_management.ui.contract

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import database.DatabaseHelper

class ContractRoomActivity : AppCompatActivity() {

    private lateinit var rvRooms: RecyclerView
    private lateinit var edtSearch: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: ContractRoomAdapter
    private var allRooms = mutableListOf<ContractRoomItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_contract)

        db = DatabaseHelper(this)
        rvRooms = findViewById(R.id.rvRooms)
        rvRooms.layoutManager = LinearLayoutManager(this)
        edtSearch = findViewById(R.id.edtSearchRoom)
        btnSearch = findViewById(R.id.btnSearch)

        setupHeader()
        loadRooms()
        setupSearch()
    }

    /** ---------------- HEADER ---------------- */
    private fun setupHeader() {
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val tvTitle = findViewById<TextView>(R.id.tvHeaderTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvHeaderSubtitle)

        tvTitle.text = "Chọn phòng để lập hợp đồng"
        tvSubtitle.text = "Chỉ hiển thị các phòng trống"

        ivBack.setOnClickListener { finish() }
    }

    /** ---------------- LOAD DANH SÁCH PHÒNG ---------------- */
    private fun loadRooms(keyword: String? = null) {
        val whereClause = if (!keyword.isNullOrBlank()) {
            "WHERE status='EMPTY' AND name LIKE ?"
        } else {
            "WHERE status='EMPTY'"
        }

        val cursor: Cursor = if (!keyword.isNullOrBlank()) {
            db.readableDatabase.rawQuery(
                "SELECT id, name, baseRent, status FROM rooms $whereClause",
                arrayOf("%$keyword%")
            )
        } else {
            db.readableDatabase.rawQuery(
                "SELECT id, name, baseRent, status FROM rooms $whereClause",
                null
            )
        }

        val roomList = mutableListOf<ContractRoomItem>()
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val rent = cursor.getInt(2)
            val status = cursor.getString(3)
            roomList.add(
                ContractRoomItem(
                    roomId = id,
                    roomName = name,
                    price = "%,d ₫/tháng".format(rent),
                    isEmpty = status == "EMPTY"
                )
            )
        }
        cursor.close()

        if (roomList.isEmpty()) {
            Toast.makeText(this, "Không có phòng trống phù hợp!", Toast.LENGTH_SHORT).show()
        }

        allRooms = roomList
        adapter = ContractRoomAdapter(allRooms) { room ->
            val intent = Intent(this, AddContractActivity::class.java)
            intent.putExtra("roomId", room.roomId)
            intent.putExtra("roomName", room.roomName)
            startActivity(intent)
        }

        rvRooms.adapter = adapter
    }

    /** ---------------- TÌM KIẾM PHÒNG ---------------- */
    private fun setupSearch() {
        // Khi nhấn icon search
        btnSearch.setOnClickListener {
            val keyword = edtSearch.text.toString().trim()
            loadRooms(keyword)
        }

        // Khi nhập text (search realtime)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                if (keyword.isEmpty()) loadRooms() else loadRooms(keyword)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadRooms()
    }
}
