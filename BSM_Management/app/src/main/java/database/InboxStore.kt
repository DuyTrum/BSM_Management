package com.example.bsm_management.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import com.example.bsm_management.ui.message.MessageItem
import database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InboxStore(context: Context) {

    private val db: SQLiteDatabase = DatabaseHelper(context).writableDatabase

    /** Lấy list tin nhắn: ghim trước, mới trước. */
    fun getAll(): List<MessageItem> {
        val list = mutableListOf<MessageItem>()
        val c: Cursor = db.query(
            "messages",
            arrayOf("id", "title", "content", "tag", "pinned", "unread", "createdAt"),
            null, null, null, null,
            "pinned DESC, createdAt DESC"
        )
        c.use {
            val idxTitle = it.getColumnIndexOrThrow("title")
            val idxContent = it.getColumnIndexOrThrow("content")
            val idxPinned = it.getColumnIndexOrThrow("pinned")
            val idxCreated = it.getColumnIndexOrThrow("createdAt")

            while (it.moveToNext()) {
                val title = it.getString(idxTitle)
                val content = it.getString(idxContent) ?: ""
                val pinned = it.getInt(idxPinned) == 1
                val createdAt = it.getLong(idxCreated)

                list.add(
                    MessageItem(
                        title = title,
                        content = content,
                        time = timeLabel(createdAt),   // <-- gọi đúng phạm vi, không dùng tên tham số
                        pinned = pinned
                    )
                )
            }
        }
        return list
    }

    /** Thêm 1 tin nhắn mới. */
    fun insert(title: String, content: String, tag: String? = null, pinned: Boolean = false): Long {
        val cv = ContentValues().apply {
            put("title", title)
            put("content", content)
            put("tag", tag)
            put("pinned", if (pinned) 1 else 0)
            put("unread", 1)
            put("createdAt", System.currentTimeMillis())
        }
        return db.insert("messages", null, cv)
    }

    // ========= Helpers =========

    // Đặt private trong class để luôn nhìn thấy từ chỗ map dữ liệu
    private fun timeLabel(ts: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - ts
        val min = 60_000L
        val hour = 60 * min
        val day = 24 * hour
        return when {
            diff < min  -> "Vừa xong"
            diff < hour -> "${diff / min} phút trước"
            diff < day  -> "${diff / hour} giờ trước"
            else        -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ts))
        }
    }
}
