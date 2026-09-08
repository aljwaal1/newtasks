package com.aljwaal.newtasks

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Process-level consent gate for Google Mobile Ads.
 *
 * The first regular app screen starts the UMP refresh. Ads are requested only when
 * ConsentInformation.canRequestAds() returns true. AlarmActivity never uses this gate.
 */
internal object AdConsentGate {
    var canRequestAds by mutableStateOf(false)
        private set

    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private var started = false
    private var adsInitialized = false

    fun start(activity: Activity) {
        if (started) return
        started = true

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        fun refreshState() {
            canRequestAds = consentInformation.canRequestAds()
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

            if (canRequestAds && !adsInitialized) {
                adsInitialized = true
                MobileAds.initialize(activity.applicationContext) { }
            }
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // A valid decision from a previous session can already permit requests.
                refreshState()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refreshState()
                }
            },
            {
                // If refresh fails, UMP may still have a valid prior consent decision.
                refreshState()
            }
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            canRequestAds = consentInformation.canRequestAds()
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }
    }
}
