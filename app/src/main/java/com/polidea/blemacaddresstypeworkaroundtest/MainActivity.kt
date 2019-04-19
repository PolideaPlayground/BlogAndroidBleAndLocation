package com.polidea.blemacaddresstypeworkaroundtest

import android.bluetooth.BluetoothAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.exceptions.BleException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import java.util.concurrent.TimeUnit

const val TEST_PERIPHERAL_MAC_ADDRESS = "D8:E6:BE:4A:F7:22" // author's nRF51 devkit — change it to your peripheral

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var rxBleClient: RxBleClient
    private lateinit var testDevice: RxBleDevice
    private lateinit var testDisposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rxBleClient = RxBleClient.create(this)
        testDevice = rxBleClient.getBleDevice(TEST_PERIPHERAL_MAC_ADDRESS)

        RxJavaPlugins.setErrorHandler { error ->
            if (error is UndeliverableException && error.cause is BleException) {
                return@setErrorHandler // ignore BleExceptions as they were surely delivered at least once
            }
            // add other custom handlers if needed
            throw error
        }
    }

    override fun onStart() {
        super.onStart()
        val textView: TextView = findViewById(R.id.text)
        testDisposable = runTest()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.i("TEST", it.toString())
                    textView.text = it.toString()
                },
                {
                    Log.e("TEST", "Failure", it)
                    textView.text = "Failure, check Logcat"
                }
            )
    }

    override fun onStop() {
        super.onStop()
        testDisposable.apply { dispose() }
    }

    private fun autoConnectScanTimeoutRace() = Single.amb<RaceResult>(
        listOf(
            testDevice.establishConnection(true)
                .takeSingle()
                .map { RaceResult.DeviceConnected },
            bluetoothClassicScan(this, bluetoothAdapter)
                .filter { it.address == testDevice.macAddress }
                .takeSingle()
                .map { RaceResult.DeviceScanned },
            Completable.timer(30, TimeUnit.SECONDS)
                .andThen(Single.just(RaceResult.Timeout))
        )
    )

    private fun singleTest() = autoConnectScanTimeoutRace()
        .toObservable()
        .flatMap(
            { raceResult ->
                when (raceResult) {
                    RaceResult.DeviceConnected -> Observable.just(true)
                    else -> testDevice.establishConnection(false)
                        .map { true } // if success
                        .onErrorReturn { false } // if error
                }
            },
            { raceResult, connectionSuccess ->
                TestResult(connectionSuccess, raceResult)
            }
        )
        .takeSingle()

    private fun measureExecutionTime(): SingleTransformer<TestResult, MeasuredSingleResult<TestResult>> = SingleTransformer { upstream ->
        Single.zip(
            Single.fromCallable<Long> { System.currentTimeMillis() }, // single test start time
            upstream,
            BiFunction<Long, TestResult, MeasuredSingleResult<TestResult>> { startTime, testResult ->
                val executionTimeInMillis = System.currentTimeMillis() - startTime
                MeasuredSingleResult(testResult, executionTimeInMillis)
            }
        )
    }

    private fun runTest() = Completable.concat(
        listOf(
            turnBluetoothAdapterOff(this, bluetoothAdapter).delay(2, TimeUnit.SECONDS),
            turnBluetoothAdapterOn(this, bluetoothAdapter).delay(2, TimeUnit.SECONDS)
        )
    )
        .andThen(singleTest().compose(measureExecutionTime()))
        .repeatWhen { it }
        .scan(TestResultAccumulator(), BiFunction(TestResultAccumulator::add))

    private fun <T> Observable<T>.takeSingle(): Single<T> = this.take(1).singleOrError()
}
