package com.aljwaal.newtasks

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored adaptive banner footer used by normal app screens.
 *
 * Real revenue ads are enabled only when ADMOB_APP_ID and ADMOB_BANNER_ID are provided
 * at build time. Without them, Google's demo IDs are used automatically.
 * AlarmActivity is intentionally excluded from ads to keep urgent controls safe.
 */
@Composable
internal fun ClosedTestingAdFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(activity) {
        activity?.let { AdConsentGate.start(it) }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (BuildConfig.ADMOB_LIVE_ADS) "إعلان" else "إعلان تجريبي",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (AdConsentGate.privacyOptionsRequired && activity != null) {
                        TextButton(onClick = { AdConsentGate.showPrivacyOptions(activity) }) {
                            Text("خيارات الخصوصية", fontSize = 10.sp)
                        }
                    }
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(context, PrivacyPolicyActivity::class.java))
                        }
                    ) {
                        Text("سياسة الخصوصية", fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (AdConsentGate.canRequestAds) {
                AdaptiveBanner()
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun AdaptiveBanner() {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)
    val adView = remember(screenWidthDp, BuildConfig.ADMOB_BANNER_ID) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(
                AdSize.getLargeAnchoredAdaptiveBannerAdSize(
                    context,
                    screenWidthDp
                )
            )
            loadAd(AdRequest.Builder().build())
        }
    }

    AndroidView(
        factory = { adView },
        modifier = Modifier.fillMaxWidth()
    )

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
