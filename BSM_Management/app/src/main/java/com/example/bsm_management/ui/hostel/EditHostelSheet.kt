
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
        val edtPeople = v.findViewById<TextInputEditText>(R.id.edtHostelMaxPeople)

        // Load prefs
        val prefs = requireContext().getSharedPreferences("hostel_prefs", 0)
        edtName.setText(prefs.getString("hostel_name", ""))
        edtAddr.setText(prefs.getString("hostel_address", ""))
        edtPeople.setText(prefs.getInt("hostel_max_people", 0).toString())

        v.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = edtName.text.toString().trim()
            val addr = edtAddr.text.toString().trim()
            val maxPeople = edtPeople.text.toString().toIntOrNull() ?: 0

            prefs.edit().apply {
                putString("hostel_name", name)
                putString("hostel_address", addr)
                putInt("hostel_max_people", maxPeople)
                apply()
            }

            Toast.makeText(requireContext(), "Đã lưu thông tin!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        return v
    }
}
