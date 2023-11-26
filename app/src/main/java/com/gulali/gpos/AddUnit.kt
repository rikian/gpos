package com.gulali.gpos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.UnitEntity
import com.gulali.gpos.databinding.UnitAddBinding

class AddUnit : AppCompatActivity() {
    private lateinit var binding: UnitAddBinding
    private lateinit var helper: com.gulali.gpos.helper.Helper
    private lateinit var unitEntity: UnitEntity
    private lateinit var gposRepo: Repository
    private var duplecateConstraint = "UNIQUE constraint failed: units.name (code 2067 SQLITE_CONSTRAINT_UNIQUE[2067])"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }
        gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
        helper = com.gulali.gpos.helper.Helper()
        binding.saveUnit.setOnClickListener {
            val unitName = binding.unitName.text.toString()
            if (unitName == "") {
                helper.generateTOA(this, "Unit cannot be empty", true)
                return@setOnClickListener
            }

            unitEntity = UnitEntity(
                name = unitName,
                createdAt = helper.getDate(),
                updatedAt = helper.getDate()
            )

            try {
                gposRepo.insertUnit(unitEntity)
                finish()
            } catch (e: Exception) {
                if (e.message == duplecateConstraint) {
                    helper.generateTOA(this, "unit $unitName already exist", false)
                    return@setOnClickListener
                } else {
                    helper.generateTOA(this, "An error occurred while saving the unit", false)
                }
            }
        }
        binding.btnBackTbc.setOnClickListener {
            finish()
        }
    }
}