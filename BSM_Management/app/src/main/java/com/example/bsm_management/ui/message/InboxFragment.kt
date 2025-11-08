package com.example.bsm_management.ui.message

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bsm_management.databinding.FragmentInboxBinding
import database.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

class InboxFragment : Fragment() {

    private var _vb: FragmentInboxBinding? = null
    private val vb get() = _vb!!
    private lateinit var db: DatabaseHelper
    private val adapter = InboxAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _vb = FragmentInboxBinding.inflate(inflater, container, false)
        db = DatabaseHelper(requireContext())
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        vb.rvInbox.layoutManager = LinearLayoutManager(requireContext())
        vb.rvInbox.adapter = adapter

        loadMessages()

        // ====== Nút thêm tin nhắn ======
        vb.btnAddMessage.setOnClickListener {
            val now = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(Date())
            db.insertMessage(
                sender = "Chủ trọ",
                message = "Đây là tin nhắn mới được thêm bằng nút +",
                time = now,
                isRead = false
            )
            loadMessages()
        }

        // ====== Vuốt để xác nhận xóa ======
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                val msg = adapter.getCurrentItems()[pos]

                AlertDialog.Builder(requireContext())
                    .setTitle("Xóa tin nhắn?")
                    .setMessage("Bạn có chắc muốn xóa tin nhắn này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        db.deleteMessage(msg)
                        adapter.removeAt(pos)
                    }
                    .setNegativeButton("Hủy") { d, _ ->
                        d.dismiss()
                        adapter.notifyItemChanged(pos)
                    }
                    .setCancelable(false)
                    .show()
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(vb.rvInbox)
    }

    /** Nạp danh sách tin nhắn từ SQLite */
    private fun loadMessages() {
        val messages = db.getAllMessages()
        adapter.submitList(messages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}
