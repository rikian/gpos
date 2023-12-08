package com.gulali.gpos

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.CategoryEntity
import com.gulali.gpos.database.ProductEntity
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.UnitEntity
import com.gulali.gpos.databinding.ProductAddBinding
import com.gulali.gpos.helper.Helper
import java.io.File
import java.util.UUID

class AddProduct : AppCompatActivity() {
    private lateinit var binding: ProductAddBinding
    private lateinit var helper: Helper
    private lateinit var constant: Constant
    private var imageFile: File? = null
    private var imageNameFromGallery: String? = null
    private var correctedBitmap: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 2
    private lateinit var gposRepo: Repository
    private val createNew = "Create new"
    private lateinit var productEntity: ProductEntity
    private lateinit var scanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    private var needUpdate: Boolean = false
    private lateinit var pUpdate: ProductModel
    private var idProduct: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            constant = Constant()
            needUpdate = intent.getBooleanExtra(constant.needUpdate(), false)
            idProduct = intent.getIntExtra(constant.product(), -1)
            binding.anpEdtPurchase.setRawInputType(2)
            binding.anpPriceA.setRawInputType(2)
            scanner = helper.initBarcodeScanner(this)
            mediaPlayer = helper.initBeebSound(this)

            // default product entity
            productEntity = ProductEntity(
                image = "default.jpeg",
                name = "",
                category = 0,
                barcode = "",
                stock = 0,
                unit = 0,
                purchase = 0,
                price = 0,
                createdAt = "",
                updatedAt = ""
            )
        }

        if (needUpdate && idProduct != -1) {
            try {
                val textUpdate = "Update Product"
                binding.apc.text = textUpdate
                pUpdate = gposRepo.getProductByID(idProduct)

                // set default image product and created at
                productEntity.image = pUpdate.img
                productEntity.createdAt = pUpdate.created
                productEntity.purchase = pUpdate.purchase
                productEntity.price = pUpdate.price
                productEntity.id = pUpdate.id

                binding.inptBarcode.setText(pUpdate.barcode)
                binding.edtProductName.setText(pUpdate.name)
                binding.anpQtyTot.setText(pUpdate.stock.toString())
                binding.anpEdtPurchase.setText(helper.intToRupiah(pUpdate.purchase))
                binding.anpPriceA.setText(helper.intToRupiah(pUpdate.price))
                val uri = helper.getUriFromGallery(contentResolver, pUpdate.img)
                if (uri != null) {
                    Glide.with(this)
                        .load(uri)
                        .into(binding.imgPrevProduct)
                }
                binding.cUpdate.visibility = View.VISIBLE
                binding.btnSaveProduct.visibility = View.GONE
                binding.upCancel.setOnClickListener { finish() }
                binding.upOk.setOnClickListener {
                    saveProduct(true)
                }
            } catch (e: Exception) {
                println(e.message)
                helper.generateTOA(this, "product not found", true)
                finish()
            }
        }

        // spinner section
        getListSpinner(gposRepo.getUnits())

        // handle capture image
        binding.btnTakeImgProduct.setOnClickListener {
            if (!getPermission()) {
                return@setOnClickListener
            }
            handleCaptureImage()
        }

        // handle get image from gallery
        binding.btnGetImage.setOnClickListener {
            if (!getPermission()) {
                return@setOnClickListener
            }
            openGallery()
        }

        // handle stock button
        helper.initialSockListener(binding.anpQtyMin, binding.anpQtyPlus, binding.anpQtyTot)

        // handle purchase
        binding.anpEdtPurchase.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = binding.anpEdtPurchase.selectionStart
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (isFinish) {
                        isFinish = false
                        val sp3 = if (cp < productEntity.purchase.toString().length)
                            cp else helper.intToRupiah(productEntity.purchase).length
                        binding.anpEdtPurchase.setSelection(sp3)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (productEntity.purchase == userInput) {
                        isFinish = true
                        binding.anpEdtPurchase.setText(helper.intToRupiah(productEntity.purchase))
                        return
                    }
                    productEntity.purchase = userInput
                    isFinish = true
                    binding.anpEdtPurchase.setText(helper.intToRupiah(productEntity.purchase))
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        // handle price
        binding.anpPriceA.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = binding.anpPriceA.selectionStart
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (isFinish) {
                        isFinish = false
                        val sp3 = if (cp < productEntity.price.toString().length)
                            cp else helper.intToRupiah(productEntity.price).length
                        binding.anpPriceA.setSelection(sp3)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (productEntity.price == userInput) {
                        isFinish = true
                        binding.anpPriceA.setText(helper.intToRupiah(productEntity.price))
                        return
                    }
                    productEntity.price = userInput
                    isFinish = true
                    binding.anpPriceA.setText(helper.intToRupiah(productEntity.price))
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        // handle barcode
        binding.btnScanBc2.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener {
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }
                    binding.inptBarcode.setText(it.rawValue)
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnBackTbc.setOnClickListener {
            finish()
        }

        binding.btnSaveProduct.setOnClickListener {
            saveProduct(false)
        }
    }

    override fun onResume() {
        super.onResume()
        getListSpinner(gposRepo.getUnits())
        getListCategory(gposRepo.getCategories())
    }

    private fun saveProduct(isUpdate: Boolean) {
        try {
            // check image
            if (correctedBitmap != null) {
                productEntity.image = "${UUID.randomUUID()}.jpeg"
            }

            if (imageNameFromGallery != null && imageNameFromGallery != "") {
                productEntity.image = imageNameFromGallery as String
            }

            // check barcode
            productEntity.barcode = binding.inptBarcode.text.toString()

            // check name
            val productName = binding.edtProductName.text.toString().trim()
            if (productName == "") {
                helper.generateTOA(this, "Product name cannot be empty", true)
                return
            }
            productEntity.name = productName

            // check stock
            var stock = binding.anpQtyTot.text.toString().toIntOrNull()
            if (stock == null) {
                stock = 0
            }
            productEntity.stock = stock

            // check unit
            val idxUnit = helper.readUnitByName(binding.spinner.selectedItem.toString(), gposRepo.getUnits())
            if (idxUnit == null) {
                helper.generateTOA(this, "Unit cannot be empty", false)
                return
            }
            productEntity.unit = idxUnit.id

            // check category
            val idxCategory = helper.readCategoryByName(binding.spinnerCategory.selectedItem.toString(), gposRepo.getCategories())
            if (idxCategory == null) {
                helper.generateTOA(this, "Category cannot be empty", false)
                return
            }
            productEntity.category = idxCategory.id

            // check purchase
            if (productEntity.purchase == 0) {
                helper.generateTOA(this, "Purchase cannot be empty", false)
                return
            }

            // check price
            if (productEntity.price == 0) {
                helper.generateTOA(this, "Price cannot be empty", false)
                return
            }

            // check date
            if (isUpdate) {
                productEntity.updatedAt = helper.getDate()
                gposRepo.updateProduct(productEntity)
                helper.generateTOA(this, "success update product", true)
            } else {
                productEntity.createdAt = helper.getDate()
                productEntity.updatedAt = helper.getDate()
                gposRepo.insertProduct(productEntity)
                helper.generateTOA(this, "success save product", true)
            }

            // save image to gallery if correct bitmap not null
            if (correctedBitmap != null) {
                helper.saveImageToLocalStorage(contentResolver, getString(R.string.app_name), correctedBitmap, productEntity.image)
            }
            finish()
        } catch (e: Exception) {
            helper.generateTOA(this, "failed!!\n\nerror: ${e.message}", false)
        }
    }

    private fun getListCategory(listSpinner: List<CategoryEntity>) {
        val listCategory = mutableListOf<String>()
        if (needUpdate && idProduct != -1) {
            listCategory.add(pUpdate.category)
            for (v in listSpinner) {
                if (v.name == pUpdate.category) continue
                listCategory.add(v.name)
            }
        } else {
            listCategory.add("")
            if (listSpinner.isNotEmpty()) {
                for (v in listSpinner) {
                    listCategory.add(v.name)
                }
            }
        }

        listCategory.add(createNew)
        val ad: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listCategory)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = ad
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = listCategory[position]
                if (selectedCategory == createNew) {
                    // Open a new activity to create a new unit
                    Intent(this@AddProduct, AddCategory::class.java).also {
                        startActivity(it)
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun getListSpinner(listSpinner: List<UnitEntity>) {
        val listUnit = mutableListOf<String>()
        if (needUpdate && idProduct != -1) {
            listUnit.add(pUpdate.unit)
            for (v in listSpinner) {
                if (v.name == pUpdate.unit) continue
                listUnit.add(v.name)
            }
        } else {
            listUnit.add("")
            if (listSpinner.isNotEmpty()) {
                for (v in listSpinner) {
                    listUnit.add(v.name)
                }
            }
        }
        listUnit.add(createNew)
        val ad: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listUnit)
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = ad
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedUnit = listUnit[position]
                if (selectedUnit == createNew) {
                    // Open a new activity to create a new unit
                    val intent = Intent(applicationContext, AddUnit::class.java)
                    startActivity(intent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when nothing is selected (if needed)
            }
        }
    }

    private fun handleCaptureImage() {
        // Create a temporary file to store the image
        imageFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${helper.generateUniqueFileName()}.jpeg")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Pass the file URI to the camera app
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageFile?.let { fileUri ->
                    FileProvider.getUriForFile(this, helper.getAuthority(), fileUri)
                })
                startActivityForResult(takePictureIntent, 1)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer when the activity is destroyed
        mediaPlayer?.release()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Load the captured image from the file
            correctedBitmap = imageFile?.let {BitmapFactory.decodeFile(it.absolutePath)} ?: return
            if (correctedBitmap!!.byteCount > 50000000) {
                helper.generateTOA(this, "image to large", false)
                // Delete the local file
                imageFile?.delete()
                imageFile = null
                return
            }
            // Rotate the image based on its orientation
            correctedBitmap = helper.rotateImageIfRequired(correctedBitmap, imageFile?.absolutePath ?: "")

            // set preview image
            binding.imgPrevProduct.setImageBitmap(correctedBitmap)

            // Delete the local file
            imageFile?.delete()
            imageFile = null

            // set the image name from gallery to empty string or null
            imageNameFromGallery = null
        }
        if (requestCode == 1 && resultCode == RESULT_CANCELED) {
            imageFile?.delete()
            imageFile = null
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            // Get the selected image URI
            val imageUri = data?.data
            imageNameFromGallery = imageUri?.let {
                helper.getFileNameFromUri(contentResolver, it)
            } ?: return

            // Set the image URI to the ImageView using Glide or Picasso for efficient loading
            Glide.with(this)
                .load(imageUri)
                .into(binding.imgPrevProduct)

            // set null the correct bitmap
            correctedBitmap = null
        }
    }

    private fun getPermission(): Boolean {
        val listPermission = mutableListOf<Boolean>()
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
            listPermission.add(false)
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            listPermission.add(false)
        }

        return listPermission.size == 0
    }
}