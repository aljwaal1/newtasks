package com.aljwaal.newtasks

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.google.android.gms.ads.MobileAds

private const val GOOGLE_TEST_ADAPTIVE_BANNER = "ca-app-pub-3940256099942544/9214589741"

/**
 * Bottom ad area used only for closed testing.
 *
 * It intentionally uses Google's demo ad unit so test traffic never reaches the publisher account.
 * AlarmActivity is excluded because ads must not compete with urgent alarm controls.
 */
@Composable
internal fun ClosedTestingAdFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        MobileAds.initialize(context.applicationContext) { }
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
                    "نسخة اختبار • إعلان تجريبي",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp
                )
                TextButton(
                    onClick = {
                        context.startActivity(Intent(context, PrivacyPolicyActivity::class.java))
                    }
                ) {
                    Text("سياسة الخصوصية", fontSize = 10.sp)
                }
            }
            TestAdaptiveBanner()
        }
    }
}

@Composable
private fun TestAdaptiveBanner() {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.coerceAtLeast(320)
    val adView = remember(screenWidthDp) {
        AdView(context).apply {
            adUnitId = GOOGLE_TEST_ADAPTIVE_BANNER
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
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
