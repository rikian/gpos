package com.gulali.gpos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.RegistrationBinding
import com.gulali.gpos.helper.Helper

class Registration: AppCompatActivity() {
    private lateinit var binding: RegistrationBinding
    private lateinit var gposRepo: Repository
    private lateinit var dataOwner: List<OwnerEntity>
    private lateinit var helper: Helper
    private var data = OwnerEntity(
        id = "001",
        shop = "",
        owner = "",
        address= "" ,
        phone= "",
        discountPercent= 0.0,
        discountNominal= 0,
        taxPercent = 0.0,
        taxNominal = 0,
        adm= 0,
        bluetoothPaired = "",
        createdAt= "",
        updatedAt= "",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RegistrationBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            binding.btnRegistration.setOnClickListener {
                val owner = binding.regOwner.text.toString().trim()
                if (owner == "") {
                    helper.generateTOA(this, "owner cannot be empty", true)
                    return@setOnClickListener
                }
                val shopName = binding.regShopName.text.toString().trim()
                if (shopName == "") {
                    helper.generateTOA(this, "shop name cannot be empty", true)
                    return@setOnClickListener
                }
                val regAddress = binding.regAddress.text.toString().trim()
                if (regAddress == "") {
                    helper.generateTOA(this, "address cannot be empty", true)
                    return@setOnClickListener
                }
                val regPhone = binding.regPhone.text.toString()
                if (regPhone == "") {
                    helper.generateTOA(this, "phone number cannot be empty", true)
                    return@setOnClickListener
                }

                data.owner = owner
                data.shop = shopName
                data.address = regAddress
                data.phone = regPhone
                gposRepo.createOwner(data)
                Intent(this, Index::class.java).also { index -> startActivity(index) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dataOwner = gposRepo.getOwner()
        if (dataOwner.isNotEmpty()) {
            Intent(this, Index::class.java).also { index -> startActivity(index) }
            finish()
        }
    }
}