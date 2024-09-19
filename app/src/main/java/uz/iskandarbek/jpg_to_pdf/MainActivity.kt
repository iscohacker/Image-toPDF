package uz.iskandarbek.jpg_to_pdf

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelectImages: Button
    private lateinit var btnSavePdf: Button
    private lateinit var btnCancel: Button
    private lateinit var etPdfFileName: EditText
    private lateinit var tvImageCount: TextView

    private val REQUEST_CODE_PICK_IMAGES = 1
    private var imageUris: ArrayList<Uri> = ArrayList()

    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
            uri?.let {
                createPdf(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnSelectImages = findViewById(R.id.btnSelectImages1)
        btnSavePdf = findViewById(R.id.btnSavePdf1)
        btnCancel = findViewById(R.id.btnCancel1)
        etPdfFileName = findViewById(R.id.etPdfFileName1)
        tvImageCount = findViewById(R.id.tvImageCount1)

        btnSelectImages.setOnClickListener {
            pickImages()
        }

        btnSavePdf.setOnClickListener {
            if (imageUris.isNotEmpty() && etPdfFileName.text.isNotEmpty()) {

                createFileLauncher.launch("${etPdfFileName.text}.pdf")
            } else {
                Toast.makeText(this, "Rasmlar va PDF nomini tanlang", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            resetView()
        }

        resetView()
    }

    private fun createPdf(uri: Uri) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        for (i in imageUris.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUris[i])

            val scaleFactor = minOf(
                pageWidth.toFloat() / bitmap.width,
                pageHeight.toFloat() / bitmap.height
            )
            val scaledWidth = (bitmap.width * scaleFactor).toInt()
            val scaledHeight = (bitmap.height * scaleFactor).toInt()

            val dx = (pageWidth - scaledWidth) / 2f
            val dy = (pageHeight - scaledHeight) / 2f

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, dx, dy, null)
            pdfDocument.finishPage(page)
        }

        try {
            val outputStream = contentResolver.openOutputStream(uri)
            pdfDocument.writeTo(outputStream)
            outputStream?.close()

            showSaveDialog(uri)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Xatolik yuz berdi", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun showSaveDialog(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Saqlandi !")
            .setMessage("Nima qilamiz ?")
            .setPositiveButton("Faylga borish") { dialog, _ ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/pdf")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
                dialog.dismiss()
                resetView()
            }
            .setNegativeButton("Bekor qilish") { dialog, _ ->
                resetView()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // Rasmlarni tanlash uchun funksiyani yozamiz
    private fun pickImages() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Rasmlarni tanlang"),
            REQUEST_CODE_PICK_IMAGES
        )
    }

    // Rasmlar tanlangandan so'ng qayta ishlovchi funksiyani yozamiz
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == Activity.RESULT_OK) {
            data?.let {
                imageUris.clear()
                if (it.clipData != null) {
                    // Bir nechta rasmlar tanlangan
                    for (i in 0 until it.clipData!!.itemCount) {
                        val imageUri = it.clipData!!.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else if (it.data != null) {
                    // Bitta rasm tanlangan
                    val imageUri = it.data!!
                    imageUris.add(imageUri)
                }
                updateUI()
            }
        }
    }

    // Interfeysni dastlabki holatga qaytarish
    private fun resetView() {
        btnSavePdf.visibility = View.GONE
        etPdfFileName.visibility = View.GONE
        tvImageCount.visibility = View.GONE
        btnCancel.visibility = View.GONE
        btnSelectImages.visibility = View.VISIBLE
        imageUris.clear()
        etPdfFileName.setText("")
    }

    // Tanlangan rasmlar sonini yangilash
    private fun updateUI() {
        btnSavePdf.visibility = View.VISIBLE
        etPdfFileName.visibility = View.VISIBLE
        tvImageCount.visibility = View.VISIBLE
        btnCancel.visibility = View.VISIBLE
        btnSelectImages.visibility = View.GONE
        tvImageCount.text = "Tanlangan rasmlar soni: ${imageUris.size}"
    }
}
