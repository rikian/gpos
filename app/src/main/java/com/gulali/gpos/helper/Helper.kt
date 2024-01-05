package com.gulali.gpos.helper

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.gulali.gpos.R
import com.gulali.gpos.database.CategoryEntity
import com.gulali.gpos.database.DateTime
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.database.UnitEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

open class Helper {
    fun generateTOA(ctx:Context, msg: String, isShort: Boolean) {
        return if (isShort) {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun generateUniqueFileName(): String {
        return "gpos-${UUID.randomUUID()}"
    }

    fun generatePaymentID(): String {
        return "${UUID.randomUUID()}".split("-")[0]
    }

    fun getAuthority(): String {
        return "com.gulali.gpos.fileProvider"
    }

    // Function to rotate the image based on its orientation
    fun rotateImageIfRequired(bitmap: Bitmap?, imagePath: String): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val ei = ExifInterface(imagePath)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> this.rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> this.rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> this.rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    // Function to rotate the image by a specified angle
    private fun rotateImage(bitmap: Bitmap?, angle: Float): Bitmap? {
        if (bitmap == null) {
            return null
        }
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveImageToLocalStorage(contentResolver: ContentResolver, appName: String, bitmap: Bitmap?, uniqueFileName: String): Boolean {
        try {
            if (bitmap == null) {
                return false
            }
            val timestamp = System.currentTimeMillis()
            //Tell the media scanner about the new file so that it is immediately available to the user.
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$appName")
                values.put(MediaStore.Images.Media.IS_PENDING, true)
                values.put(MediaStore.Images.Media.DISPLAY_NAME, uniqueFileName)
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                val outputStream = contentResolver.openOutputStream(uri) ?: return false
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                outputStream.close()
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                contentResolver.update(uri, values, null, null)
            } else {
                val imageFileFolder = File(
                    Environment.getExternalStorageDirectory().toString() + '/' + appName)
                if (!imageFileFolder.exists()) {
                    imageFileFolder.mkdirs()
                }
                val imageFile = File(imageFileFolder, uniqueFileName)
                val outputStream: OutputStream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
                outputStream.close()
                values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getDate(): String {
        val calendar = Calendar.getInstance()
        val currentDateAndTime = calendar.time
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(currentDateAndTime)
    }

    fun dateToUnixTimestamp(date: Date): Long {
        return date.time / 1000 // Convert milliseconds to seconds
    }

    fun parseDateStrToUnix(dateString: String): Long {
        return try {
            // Define the date format
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Parse the string to a Date object
            val date = dateFormat.parse(dateString)

            // If the date is not null, convert it to Unix timestamp in seconds
            date?.let {
                return@let dateToUnixTimestamp(it)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun parseToEndDateUnix(dateString: String): Long {
        return try {
            // Define the date format
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            // Parse the string to a Date object
            val date = dateFormat.parse(dateString)

            // If the date is not null, set the time to the end of the day
            date?.let {
                val calendar = Calendar.getInstance().apply {
                    time = it
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                return dateToUnixTimestamp(calendar.time)
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun unixTimestampToDate(timestamp: Long): Date {
        return Date(timestamp * 1000) // Convert seconds to milliseconds
    }

    fun formatDateFromTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(unixTimestampToDate(timestamp))
    }

    fun formatSpecificDate(date: Date): DateTime{
        val d = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(date)
        val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)

        return DateTime(
            date = d,
            time = t,
        )
    }

    fun getCurrentDate(): Long {
        val calendar = Calendar.getInstance()
        val date = calendar.time
        return this.dateToUnixTimestamp(date)
    }

    fun getCurrentDatePlus1(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Add 1 day to the current date
        val date = calendar.time
        return dateToUnixTimestamp(date)
    }

    fun getCurrentEndDate(): Long {
        val calendar = Calendar.getInstance()
        // Set the time to 23:59:59 for the current day
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val date = calendar.time
        return dateToUnixTimestamp(date)
    }

    fun intToRupiah(value: Int): String {
        val v = value.toString()
        val vp = v.chunked(1)

        if (vp.size <= 3) {
            return v
        }

        val rvp = mutableListOf<String>()
        var n = 0
        for (i in vp.size downTo 1) {
            if (n == 3) {
                n = 0
                rvp.add(".")
            }
            rvp.add(vp[i - 1])
            n++
        }

        var r = ""
        for (i in rvp.size downTo 1) {
            r += rvp[i - 1]
        }

        return r
    }

    fun rupiahToInt(rupiah: String): Int {
        // Remove non-numeric characters and the dot separator
        val cleanedString = rupiah.replace("\\D".toRegex(), "")

        if (cleanedString.isNotBlank()) {
            return cleanedString.toInt()
        }

        // Return 0 if the cleaned string is empty or contains only non-numeric characters
        return 0
    }

    fun initialSockListener(qtyMin: Button, qtyPlus: Button, input: EditText) {
        qtyMin.setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value != null && value > 0) {
                input.setText((value - 1).toString())
            }
        }

        qtyPlus.setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value != null) {
                val newVal = (value + 1).toString()
                input.setText(newVal)
            }
        }
    }

    fun readUnitByName(name: String, listSpinner: List<UnitEntity>): UnitEntity? {
        if (listSpinner.isEmpty()) {
            return null
        }
        for (v in listSpinner) {
            if (v.name == name) {
                return v
            }
        }
        return null
    }

    fun readCategoryByName(name: String, listSpinner: List<CategoryEntity>): CategoryEntity? {
        if (listSpinner.isEmpty()) {
            return null
        }
        for (v in listSpinner) {
            if (v.name == name) {
                return v
            }
        }
        return null
    }

    fun getFileName(fileName: String, contentResolver: ContentResolver): Uri? {
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            ) else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val query = MediaStore.Images.Media.DISPLAY_NAME + " = ?"

        contentResolver.query(collection, projection, query, arrayOf(fileName), null)?.use { cursor ->
            if (cursor.count > 0) {
                cursor.moveToFirst()
                return ContentUris.withAppendedId(
                    collection,
                    cursor.getLong(0)
                )
            } else {
                return null
            }
        }

        return null
    }

    fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        var displayName: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            cursor.moveToFirst()
            displayName = cursor.getString(nameIndex)
        }

        return displayName
    }

    fun getUriFromGallery(contentResolver: ContentResolver, fileName: String): Uri? {
        var imageUri: Uri? = null
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndex(MediaStore.Images.Media._ID)
                val imageId = it.getLong(idColumn)
                imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId.toString())
            }
        }

        return imageUri
    }

