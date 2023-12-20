package com.gulali.gpos

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.HistoryStockEntity
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.HistoryStockBinding
import com.gulali.gpos.helper.Helper
import java.io.File
import java.io.IOException

class HistoryStock : AppCompatActivity() {
    private val constant: Constant = Constant()
    private var idProduct: Int = -1
    private lateinit var helper: Helper
    private lateinit var gposRepo: Repository
    private lateinit var historyStock: List<HistoryStockEntity>
    private lateinit var binding: HistoryStockBinding
    private lateinit var tableLayout: TableLayout
    private lateinit var inflater: LayoutInflater
    private var isNeedReload: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HistoryStockBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            idProduct = intent.getIntExtra(constant.product(), -1)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            tableLayout = binding.tbLayout
            inflater = LayoutInflater.from(this@HistoryStock)

        }
    }

    override fun onResume() {
        super.onResume()
        if (!isNeedReload) return
        if (idProduct == -1) return finish()
        historyStock = gposRepo.getStockHistoryById(idProduct)
        for (h in historyStock) {
            val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(h.date.created))

            // Inflate the history_stock_row.xml layout
            val tableRow: TableRow = inflater.inflate(R.layout.history_stock_row, binding.root, false) as TableRow

            // Access TextViews in the inflated layout and set their values
            val date: TextView = tableRow.findViewById(R.id.date)
            val time: TextView = tableRow.findViewById(R.id.time)
            val trID: TextView = tableRow.findViewById(R.id.transactionID)
            val stockIn: TextView = tableRow.findViewById(R.id.stockIn)
            val sPurchase: TextView = tableRow.findViewById(R.id.s_purchase)
            val stockOut: TextView = tableRow.findViewById(R.id.stockOut)
            val remainStock: TextView = tableRow.findViewById(R.id.remain_stock)

            date.text = dateTime.date
            time.text = dateTime.time
            trID.text = if (h.transactionID == "") "-" else h.transactionID
            sPurchase.text = if (h.purchase == 0) "-" else "Rp ${helper.intToRupiah(h.purchase)}"
            stockIn.text = h.inStock.toString()
            stockOut.text = h.outStock.toString()
            remainStock.text = h.currentStock.toString()

            tableRow.setOnClickListener {
                if (h.transactionID == "") return@setOnClickListener
                Intent(this, TransactionDetails::class.java).also {
                    it.putExtra(constant.idTransaction(), h.transactionID)
                    startActivity(it)
                }
            }

            // Add the inflated layout to the TableLayout
            tableLayout.addView(tableRow)
        }
        binding.btnToCsv.setOnClickListener {
            exportDatabaseToCSVFile(historyStock)
        }
        isNeedReload = false
    }

    private fun exportDatabaseToCSVFile(list: List<HistoryStockEntity>) {
        val csvFile = generateFile(getFileName())
        if (csvFile != null) {
            exportDirectorsToCSVFile(csvFile, list)
        } else {
            // Handle file creation failure
        }
    }

    private fun generateFile(fileName: String): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val csvFile = File(downloadsDir, fileName)

        return try {
            csvFile.createNewFile()
            csvFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(): String = "test.csv"

    private fun exportDirectorsToCSVFile(csvFile: File, list: List<HistoryStockEntity>) {
        csvWriter().open(csvFile, append = false) {
            // Header
            writeRow(listOf("Date", "Time", "Transaction ID", "Purchase", "In", "Out", "Stock"))
            list.forEachIndexed { _, h ->
                val dt = helper.formatSpecificDate(helper.unixTimestampToDate(h.date.created))
                writeRow(listOf(dt.date, dt.time, h.transactionID, h.purchase, h.inStock, h.outStock, h.currentStock))
            }

            // File has been written, no need to save to the directory again
        }
    }
}

// share your file, don't forget adding provider in your Manifest