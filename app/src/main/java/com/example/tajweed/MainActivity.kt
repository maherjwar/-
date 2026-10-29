package com.example.tajweed

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- البيانات ----------
data class Question(val prompt: String, val verse: String, val options: List<String>, val answer: Int, val explain: String)
data class Lesson(val title: String, val intro: String, val questions: List<Question>)

private val noon = listOf("إظهار", "إدغام", "إقلاب", "إخفاء")
private val madd = listOf("مد طبيعي", "مد متصل", "مد منفصل")
private val meem = listOf("إخفاء شفوي", "إدغام شفوي", "إظهار شفوي")

val lessons = listOf(
    Lesson("النون الساكنة والتنوين",
        "أربعة أحكام: إظهار (ء هـ ع ح غ خ)، إدغام (ي ر م ل و ن)، إقلاب (ب)، إخفاء (باقي الحروف).",
        listOf(
            Question("ما حكم النون الساكنة؟", "مَنْ آمَنَ", noon, 0, "بعدها همزة وهي من حروف الحلق."),
            Question("ما حكم النون الساكنة؟", "مِنْ بَعْدِ", noon, 2, "بعدها باء فتُقلب ميمًا مع غنة."),
            Question("ما حكم النون الساكنة؟", "مَنْ يَقُولُ", noon, 1, "بعدها ياء من حروف «يرملون»."),
            Question("ما حكم النون الساكنة؟", "أَنْتُمْ", noon, 3, "بعدها تاء من حروف الإخفاء.")
        )),
    Lesson("المدود",
        "الطبيعي حركتان. المتصل: حرف المد والهمزة في كلمة واحدة. المنفصل: المد في آخر كلمة والهمزة في أول التي تليها.",
        listOf(
            Question("ما نوع المد؟", "السَّمَاءِ", madd, 1, "الألف بعدها همزة في الكلمة نفسها."),
            Question("ما نوع المد؟", "بِمَا أُنْزِلَ", madd, 2, "الألف في آخر «ما» والهمزة في أول «أنزل»."),
            Question("ما نوع المد؟", "قَالَ", madd, 0, "لا همز بعد المد ولا سكون.")
        )),
    Lesson("الميم الساكنة",
        "إخفاء شفوي عند الباء، إدغام شفوي عند الميم، وإظهار شفوي عند باقي الحروف.",
        listOf(
            Question("ما حكم الميم الساكنة؟", "تَرْمِيهِمْ بِحِجَارَةٍ", meem, 0, "بعدها باء."),
            Question("ما حكم الميم الساكنة؟", "لَهُمْ مَا", meem, 1, "بعدها ميم."),
            Question("ما حكم الميم الساكنة؟", "هُمْ فِيهَا", meem, 2, "بعدها فاء.")
        ))
)

// ---------- حفظ التقدم ----------
class Store(c: Context) {
    private val p = c.getSharedPreferences("progress", 0)
    var xp by mutableIntStateOf(p.getInt("xp", 0)); private set
    var streak by mutableIntStateOf(p.getInt("streak", 0)); private set
    var done by mutableIntStateOf(p.getInt("done", 0)); private set

    fun finish(index: Int, gained: Int) {
        val today = System.currentTimeMillis() / 86_400_000L
        val last = p.getLong("last", 0)
        if (last != today) streak = if (last == today - 1) streak + 1 else 1
        xp += gained
        if (index == done) done++
        p.edit().putInt("xp", xp).putInt("streak", streak).putInt("done", done).putLong("last", today).apply()
    }
}

// ---------- الواجهة ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme { Surface { App(store) } }
            }
        }
    }
}

@Composable
fun App(st: Store) {
    var open by remember { mutableStateOf<Int?>(null) }
    val i = open
    if (i == null) Home(st) { open = it }
    else LessonScreen(lessons[i]) { gained ->
        if (gained != null) st.finish(i, gained)
        open = null
    }
}

@Composable
fun Home(st: Store, onOpen: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("🔥 ${st.streak}", fontSize = 20.sp)
            Text("⭐ ${st.xp}", fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(lessons) { i, l ->
                val locked = i > st.done
                Button(
                    onClick = { onOpen(i) }, enabled = !locked,
                    modifier = Modifier.fillMaxWidth(0.85f).height(72.dp)
                ) {
                    Text((if (i < st.done) "✅ " else if (locked) "🔒 " else "▶ ") + l.title, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun LessonScreen(l: Lesson, onExit: (Int?) -> Unit) {
    var step by remember { mutableIntStateOf(-1) }
    var hearts by remember { mutableIntStateOf(3) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var correct by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when {
            hearts == 0 -> {
                Text("نفدت القلوب 💔 أعد المحاولة", fontSize = 22.sp)
                Button({ onExit(null) }) { Text("رجوع") }
            }
            step == -1 -> {
                Text(l.title, fontSize = 26.sp)
                Text(l.intro, fontSize = 18.sp)
                Button({ step = 0 }, Modifier.fillMaxWidth()) { Text("ابدأ") }
            }
            step >= l.questions.size -> {
                Text("أحسنت! 🎉  +${correct * 10} نقطة", fontSize = 24.sp)
                Button({ onExit(correct * 10) }, Modifier.fillMaxWidth()) { Text("متابعة") }
            }
            else -> {
                val q = l.questions[step]
                LinearProgressIndicator(progress = { step / l.questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("❤️ $hearts")
                Text(q.prompt, fontSize = 18.sp)
                Text(q.verse, fontSize = 32.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                q.options.forEachIndexed { i, o ->
                    Button(
                        onClick = {
                            if (picked == null) {
                                picked = i
                                if (i == q.answer) correct++ else hearts--
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                picked == null -> MaterialTheme.colorScheme.primary
                                i == q.answer -> Color(0xFF58A700)
                                i == picked -> Color(0xFFEA2B2B)
                                else -> Color.Gray
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(o, fontSize = 18.sp) }
                }
                if (picked != null) {
                    Text(q.explain, fontSize = 16.sp)
                    Button({ picked = null; step++ }, Modifier.fillMaxWidth()) { Text("التالي") }
                }
            }
        }
    }
}
