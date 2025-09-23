package com.example.bsm_management.ui.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bsm_management.databinding.FragmentInboxBinding

class InboxFragment : Fragment() {

    private var _vb: FragmentInboxBinding? = null
    private val vb get() = _vb!!

    private val adapter = MessageAdapter { message ->
        // TODO: xử lý khi click tin nhắn (mở chi tiết)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _vb = FragmentInboxBinding.inflate(inflater, container, false)
        return vb.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vb.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        vb.rvMessages.adapter = adapter

        // Demo dữ liệu mẫu
        adapter.submitList(
            listOf(
                MessageItem("Nguyễn Văn A", "Hợp đồng sắp hết hạn...", "10:30", true),
                MessageItem("Hệ thống", "Bạn có 1 hóa đơn mới", "Hôm qua", false),
                MessageItem("Lê Thị B", "Cho tôi hỏi về phòng trọ...", "12/09", true),
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _vb = null
    }
}