    fun initBarcodeScanner(ctx: Context): GmsBarcodeScanner {
        val scanOptions = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .allowManualInput()
            .enableAutoZoom()
            .build()
        return GmsBarcodeScanning.getClient(ctx, scanOptions)
    }

    fun initBeebSound(ctx: Context): MediaPlayer? {
        return MediaPlayer.create(ctx, R.raw.beep_scanner)
    }

    fun filterProductByName(q: String, dataProduct: List<ProductModel>): List<ProductModel> {
        val matchingProducts = mutableListOf<ProductModel>()
        if (q.isEmpty()) return dataProduct
        for (product in dataProduct) {
            val pattern = Regex(Regex.escape(q), setOf(RegexOption.IGNORE_CASE))
            if (pattern.containsMatchIn(product.name)) {
                matchingProducts.add(product)
            }
        }
        return matchingProducts
    }

    fun filterProductByBarcode(q: String, dataProduct: List<ProductModel>): List<ProductModel>? {
        val matchingProducts = mutableListOf<ProductModel>()
        if (q.isEmpty()) return null
        for (product in dataProduct) {
            val pattern = Regex(Regex.escape(q))
            if (pattern.containsMatchIn(product.barcode)) {
                matchingProducts.add(product)
            }
        }
        return matchingProducts
    }

    fun getDiscountNominal(discount: Double, price: Int, qty: Int): Int{
        val totalPrice = price * qty
        val discountPercentage = discount / 100
        val discountNominal = discountPercentage * totalPrice
        return discountNominal.toInt()
    }

    fun getDiscountNominal(nominal: Int, discount: Double): Int {
        val discountPercentage = discount / 100
        val totalDiscount = discountPercentage * nominal
        return totalDiscount.toInt()
    }

    fun getTotalPriceAfterDiscount(discount: Double, price: Int, qty: Int): Int {
        val totalPrice = price * qty
        val discountNominal = getDiscountNominal(discount, price, qty)
        return totalPrice - discountNominal
    }

    fun setPriceAfterDiscount(
        targetBeforeDiscount: TextView,
        targetAfterDiscount: TextView,
        discount: Double,
        price: Int,
        qty: Int,
        needRp: Boolean,
        ctx: Context
    ) {
        try {
            val totalPrice = price * qty
            val totalPriceStr: String = if (needRp) {
                "Rp ${this.intToRupiah(totalPrice)}"
            } else {
                this.intToRupiah(totalPrice)
            }

            targetBeforeDiscount.text = totalPriceStr
            targetBeforeDiscount.paintFlags = targetBeforeDiscount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            targetBeforeDiscount.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            targetBeforeDiscount.setTypeface(null, Typeface.NORMAL)
            targetBeforeDiscount.setTypeface(null, Typeface.BOLD)

            if (discount == 0.0) {
                targetAfterDiscount.visibility = View.GONE
            } else {
                val totalPriceAfterDiscount = getTotalPriceAfterDiscount(discount, price, qty)
                val totalPriceAfterDiscountStr = if (needRp) {
                    "Rp ${this.intToRupiah(totalPriceAfterDiscount.toInt())}"
                } else {
                    this.intToRupiah(totalPriceAfterDiscount.toInt())
                }
                targetBeforeDiscount.paintFlags = targetBeforeDiscount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                targetBeforeDiscount.setTextColor(ContextCompat.getColor(ctx, R.color.text_gray))
                targetBeforeDiscount.setTypeface(null, Typeface.ITALIC)

                targetAfterDiscount.visibility = View.VISIBLE
                targetAfterDiscount.text = totalPriceAfterDiscountStr
            }
        } catch (e: Exception) {
            this.generateTOA(ctx, "Something wrong!!\nPlease make sure you give the correct input", true)
        }
    }

    fun strToDouble(v: String): Double {
        return try {
            v.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
    }

    fun strToInt(v: String): Int {
        return try {
            v.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getTotalPayment(t: TransactionEntity): Int {
        return try {
            var result = 0
            result += t.dataTransaction.subTotalProduct
            result -= t.dataTransaction.discountNominal
            result += t.dataTransaction.taxNominal
            result += t.dataTransaction.adm
            result
        } catch (e: Exception) {
            0
        }
    }

    // for set rupiah in edit text
    fun setSelectionEditText(edt: EditText, sS: Int, sE: Int) {
        val selection = if (sS < sE.toString().length)
            sS else this.intToRupiah(sE).length
        edt.setSelection(selection)
    }

    fun setEditTextWithRupiahFormat(e: EditText, nominal: Int) {
        e.setText(this.intToRupiah(nominal))
    }
}