package com.polidea.blemacaddresstypeworkaroundtest

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.polidea.rxandroidble2.RxBleAdapterStateObservable
import io.reactivex.Completable


fun turnBluetoothAdapterOn(context: Context, bluetoothAdapter: BluetoothAdapter): Completable = changeBluetoothAdapterState(
    context, RxBleAdapterStateObservable.BleAdapterState.STATE_ON, bluetoothAdapter::enable
)

fun turnBluetoothAdapterOff(context: Context, bluetoothAdapter: BluetoothAdapter): Completable = changeBluetoothAdapterState(
    context, RxBleAdapterStateObservable.BleAdapterState.STATE_OFF, bluetoothAdapter::disable
)

private fun changeBluetoothAdapterState(
    context: Context,
    targetState: RxBleAdapterStateObservable.BleAdapterState,
    stateChanger: () -> Boolean
) = Completable.create { completableEmitter ->
    val disposable = RxBleAdapterStateObservable(context.applicationContext)
        .filter { it == targetState }
        .take(1)
        .ignoreElements()
        .subscribe(
            completableEmitter::onComplete,
            completableEmitter::onError
        )
    completableEmitter.setDisposable(disposable)
    val success = stateChanger.invoke()
    if (!success) completableEmitter.tryOnError(RuntimeException("Cannot turn BluetoothAdapter to $targetState"))
}
