package com.gulali.gpos.constant

import android.content.pm.PackageManager

class Constant {
    fun needUpdate(): String {
        return "NEED_UPDATE"
    }

    fun product(): String {
        return "PRODUCT"
    }

    fun idTransaction(): String {
        return "ID_TRANSACTION"
    }

    fun fromPayment(): String {
        return "FROM_PAYMENT"
    }

    fun getOKGranted(): Int {
        return PackageManager.PERMISSION_GRANTED
    }
}