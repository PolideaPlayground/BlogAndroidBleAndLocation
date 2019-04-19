# BlogAndroidBleAndLocation

This is a test project that may be used to reproduce test described on Polidea's blogpost (link will appear later).

This Android application tests performance and feasibility of a particular implementation of workaround for [Android's Bluetooth MAC address
 type resolution bug](https://github.com/Polidea/RxAndroidBle/wiki/FAQ:-Cannot-connect#connect-without-a-scan) without using Location 
 access.

## Before installing the app
This application requires a MAC address set in `MainActivity.kt` — constant value named `TEST_PERIPHERAL_MAC_ADDRESS`.
