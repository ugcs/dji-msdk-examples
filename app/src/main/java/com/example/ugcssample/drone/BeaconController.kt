package com.example.ugcssample.drone

import dji.common.error.DJIError
import dji.common.flightcontroller.LEDsSettings
import dji.common.product.Model
import dji.common.util.CommonCallbacks
import dji.sdk.products.Aircraft
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class BeaconController {
    private var aircraft: Aircraft? = null

    fun init(aircraft: Aircraft) {
        this.aircraft = aircraft
    }

    private fun isM300() = aircraft?.model == Model.MATRICE_300_RTK

    fun areBeaconsSupported(): Boolean {
        return if (isM300()) {
            true
        } else {
            val beacon = aircraft?.accessoryAggregation?.beacon
            beacon != null && beacon.isConnected
        }
    }

    suspend fun areBeaconsSwitchOn(): Boolean {
        return suspendCoroutine { continuation ->
            if (isM300()) {
                aircraft?.flightController?.getLEDsEnabledSettings(object : CommonCallbacks.CompletionCallbackWith<LEDsSettings> {
                    override fun onSuccess(settings: LEDsSettings?) {
                        if (settings != null) {
                            Timber.i("areBeaconsSwitchOn beaconsOn = ${settings.areBeaconsOn()}")
                            continuation.resume(settings.areBeaconsOn())
                        }
                    }

                    override fun onFailure(djiError: DJIError?) {
                        continuation.resumeWithException(Exception(djiError?.description.orEmpty()))
                    }
                })
            } else {
                val beacon = aircraft?.accessoryAggregation?.beacon
                beacon?.getEnabled(object : CommonCallbacks.CompletionCallbackWith<Boolean> {
                    override fun onSuccess(isBeaconOn: Boolean?) {
                        Timber.i("areBeaconsSwitchOn isBeaconOn = $isBeaconOn")
                        continuation.resume(isBeaconOn == true)
                    }

                    override fun onFailure(djiError: DJIError?) {
                        continuation.resumeWithException(Exception(djiError?.description.orEmpty()))
                    }
                })
            }
        }
    }

    suspend fun switchBeaconsOn(beaconsOn: Boolean): Boolean {
        Timber.i("switchBeaconsOn beaconsOn = $beaconsOn")
        return suspendCoroutine { continuation ->
            if (isM300()) {
                val ledSettingsBuilder = LEDsSettings.Builder().beaconsOn(beaconsOn)
                Timber.d("switchBeaconsOn For M300")
                aircraft?.flightController?.setLEDsEnabledSettings(ledSettingsBuilder.build()) { djiError ->
                    if (djiError != null) {
                        Timber.e("switchBeaconsOn #${djiError.errorCode}: ${djiError.description}")
                        continuation.resumeWithException(Exception(djiError.description))
                    }
                    Timber.d("switchBeaconsOn finished")
                    continuation.resume(true)
                }
            } else {
                val beacon = aircraft?.accessoryAggregation?.beacon
                Timber.d("switchBeaconsOn For [others]")
                beacon?.setEnabled(beaconsOn) { djiError ->
                    if (djiError != null) {
                        Timber.e("switchBeaconsOn #${djiError.errorCode}: ${djiError.description}")
                        continuation.resumeWithException(Exception(djiError.description))
                    }
                    Timber.d("switchBeaconsOn finished")
                    continuation.resume(true)
                }
            }
        }
    }
}