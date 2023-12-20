package com.gulali.gpos.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class Permission(
    private val ctx: Context,
    private val funRequest: ActivityResultLauncher<String>
) {
    val granted = PackageManager.PERMISSION_GRANTED
    private fun isBluetoothPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) == granted
    }
    private fun isBluetoothAdminPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN) == granted
    }
    private fun isBluetoothAccessCoarseLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == granted
    }
    fun checkBluetoothPermission(): Boolean {
        if (!this.isBluetoothPermissionGranted()) {
            this.funRequest.launch(Manifest.permission.BLUETOOTH)
            return false
        }
        if (!this.isBluetoothAdminPermissionGranted()) {
            this.funRequest.launch(Manifest.permission.BLUETOOTH_ADMIN)
            return false
        }
        if (!this.isBluetoothAccessCoarseLocationPermissionGranted()) {
            this.funRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != this.granted) {
                this.funRequest.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return false
            }
        }

        return true
    }
}