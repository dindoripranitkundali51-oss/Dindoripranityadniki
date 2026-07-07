package com.example.dindoripranityadnyiki.core.service

import android.annotation.SuppressLint
import android.content.Context
import com.example.dindoripranityadnyiki.R
import com.itextpdf.barcodes.BarcodeQRCode
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream

data class ReceiptPdfData(
    val bookingId: String,
    val receiptNo: String,
    val yajmanName: String,
    val mobile: String,
    val address: String,
    val poojaName: String,
    val amount: Double,
    val date: String,
    val gurujiName: String,
    val paymentId: String,
    val verifyUrl: String
)

class PdfService(private val context: Context) {

    fun generateReceipt(
        bookingId: String,
        poojaName: String,
        amount: Double,
        date: String,
        receiptNo: String = "REC-${bookingId.take(8).uppercase()}",
        yajmanName: String = "",
        mobile: String = "",
        address: String = "",
        gurujiName: String = "",
        paymentId: String = "",
        verifyUrl: String = "https://dindori-pranit-yadnyiki.web.app/verify-receipt?id=$receiptNo"
    ): File? {
        return generateReceipt(
            ReceiptPdfData(
                bookingId = bookingId,
                receiptNo = receiptNo,
                yajmanName = yajmanName,
                mobile = mobile,
                address = address,
                poojaName = poojaName,
                amount = amount,
                date = date,
                gurujiName = gurujiName,
                paymentId = paymentId,
                verifyUrl = verifyUrl
            )
        )
    }

    @SuppressLint("ResourceType")
    fun generateReceipt(receipt: ReceiptPdfData): File? {
        val receiptDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val filePath = File(receiptDir, "${receipt.receiptNo.ifBlank { receipt.bookingId }}.pdf")

        return try {
            if (filePath.exists()) filePath.delete()

            val writer = PdfWriter(FileOutputStream(filePath))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            document.setMargins(24f, 24f, 24f, 24f)

            val fontBytes = context.resources.openRawResource(R.font.notosansdevanagariregular).readBytes()
            val marathiFont = PdfFontFactory.createFont(
                fontBytes,
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            )
            val saffron = DeviceRgb(230, 81, 0)
            val green = DeviceRgb(22, 163, 74)
            val darkGreen = DeviceRgb(31, 94, 49)
            val paleGreen = DeviceRgb(232, 245, 208)
            val muted = DeviceRgb(100, 116, 139)

            val header = Table(UnitValue.createPercentArray(floatArrayOf(18f, 64f, 18f))).useAllAvailableWidth()
            val maharajBytes = context.resources.openRawResource(R.drawable.swami_samarth_receipt).readBytes()
            val maharajImage = Image(ImageDataFactory.create(maharajBytes))
                .setWidth(78f)
                .setHeight(78f)
                .setAutoScale(false)
            val gurumauliBytes = context.resources.openRawResource(R.drawable.gurumauli_receipt).readBytes()
            val gurumauliImage = Image(ImageDataFactory.create(gurumauliBytes))
                .setWidth(78f)
                .setHeight(78f)
                .setAutoScale(false)
            header.addCell(
                Cell()
                    .add(maharajImage)
                    .setBackgroundColor(paleGreen)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            header.addCell(
                Cell()
                    .add(
                        Paragraph("|| श्री स्वामी समर्थ ||")
                            .setFont(marathiFont)
                            .setBold()
                            .setFontSize(12f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(saffron)
                    )
                    .add(
                        Paragraph("श्री स्वामी समर्थ कृषी विकास व संशोधन चॅरिटेबल ट्रस्ट")
                            .setFont(marathiFont)
                            .setBold()
                            .setFontSize(16f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(darkGreen)
                    )
                    .add(
                        Paragraph("श्री स्वामी समर्थ सेवा व आध्यात्मिक विकास मार्ग (दिंडोरी प्रणित)")
                            .setFont(marathiFont)
                            .setBold()
                            .setFontSize(10f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(saffron)
                    )
                    .add(
                        Paragraph("Digital Dakshina Receipt")
                            .setFont(marathiFont)
                            .setBold()
                            .setFontSize(13f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontColor(green)
                    )
                    .add(
                        Paragraph("Receipt No: ${receipt.receiptNo}")
                            .setFont(marathiFont)
                            .setFontSize(10f)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setFontColor(muted)
                    )
                    .setBackgroundColor(paleGreen)
                    .setBorder(null)
            )
            header.addCell(
                Cell()
                    .add(gurumauliImage)
                    .setBackgroundColor(paleGreen)
                    .setBorder(null)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            document.add(header)
            document.add(Paragraph("\n"))

            document.add(
                Paragraph("दिंडोरी प्रणित याज्ञिकी सेवा")
                    .setFont(marathiFont)
                    .setBold()
                    .setFontSize(22f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(saffron)
            )
            document.add(
                Paragraph("Digital Dakshina Receipt")
                    .setFont(marathiFont)
                    .setBold()
                    .setFontSize(16f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(green)
            )
            document.add(
                Paragraph("|| श्री स्वामी समर्थ ||")
                    .setFont(marathiFont)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(13f)
            )
            document.add(Paragraph("\n"))

            val table = Table(UnitValue.createPercentArray(floatArrayOf(38f, 62f))).useAllAvailableWidth()
            addCell(table, "Receipt No", receipt.receiptNo, marathiFont)
            addCell(table, "Booking ID", receipt.bookingId, marathiFont)
            addCell(table, "Yajman Name", receipt.yajmanName.ifBlank { "-" }, marathiFont)
            addCell(table, "Mobile No.", receipt.mobile.ifBlank { "-" }, marathiFont)
            addCell(table, "Address", receipt.address.ifBlank { "-" }, marathiFont)
            addCell(table, "Seva", receipt.poojaName, marathiFont)
            addCell(table, "Date", receipt.date, marathiFont)
            addCell(table, "Guruji", receipt.gurujiName.ifBlank { "-" }, marathiFont)
            addCell(table, "Total Dakshina", "Rs. ${"%.2f".format(receipt.amount)}", marathiFont)
            addCell(table, "Payment ID", receipt.paymentId.ifBlank { "-" }, marathiFont)
            addCell(table, "Status", "Verified / Valid", marathiFont)
            document.add(table)

            document.add(Paragraph("\n"))
            if (receipt.verifyUrl.isNotBlank()) {
                val qr = BarcodeQRCode(receipt.verifyUrl)
                val qrImage = Image(qr.createFormXObject(DeviceRgb(0, 0, 0), pdf))
                    .setWidth(96f)
                    .setHeight(96f)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                document.add(qrImage)
                document.add(
                    Paragraph("Scan QR to verify this receipt")
                        .setFont(marathiFont)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(10f)
                        .setFontColor(muted)
                )
                document.add(
                    Paragraph(receipt.verifyUrl)
                        .setFont(marathiFont)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(8f)
                        .setFontColor(muted)
                )
            }

            document.add(
                Paragraph("\nThis is a system generated receipt. Signature is not required.")
                    .setFont(marathiFont)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(muted)
            )

            document.close()
            filePath
        } catch (e: Exception) {
            null
        }
    }

    private fun addCell(table: Table, label: String, value: String, font: PdfFont) {
        table.addCell(Cell().add(Paragraph(label).setFont(font).setBold()))
        table.addCell(Cell().add(Paragraph(value).setFont(font)))
    }
}
