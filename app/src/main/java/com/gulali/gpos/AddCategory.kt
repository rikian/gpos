package com.gulali.gpos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.CategoryEntity
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.CategoryAddBinding
import com.gulali.gpos.helper.Helper

class AddCategory : AppCompatActivity() {
    private lateinit var binding: CategoryAddBinding
    private lateinit var helper: Helper
    private lateinit var categoryEntity: CategoryEntity
    private lateinit var gposRepo: Repository
    private var duplicateConstraint = "UNIQUE constraint failed: categories.name (code 2067 SQLITE_CONSTRAINT_UNIQUE[2067])"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CategoryAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()

            binding.saveCategory.setOnClickListener{
                val categoryName = binding.categoryName.text.toString().trim()
                if (categoryName == "") {
                    helper.generateTOA(this, "Category name cannot be empty", true)
                    return@setOnClickListener
                }

                val dateTimeLong = DataTimeLong(
                    created = helper.getCurrentDate(),
                    updated = helper.getCurrentDate(),
                )

                categoryEntity = CategoryEntity(
                    name = categoryName,
                    date = dateTimeLong
                )

                try {
                    gposRepo.insertCategory(categoryEntity)
                    finish()
                } catch (e: Exception) {
                    if (e.message == duplicateConstraint) {
                        helper.generateTOA(this, "unit $categoryName already exist", false)
                        return@setOnClickListener
                    } else {
                        helper.generateTOA(this, "An error occurred while saving the unit", false)
                    }
                }
            }
        }
    }
}