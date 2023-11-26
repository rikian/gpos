package com.gulali.gpos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gulali.gpos.adapter.ItemAdapter
import com.gulali.gpos.databinding.Main2Binding
import com.gulali.gpos.databinding.MainBinding
import com.gulali.gpos.helper.Helper

class Main : AppCompatActivity() {
    private lateinit var binding: Main2Binding
    private lateinit var helper: Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Main2Binding.inflate(layoutInflater).also {
            binding = it
            setContentView(it.root)
        }
    }
}