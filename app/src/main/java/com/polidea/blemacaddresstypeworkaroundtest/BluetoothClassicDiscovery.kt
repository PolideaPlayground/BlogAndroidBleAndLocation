package com.polidea.blemacaddresstypeworkaroundtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


fun bluetoothClassicScan(context: Context, bluetoothAdapter: BluetoothAdapter): Observable<BluetoothDevice> {
    val applicationContext = context.applicationContext
    val repeatedBluetoothClassicScan = bluetoothClassicScanRun(applicationContext, bluetoothAdapter)
        .repeatWhen { it.delay(1, TimeUnit.SECONDS) }
    return bluetoothClassicFoundDevices(applicationContext)
        .mergeWith(repeatedBluetoothClassicScan)
}

private fun bluetoothClassicFoundDevices(context: Context): Observable<BluetoothDevice> = Observable.create {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bluetoothDevice: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            Log.d("SCAN", "Device address: ${bluetoothDevice.address}")
            it.onNext(bluetoothDevice)
        }
    }
    context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    it.setCancellable { context.unregisterReceiver(receiver) }
}

private fun bluetoothClassicScanRun(context: Context, bluetoothAdapter: BluetoothAdapter): Completable = Completable.create {
    val isScanStarted = AtomicBoolean(false)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("SCAN", "FINISHED")
            isScanStarted.set(false)
            it.onComplete()
        }
    }
    context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    it.setCancellable {
        if (bluetoothAdapter.cancelDiscovery() && isScanStarted.get()) Log.d("SCAN", "CANCELLED")
        context.unregisterReceiver(receiver)
    }
    if (!bluetoothAdapter.startDiscovery()) {
        it.tryOnError(RuntimeException("Could not start a Bluetooth Classic peripheral discovery"))
    } else {
        isScanStarted.set(true)
        Log.d("SCAN", "STARTED")
    }
}