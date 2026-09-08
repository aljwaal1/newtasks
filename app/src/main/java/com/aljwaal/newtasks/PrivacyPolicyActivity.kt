package com.aljwaal.newtasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            "سياسة الخصوصية",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "منبه المهام الذكي • آخر تحديث: 8 سبتمبر 2026",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(Modifier.height(18.dp))
                        PolicySection(
                            "بيانات المهام",
                            "تُحفظ المهام والملاحظات والتصنيفات والأولويات والنسخ الاحتياطية محليًا على جهاز المستخدم. لا ينشئ التطبيق حسابًا للمستخدم ولا يرسل محتوى المهام إلى خادم خاص بالمطور."
                        )
                        PolicySection(
                            "الإعلانات في النسخة التجريبية",
                            "تستخدم نسخة الاختبار المغلق Google Mobile Ads لعرض إعلانات تجريبية فقط. قد تعالج خدمات Google معلومات تقنية مرتبطة بطلب الإعلان، مثل عنوان IP، معلومات الجهاز، تفاعلات الإعلان، ومعلومات تشخيصية، وفق سياسات Google. لا تُستخدم بيانات المهام نفسها لتخصيص الإعلانات."
                        )
                        PolicySection(
                            "الصلاحيات",
                            "يستخدم التطبيق صلاحيات الإشعارات والمنبه الدقيق والاهتزاز والعمل بعد إعادة التشغيل والشاشة المنبثقة عند الحاجة لتنفيذ التذكيرات. كما يستخدم اتصال الشبكة فقط لتحميل محتوى الإعلان التجريبي."
                        )
                        PolicySection(
                            "النسخ الاحتياطي",
                            "يمكن للمستخدم إنشاء نسخة احتياطية محلية أو تصدير ملف JSON ومشاركته يدويًا. لا يرفع التطبيق النسخة الاحتياطية تلقائيًا إلى أي خدمة سحابية."
                        )
                        PolicySection(
                            "التواصل",
                            "للاستفسار المتعلق بالخصوصية أو التطبيق: fastunllocked2017@gmail.com"
                        )
                        PolicySection(
                            "التغييرات",
                            "قد يتم تحديث هذه السياسة عند تغيير وظائف التطبيق أو خدمات الإعلانات. سيُحدّث تاريخ السياسة عند حدوث تغيير جوهري."
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PolicySection(title: String, body: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth(),
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = Color(0xFF1E293B)
    )
    Spacer(Modifier.height(5.dp))
    Text(
        body,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Color(0xFF334155)
    )
    Spacer(Modifier.height(16.dp))
}
