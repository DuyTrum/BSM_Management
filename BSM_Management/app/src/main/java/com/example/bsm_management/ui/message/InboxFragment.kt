package com.example.bsm_management.ui.message

import android.app.AlertDialog
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.ui.model.MessageItem
import database.DatabaseHelper


data class MessageItem(
    val id: Long,
    val title: String,
    val content: String?,
    val tag: String?,
    val pinned: Boolean,
    val unread: Boolean,
    val createdAt: Long
)

class InboxFragment : Fragment(R.layout.fragment_inbox) {

    private val adapter = MessageAdapter { item ->
        // TODO: mở màn chat/chi tiết theo item.id
    }

    private lateinit var db: DatabaseHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = resources.getDimensionPixelSize(R.dimen.pad_m)
            }
        })

        refreshList()

        view.findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }
    }

    private fun refreshList() {
        adapter.submitList(queryAllMessages())
    }

    /** ===== DB helpers ===== */

    private fun queryAllMessages(): List<MessageItem> {
        val out = mutableListOf<MessageItem>()
        val sql = """
            SELECT id, title, content, tag, pinned, unread, createdAt
            FROM messages
            ORDER BY pinned DESC, createdAt DESC
        """.trimIndent()

        val c: Cursor = db.readableDatabase.rawQuery(sql, null)
        c.use {
            val idxId = c.getColumnIndexOrThrow("id")
            val idxTitle = c.getColumnIndexOrThrow("title")
            val idxContent = c.getColumnIndexOrThrow("content")
            val idxTag = c.getColumnIndexOrThrow("tag")
            val idxPinned = c.getColumnIndexOrThrow("pinned")
            val idxUnread = c.getColumnIndexOrThrow("unread")
            val idxCreated = c.getColumnIndexOrThrow("createdAt")

            while (c.moveToNext()) {
                out += MessageItem(
                    id = c.getLong(idxId),
                    title = c.getString(idxTitle),
                    content = c.getString(idxContent),
                    tag = c.getString(idxTag),
                    pinned = c.getInt(idxPinned) == 1,
                    unread = c.getInt(idxUnread) == 1,
                    createdAt = c.getLong(idxCreated)
                )
            }
        }
        return out
    }

    private fun insertMessage(title: String, content: String?) {
        val values = ContentValues().apply {
            put("title", title)
            put("content", content)
            put("tag", null as String?)
            put("pinned", 0)
            put("unread", 1)
            put("createdAt", System.currentTimeMillis())
        }
        db.writableDatabase.insert("messages", null, values)
    }

    /** ===== UI ===== */

    private fun showAddDialog() {
        val ctx = requireContext()
        val inputTitle = EditText(ctx).apply { hint = "Tiêu đề" }
        val inputContent = EditText(ctx).apply { hint = "Nội dung" }

        val container = androidx.appcompat.widget.LinearLayoutCompat(ctx).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            val pad = resources.getDimensionPixelSize(R.dimen.pad_l)
            setPadding(pad, pad, pad, 0)
            addView(inputTitle)
            addView(inputContent)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Tin nhắn mới")
            .setView(container)
            .setPositiveButton("Lưu") { d, _ ->
                val title = inputTitle.text?.toString()?.trim().orEmpty()
                val content = inputContent.text?.toString()?.trim().orEmpty()
                if (title.isNotEmpty()) {
                    insertMessage(title, if (content.isBlank()) null else content)
                    refreshList()
                }
                d.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // DatabaseHelper dùng singleton nội bộ, không cần close thủ công.
        // Nếu bạn tạo SQLiteDatabase riêng thì nhớ close().
    }
}
