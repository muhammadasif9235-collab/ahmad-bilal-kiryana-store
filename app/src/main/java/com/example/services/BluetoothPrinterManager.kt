package com.example.services

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.LedgerEntry
import com.example.data.Order
import com.example.data.RationCardBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.UUID

data class PairedBluetoothPrinter(
    val name: String,
    val address: String
)

class BluetoothPrinterManager(private val context: Context) {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private fun bluetoothAdapter(): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothAvailableAndEnabled(): Boolean {
        val adapter = bluetoothAdapter() ?: return false
        return adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Result<List<PairedBluetoothPrinter>> {
        if (!hasBluetoothPermission()) {
            return Result.failure(SecurityException("Bluetooth permission not granted"))
        }
        val adapter = bluetoothAdapter()
            ?: return Result.failure(IllegalStateException("Bluetooth hardware not available on this device"))
        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is turned off"))
        }
        val bonded: Set<BluetoothDevice> = adapter.bondedDevices ?: emptySet()
        val list = bonded.map { device ->
            PairedBluetoothPrinter(
                name = device.name?.ifBlank { "Bluetooth Device" } ?: "Bluetooth Device",
                address = device.address ?: ""
            )
        }.filter { it.address.isNotBlank() }
        return Result.success(list)
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(deviceAddress: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(SecurityException("Bluetooth permission required"))
        }
        if (deviceAddress.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No Bluetooth printer selected"))
        }
        val adapter = bluetoothAdapter()
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth unavailable"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Please turn on Bluetooth"))
        }

        var socket: BluetoothSocket? = null
        try {
            val device = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            socket.outputStream.apply {
                write(data)
                flush()
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("Could not connect to printer: ${e.localizedMessage ?: "I/O error"}", e))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
                // ignore close errors
            }
        }
    }

    suspend fun printOrderReceipt(
        deviceAddress: String,
        order: Order,
        shopPhone: String,
        shopNameEnglish: String = ReceiptFormatter.SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = ReceiptFormatter.SHOP_CITY_ENGLISH
    ): Result<Unit> {
        val bytes = ReceiptFormatter.buildEscPosBytes(
            order = order,
            shopPhone = shopPhone,
            shopNameEnglish = shopNameEnglish,
            shopAddressEnglish = shopAddressEnglish
        )
        return printBytes(deviceAddress, bytes)
    }

    suspend fun printPaymentReceipt(
        deviceAddress: String,
        entry: LedgerEntry,
        shopPhone: String,
        shopNameEnglish: String = ReceiptFormatter.SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = ReceiptFormatter.SHOP_CITY_ENGLISH
    ): Result<Unit> {
        val bytes = ReceiptFormatter.buildPaymentEscPosBytes(
            entry = entry,
            shopPhone = shopPhone,
            shopNameEnglish = shopNameEnglish,
            shopAddressEnglish = shopAddressEnglish
        )
        return printBytes(deviceAddress, bytes)
    }

    suspend fun printRationCardReceipt(
        deviceAddress: String,
        bill: RationCardBill,
        shopPhone: String,
        shopNameEnglish: String = ReceiptFormatter.SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = ReceiptFormatter.SHOP_CITY_ENGLISH
    ): Result<Unit> {
        val bytes = ReceiptFormatter.buildRationCardEscPosBytes(
            bill = bill,
            shopPhone = shopPhone,
            shopNameEnglish = shopNameEnglish,
            shopAddressEnglish = shopAddressEnglish
        )
        return printBytes(deviceAddress, bytes)
    }

    suspend fun printTestSlip(
        deviceAddress: String,
        shopNameEnglish: String = ReceiptFormatter.SHOP_NAME_ENGLISH,
        shopAddressEnglish: String = ReceiptFormatter.SHOP_CITY_ENGLISH,
        shopPhone: String = ""
    ): Result<Unit> {
        val nameLine = shopNameEnglish.ifBlank { ReceiptFormatter.SHOP_NAME_ENGLISH }.uppercase(Locale.US).take(32)
        val addrLine = shopAddressEnglish.ifBlank { ReceiptFormatter.SHOP_CITY_ENGLISH }.take(32)
        val phoneLine = if (shopPhone.isNotBlank()) "Tel: $shopPhone\n" else ""
        val testText = (
            "================================\n" +
                "$nameLine\n" +
                "$addrLine\n" +
                phoneLine +
                "================================\n" +
                "58mm ESC/POS Thermal Test\n" +
                "Status: CONNECTED & READY\n" +
                "================================\n\n\n\n"
            ).toByteArray(Charsets.UTF_8)
        return printBytes(
            deviceAddress,
            byteArrayOf(0x1B, 0x40, 0x1B, 0x61, 0x01) + testText + byteArrayOf(0x1D, 0x56, 0x41, 0x03)
        )
    }
}
