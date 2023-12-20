package com.gulali.gpos.repository

import android.content.Context
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.CartEntity
import com.gulali.gpos.database.CartPaymentEntity
import com.gulali.gpos.helper.Helper

data class CartRepoParam(
    var db: AdapterDb,
    var helper: Helper,
    var ctx: Context
)

class CartRepo(p: CartRepoParam) {
    private val repo = p.db.cartDao()
    private val h = p.helper
    private val ctx = p.ctx

    fun saveProductInCart(data: List<CartEntity>): Boolean {
        return try {
            this.repo.saveCart(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateProductInCart(data: CartEntity): Int {
        return try {
            this.repo.updateProductInCart(data)
        } catch (e: Exception) {
            0
        }
    }

    fun deleteProductInCart(idPayment: String, idProduct: Int): Int {
        return try {
            this.repo.deleteProductInCart(idPayment, idProduct)
        } catch (e: Exception) {
            0
        }
    }
    fun countProductInCart(idPayment: String): Int {
        return try {
            this.repo.countProductInCart(idPayment)
        } catch (e: Exception) {
            0
        }
    }
    fun getCurrentTotalPriceInCart(idPayment: String): Int {
        return this.repo.getCurrentTotalPriceInCart(idPayment)
    }
    fun getProductsInCart(id: String): List<CartEntity> {
        return this.repo.getProductsInCart(id)
    }

    fun getProductInCart(idPayment: String, idProduct: Int): CartEntity? {
        return try {
            this.repo.getProductInCart(idPayment, idProduct)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCartPayment(data: CartPaymentEntity): Boolean {
        return try {
            this.repo.saveCartPayment(data)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCartPayment(idPayment: String): CartPaymentEntity? {
        return try {
            this.repo.getCartPayment(idPayment)
        } catch (e: Exception) {
            null
        }
    }

    fun truncateCartPayment(): Boolean {
        return try {
            this.repo.truncateCartPayment()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun updateCartPayment(data: CartPaymentEntity): Boolean {
        return try {
            this.repo.updateCartPayment(data)
            true
        } catch (e: Exception) {
            false
        }
    }
}