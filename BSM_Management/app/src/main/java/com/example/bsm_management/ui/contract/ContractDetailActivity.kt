package com.example.bsm_management.ui.contract

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bsm_management.R
import database.DatabaseHelper
import database.dao.ContractDAO
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ContractDetailActivity : AppCompatActivity() {

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val vn = NumberFormat.getInstance(Locale("vi", "VN"))

    private lateinit var dao: ContractDAO
    private var contractId = -1
    private var tenantPhone = ""

    private lateinit var contract: Contract
    private var roomPrice = 0   // ⭐ baseRent từ bảng rooms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contract_detail)

        applyInsets()

        dao = ContractDAO(this)
        contractId = intent.getIntExtra("contractId", -1)

        if (contractId == -1) {
            toast("Không tìm thấy hợp đồng!")
            finish()
            return
        }

        val data = dao.getById(contractId)
        if (data == null) {
            toast("Hợp đồng không tồn tại!")
            finish()
            return
        }

        contract = data
        tenantPhone = contract.tenantPhone

        setupHeader(contract.roomId)
        loadRoomPrice(contract.roomId)
        bindUI()
        setupActions()
    }

    /* ============================================================
       SYSTEM UI PADDING
     ============================================================ */
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, ins ->
            val bars = ins.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            ins
        }
    }

    /* ============================================================
       LOAD ROOM PRICE (baseRent)
     ============================================================ */
    private fun loadRoomPrice(roomId: Int) {
        val db = DatabaseHelper(this).readableDatabase
        db.rawQuery(
            "SELECT baseRent FROM rooms WHERE id = ?",
            arrayOf(roomId.toString())
        ).use { c ->
            if (c.moveToFirst()) roomPrice = c.getInt(0)
        }
    }

    /* ============================================================
       BIND TO UI
     ============================================================ */
    private fun bindUI() {
        findViewById<TextView>(R.id.tvRoomName).text = "Phòng ${contract.roomId}"
        findViewById<TextView>(R.id.tvTenant).text = "Khách thuê: ${contract.tenantName}"

        val start = df.format(Date(contract.startDate))
        val end = contract.endDate?.let { df.format(Date(it)) } ?: "Vô thời hạn"

        findViewById<TextView>(R.id.tvDuration).text = "$start - $end"
        findViewById<TextView>(R.id.tvRent).text = "${vn.format(roomPrice)} đ/tháng"
        findViewById<TextView>(R.id.tvDeposit).text = "%,d ₫".format(contract.deposit)
        findViewById<TextView>(R.id.tvStatus).text =
            if (contract.active == 1) "Đang hiệu lực" else "Đã hết hạn"
        findViewById<TextView>(R.id.tvPhone).text = "📞 ${contract.tenantPhone}"
        findViewById<TextView>(R.id.tvNote).text =
            "Hợp đồng ${if (contract.endDate != null) "có thời hạn" else "vô thời hạn"}, cọc ${"%,d".format(contract.deposit)} ₫."
    }

    /* ============================================================
       HEADER
     ============================================================ */
    private fun setupHeader(roomId: Int) {
        findViewById<TextView>(R.id.tvHeaderTitle).text = "Chi tiết hợp đồng"
        findViewById<TextView>(R.id.tvHeaderSubtitle).text = "Phòng $roomId"
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    /* ============================================================
       ACTIONS
     ============================================================ */
    private fun setupActions() {
        findViewById<LinearLayout>(R.id.btnCall).setOnClickListener {
            if (tenantPhone.isBlank()) toast("Không có số điện thoại!")
            else startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tenantPhone")))
        }

        findViewById<LinearLayout>(R.id.btnPrint).setOnClickListener { doPrint() }
        findViewById<LinearLayout>(R.id.btnShare).setOnClickListener { doSharePdf() }

        findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete() }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Xóa hợp đồng")
            .setMessage("Bạn chắc muốn xóa hợp đồng này?")
            .setPositiveButton("Xóa") { _, _ ->
                if (dao.delete(contractId) > 0) {
                    toast("Đã xóa")
                    finish()
                } else toast("Không thể xóa!")
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /* ============================================================
       PRINT / SHARE PDF
     ============================================================ */
    private fun doPrint() {
        val view = inflateContractView()
        val bmp = renderBitmap(view)

        val helper = androidx.print.PrintHelper(this)
        helper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        helper.printBitmap("In hợp đồng", bmp)
    }

    private fun doSharePdf() {
        val view = inflateContractView()
        val bmp = renderBitmap(view)

        val pdf = File(cacheDir, "contract_$contractId.pdf")
        createPdfFromBitmap(bmp, pdf)

        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", pdf)

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(share, "Chia sẻ PDF hợp đồng"))
    }

    private fun createPdfFromBitmap(bmp: Bitmap, file: File) {
        val doc = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(bmp.width, bmp.height, 1)
            .create()

        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    /* ============================================================
       EXPORT VIEW
     ============================================================ */
    private fun inflateContractView(): View {
        val view = layoutInflater.inflate(R.layout.contract_export_view, null)

        view.findViewById<TextView>(R.id.tvRoomName).text = "Phòng ${contract.roomId}"
        view.findViewById<TextView>(R.id.tvTenant).text = "Khách thuê: ${contract.tenantName}"

        val start = df.format(Date(contract.startDate))
        val end = contract.endDate?.let { df.format(Date(it)) } ?: "Vô thời hạn"

        view.findViewById<TextView>(R.id.tvDuration).text = "$start - $end"
        view.findViewById<TextView>(R.id.tvRent).text = "${vn.format(roomPrice)} đ/tháng"
        view.findViewById<TextView>(R.id.tvDeposit).text = "%,d ₫".format(contract.deposit)
        view.findViewById<TextView>(R.id.tvStatus).text =
            if (contract.active == 1) "Đang hiệu lực" else "Đã hết hạn"

        view.findViewById<TextView>(R.id.tvPhone).text = contract.tenantPhone
        view.findViewById<TextView>(R.id.tvNote).text =
            "Cọc ${"%,d".format(contract.deposit)} ₫."

        return view
    }

    private fun renderBitmap(view: View): Bitmap {
        val width = 1080
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, width, view.measuredHeight)

        val bmp = Bitmap.createBitmap(width, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        view.draw(canvas)
        return bmp
    }

    /* ============================================================
       HELPERS
     ============================================================ */
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
