package com.gulali.gpos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.databinding.RegistrationBinding
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.repository.OwnerRepo
import com.gulali.gpos.repository.OwnerRepoParam

class Registration: AppCompatActivity() {
    private lateinit var binding: RegistrationBinding
    private lateinit var ownerRepo: OwnerRepo
    private lateinit var helper: Helper
    private var dateTimeLong = DataTimeLong(
        created = 0,
        updated = 0
    )
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
        date = dateTimeLong
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RegistrationBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            ownerRepo = OwnerRepo(OwnerRepoParam(
                ctx = this,
                helper = helper,
                db = AdapterDb.getGposDatabase(this)
            ))
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
                data.date.created = helper.getCurrentDate()
                data.date.updated = helper.getCurrentDate()
                if (!ownerRepo.createOwner(data)) return@setOnClickListener
                launchIndex(this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dataOwner = ownerRepo.getOwnerData()
        if (!dataOwner.isNullOrEmpty()) {
            return launchIndex(this)
        }
    }

    private fun launchIndex(ctx: Context) {
        val intent = Intent(ctx, Index::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}