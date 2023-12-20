package com.gulali.gpos.repository

import android.content.Context
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.helper.Helper

data class OwnerRepoParam(
    var db: AdapterDb,
    var helper: Helper,
    var ctx: Context
)
class OwnerRepo(op: OwnerRepoParam) {
    private val repo = op.db.ownerDao()
    private val h = op.helper
    private val ctx = op.ctx

    fun getOwnerData(): List<OwnerEntity>? {
        return try {
            this.repo.getOwner()
        } catch (e: Exception) {
            null
        }
    }

    fun createOwner(data: OwnerEntity): Boolean{
        return try {
            repo.createOwner(data)
            true
        } catch (e: Exception) {
            this.h.generateTOA(this.ctx, e.message.toString(), true)
            false
        }
    }
}