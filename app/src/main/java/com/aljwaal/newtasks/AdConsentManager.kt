package com.aljwaal.newtasks

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google UMP consent helper.
 * Consent information is refreshed on every app launch before any live ad request.
 */
internal class AdConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onComplete()
                }
            },
            {
                // A previous valid consent decision can still permit ads when refresh fails.
                onComplete()
            }
        )
    }

    fun showPrivacyOptions(onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onDismissed()
        }
    }
}
