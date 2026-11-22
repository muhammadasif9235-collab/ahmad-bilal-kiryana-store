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
import com.example.data.Order
import com.example.data.RationCardBill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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

    suspend fun printOrderReceipt(deviceAddress: String, order: Order, shopPhone: String): Result<Unit> {
        val bytes = ReceiptFormatter.buildEscPosBytes(order, shopPhone)
        return printBytes(deviceAddress, bytes)
    }

    suspend fun printRationCardReceipt(deviceAddress: String, bill: RationCardBill, shopPhone: String): Result<Unit> {
        val bytes = ReceiptFormatter.buildRationCardEscPosBytes(bill, shopPhone)
        return printBytes(deviceAddress, bytes)
    }

    suspend fun printTestSlip(deviceAddress: String): Result<Unit> {
        val testText = """
            ================================
               AHMAD BILAL KIRYANA STORE
                TANDA, DISTRICT GUJRAT
            ================================
            Bluetooth Thermal Printer Test
            Status: CONNECTED & READY
            ================================



        """.trimIndent().toByteArray(Charsets.UTF_8)
        return printBytes(deviceAddress, byteArrayOf(0x1B, 0x40) + testText)
    }
}
