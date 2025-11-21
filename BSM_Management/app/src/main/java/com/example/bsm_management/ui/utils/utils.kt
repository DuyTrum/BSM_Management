package com.example.bsm_management.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.bsm_management.ui.tenant.Tenant
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object ExcelExporter {

    fun exportTenants(context: Context, tenants: List<Tenant>): Uri? {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Danh sách khách thuê")

        // STYLE HEADER
        val headerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
        }

        // HEADER
        val header = sheet.createRow(0)
        header.createCell(0).apply {
            setCellValue("Tên khách thuê")
            cellStyle = headerStyle
        }
        header.createCell(1).apply {
            setCellValue("Số điện thoại")
            cellStyle = headerStyle
        }
        header.createCell(2).apply {
            setCellValue("Phòng")
            cellStyle = headerStyle
        }

        // DATA
        tenants.forEachIndexed { index, t ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(t.name)
            row.createCell(1).setCellValue(t.phone)
            row.createCell(2).setCellValue("P${t.roomId}")
        }


        val file = File(
            context.getExternalFilesDir(null),
            "tenants_${System.currentTimeMillis()}.xlsx"
        )

        FileOutputStream(file).use { fos ->
            workbook.write(fos)
        }
        workbook.close()

        return FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
    }
}
