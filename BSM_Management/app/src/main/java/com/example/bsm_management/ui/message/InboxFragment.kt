package com.example.bsm_management.ui.message

import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.R
import com.example.bsm_management.data.InboxStore

class InboxFragment : Fragment(R.layout.fragment_inbox) {

    private val adapter = MessageAdapter { item ->
        // TODO: mở màn chat/chi tiết
    }
    private lateinit var store: InboxStore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        store = InboxStore(requireContext())

        val rv = view.findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = resources.getDimensionPixelSize(R.dimen.pad_m)
            }
        })

        // Load lần đầu
        refreshList()

        // FAB thêm message
        view.findViewById<View>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }
    }

    private fun refreshList() {
        adapter.submitList(store.getAll())
    }

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
                    store.insert(title, content, tag = null, pinned = false)
                    refreshList()
                }
                d.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
