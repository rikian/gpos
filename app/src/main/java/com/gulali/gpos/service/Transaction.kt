package com.gulali.gpos.service
//
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import androidx.appcompat.app.AppCompatActivity
//import com.gulali.gpos.adapter.ProductSearch
//import com.gulali.gpos.database.CartEntity
//import com.gulali.gpos.database.CartPaymentEntity
//import com.gulali.gpos.database.DataTransaction
//import com.gulali.gpos.database.ProductModel
//import com.gulali.gpos.databinding.TransactionAddBinding
//import com.gulali.gpos.helper.Helper
//
//class Transaction : AppCompatActivity() {
//    private lateinit var binding: TransactionAddBinding
//    private lateinit var helper: Helper
//    private lateinit var cartPayment: CartPaymentEntity
//    private lateinit var cartProducts: List<CartEntity>
//    private lateinit var dataTransaction: DataTransaction
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        TransactionAddBinding.inflate(layoutInflater).also {
//            binding = it
//            setContentView(binding.root)
//            helper = Helper()
//            dataTransaction = DataTransaction(
//                totalItem= 0,
//                subTotalProduct= 0,
//                discountNominal= 0,
//                discountPercent= 0.0,
//                taxNominal= 0,
//                taxPercent= 0.0,
//                adm= 0,
//                cash= 0,
//                grandTotal= 0,
//            )
//            cartPayment = CartPaymentEntity(
//                id = helper.generatePaymentID(),
//                dataTransaction = dataTransaction
//            )
//            var querySearchProduct = ""
//            binding.searchProduct.addTextChangedListener(object : TextWatcher {
//                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//                override fun afterTextChanged(s: Editable?) {
//                    val currentQuery = s.toString().lowercase()
//                    if (querySearchProduct == currentQuery) {
//                        return
//                    }
//                    querySearchProduct = currentQuery
//                    cartProducts = gposRepo.getProductByName(querySearchProduct)
//                    if (cartProducts.isEmpty()) {
//                        return
//                    }
//                    binding.productSearchView.adapter = dialog.showAvailableProducts(
//                        products,
//                        cartPaymentEntity.id
//                    )
//                }
//            }
//        }
//    }
//
//    fun showAvailableProducts(products: List<ProductModel>, idPayment: String): ProductSearch {
//        val productSearch = ProductSearch(products, this.helper, this.activity, contentResolver)
//        productSearch.setOnItemClickListener(object : ProductSearch.OnItemClickListener {
//            override fun onItemClick(position: Int) {
//                checkExistenceProduct(products[position], idPayment)
//            }
//        })
//        productSearch.notifyDataSetChanged()
//        return productSearch
//    }
//}