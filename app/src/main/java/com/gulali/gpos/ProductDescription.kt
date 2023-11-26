package com.gulali.gpos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.databinding.ProductDescriptionBinding

class ProductDescription : AppCompatActivity() {
    private lateinit var binding: ProductDescriptionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProductDescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}