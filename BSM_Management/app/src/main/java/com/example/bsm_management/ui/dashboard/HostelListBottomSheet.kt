package com.example.bsm_management.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HostelListBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_hostel_list, container, false)
    }
}
