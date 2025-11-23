
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.bsm_management.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditHostelSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.sheet_edit_hostel, container, false)

        val edtName   = v.findViewById<TextInputEditText>(R.id.edtHostelName)
        val edtAddr   = v.findViewById<TextInputEditText>(R.id.edtHostelAddress)

        // Load prefs
        val prefs = requireContext().getSharedPreferences("hostel_prefs", 0)
        edtName.setText(prefs.getString("hostel_name", ""))
        edtAddr.setText(prefs.getString("hostel_address", ""))

        v.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = edtName.text.toString().trim()
            val addr = edtAddr.text.toString().trim()

            prefs.edit().apply {
                putString("hostel_name", name)
                putString("hostel_address", addr)
                apply()
            }

            Toast.makeText(requireContext(), "Đã lưu thông tin!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return v
    }
}
