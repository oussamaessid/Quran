package app.quran

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  DATA
// ─────────────────────────────────────────────────────────────────────────────

data class AdhkarCategory(
    val titleArabic : String,
    val titleFr     : String,
    val icon        : String,
    val accentColor : Color,
    val items       : List<AdhkarItem>
)

data class AdhkarItem(
    val arabic : String,
    val source : String,
    val times  : Int = 1
)

val ADHKAR_CATEGORIES = listOf(

    AdhkarCategory(
        titleArabic = "أذكار الصباح",
        titleFr     = "Matin",
        icon        = "🌄",
        accentColor = QuranColors.GoldBlaze,
        items = listOf(
            AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ\"ٌ",
                "البقرة: ٢٥٥", 1),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)", "الإخلاص", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1)مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي ٱلۡعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)", "الفلق", 3),
            AdhkarItem("أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)", "الناس", 3),
            AdhkarItem("أصبحنا وأصبح الملك لله: \"أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير، رب أسألك خير ما في هذا اليوم وخير ما بعده، وأعوذ بك من شر ما في هذا اليوم وشر ما بعده، رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر", "أصبحنا وأصبح الملك لله", 1),
            AdhkarItem("اللهم بك أصبحنا، وبك أمسينا، وبك  نحيا، وبك نموت، وإليك النشور", "اللهم بك أصبحنا", 1),
            AdhkarItem("اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي فاغفر لي فإنه لا يغفر الذنوب إلا أنت", "سيد الاستغفار", 1),
            AdhkarItem("بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم", "التحصين", 3),
            AdhkarItem("سبحان الله وبحمده، عدد خلقه، ورضا نفسه، وزنة عرشه، ومداد كلماته", "سبحان الله وبحمده", 3),
            AdhkarItem("ررضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً", "الرضا بالله", 3),
            AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "التهليل والتسبيح", 10),
            AdhkarItem("أستغفر الله العظيم وأتوب إليه", "الاستغفار", 100),
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار المساء",
        titleFr     = "Soir",
        icon        = "🌙",
        accentColor = QuranColors.GoldBright,
        items = listOf(
            AdhkarItem(
                "آيَةُ الْكُرْسِيِّ\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
                "البقرة: ٢٥٥",
                1
            ),
            AdhkarItem(
                "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
                "حديث",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ بِكَ أَمْسَيْنَا وَبِكَ أَصْبَحْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِير",
                "حديث",
                1
            ),
            AdhkarItem(
                "قُلْ هُوَ اللَّهُ أَحَدٌ\nاللَّهُ الصَّمَدُ\nلَمْ يَلِدْ وَلَمْ يُولَدْ\nوَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
                "الإخلاص",
                3
            ),
            AdhkarItem(
                "قُلْ أَعُوذُ بِرَبِّ الْفَلَق\nمِن شَرِّ مَا خَلَقَ\nوَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ\nوَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ\nوَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
                "الفلق",
                3
            ),
            AdhkarItem(
                "قُلْ أَعُوذُ بِرَبِّ النَّاسِ\nمَلِكِ النَّاسِ\nإِلَٰهِ النَّاسِ\nمِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ\nالَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ\nمِنَ الْجِنَّةِ وَالنَّاسِ",
                "الناس",
                3
            ),
            AdhkarItem(
                "أعوذ بكلمات الله التامات من شر ما خلق",
                "حديث",
                3
            ),
            AdhkarItem(
                "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم",
                "حديث",
                3
            ),
            AdhkarItem(
                "رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً",
                "حديث",
                3
            ),
            AdhkarItem(
                "يا حي يا قيوم برحمتك أستغيث أصلح لي شأني كله ولا تكلني إلى نفسي طرفة عين",
                "حديث",
                1
            ),
            AdhkarItem(
                "اللهم إني أسألك العفو والعافية في الدنيا والآخرة",
                "حديث",
                1
            ),
            AdhkarItem(
                "سبحان الله وبحمده",
                "حديث",
                100
            ),
            AdhkarItem(
                "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
                "حديث",
                10
            ),
            AdhkarItem(
                "اللهم صل وسلم وبارك على نبينا محمد",
                "حديث",
                10
            ),
            AdhkarItem(
                "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه",
                "حديث",
                3
            )
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار النوم",
        titleFr     = "Sommeil",
        icon        = "🌟",
        accentColor = QuranColors.Gold,
        items = listOf(
            // Adhkar principaux
            AdhkarItem(
                "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
                "حديث",
                1
            ),
            AdhkarItem(
                "آيَةُ الْكُرْسِيِّ\nاللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُو الْعَلِيُّ الْعَظِيمُ",
                "البقرة: ٢٥٥",
                1
            ),
            AdhkarItem(
                "آخر آيتين من سورة البقرة:\nءَامَنَ ٱلرَّسُولُ بِمَآ أُنزِلَ ... أَنْصُرْنَا عَلَى ٱلۡقَوۡمِ ٱلۡكَٰفِرِينَ",
                "البقرة",
                1
            ),
            // Sourates courtes avant le sommeil
            AdhkarItem(
                "سورة الإخلاص:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)",
                "الإخلاص",
                3
            ),
            AdhkarItem(
                "سورة الفلق:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي ٱلۡعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)",
                "الفلق",
                3
            ),
            AdhkarItem(
                "سورة الناس:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)",
                "الناس",
                3
            ),
            // Adhkar avec répétition
            AdhkarItem(
                "جمع الكفين والنفث فيهما: وقراءة (قل هو الله أحد)، (قل أعوذ برب الفلق)، (قل أعوذ برب الناس)، ثم مسح ما استطاع من الجسد، يبدأ بالرأس والوجه",
                "حديث",
                3
            ),
            AdhkarItem(
                "باسمك ربي وضعت جنبي: بِاسْمِكَ رَبِّـي وَضَعْـتُ جَنْـبي ... حفظها بما تحفظ به عبادك الصالحين",
                "حديث",
                1
            ),
            AdhkarItem(
                "اللهم أسلمت نفسي إليك: اللّهُـمَّ أَسْـلَمْتُ نَفْـسي إِلَـيْكَ ... آمَنْـتُ بِكِتـابِكَ الّـذي أَنْزَلْـتَ وَبِنَبِـيِّكَ الّـذي أَرْسَلْـت",
                "حديث",
                1
            ),
            AdhkarItem(
                "اللّهُـمَّ قِنـي عَذابَـكَ يَـوْمَ تَبْـعَثُ عِبـادَك",
                "حديث",
                3
            ),
            AdhkarItem(
                "باسمك اللهم أموت وأحيا",
                "حديث",
                1
            ),
            AdhkarItem(
                "سبحان الله",
                "حديث",
                33
            ),
            AdhkarItem(
                "الحمد لله",
                "حديث",
                33
            ),
            AdhkarItem(
                "الله أكبر",
                "حديث",
                34
            ),
            AdhkarItem(
                "دعاء آخر النوم: اللهم رب السماوات السبع وما أظلّت ... ولا إله غيرك",
                "حديث",
                1
            )
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار الصلاة",
        titleFr     = "Après la prière",
        icon        = "🤲",
        accentColor = QuranColors.GoldWarm,
        items = listOf(
            // الاستغفار والتهليل
            AdhkarItem(
                "أستغفر الله",
                "حديث",
                3
            ),
            AdhkarItem(
                "اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام",
                "حديث",
                1
            ),
            // التهليل المخصوص
            AdhkarItem(
                "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير، اللهم لا مانع لما أعطيت، ولا معطي لما منعت، ولا ينفع ذا الجد منك الجد",
                "حديث",
                1
            ),
            // آية الكرسي
            AdhkarItem(
                "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُو الْعَلِيُّ الْعَظِيمُ",
                "البقرة: ٢٥٥",
                1
            ),
            // المعوذات
            AdhkarItem(
                "سورة الإخلاص:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)",
                "الإخلاص",
                3
            ),
            AdhkarItem(
                "سورة الفلق:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي الْعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)",
                "الفلق",
                3
            ),
            AdhkarItem(
                "سورة الناس:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُل أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)",
                "الناس",
                3
            ),
            AdhkarItem(
                "سبحان الله",
                "حديث",
                33
            ),
            AdhkarItem(
                "الحمد لله",
                "حديث",
                33
            ),
            AdhkarItem(
                "الله أكبر",
                "حديث",
                33
            ),
            AdhkarItem(
                "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
                "حديث",
                1
            )
        )
    ),

    AdhkarCategory(
        titleArabic = "أدعية قرآنية",
        titleFr     = "Duas coraniques",
        icon        = "📖",
        accentColor = QuranColors.GoldAccent,
        items = listOf(
            AdhkarItem("رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
                "البقرة: ٢٠١", 1),
            AdhkarItem("رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا", "البقرة: ٢٨٦", 1),
            AdhkarItem("رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي", "طه: ٢٥-٢٦", 1),
            AdhkarItem("رَبِّ زِدْنِي عِلْمًا", "طه: ١١٤", 1),
            AdhkarItem("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "آل عمران: ١٧٣", 1),
            AdhkarItem("لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
                "الأنبياء: ٨٧", 1),
            AdhkarItem("رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ", "القصص: ٢٤", 1),
        )
    ),
)


@Composable
fun AdhkarScreen(onBack: () -> Unit) {

    var selectedCategory by remember { mutableStateOf<AdhkarCategory?>(null) }

    val inf   = rememberInfiniteTransition(label = "adhkar")
    val shimX by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        "shim"
    )

    AnimatedContent(
        targetState = selectedCategory,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { -it } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally { -it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(200)))
            }
        },
        label = "adhkar_nav"
    ) { category ->
        if (category == null) {
            AdhkarListScreen(
                shimX           = shimX,
                onBack          = onBack,
                onCategoryClick = { selectedCategory = it }
            )
        } else {
            AdhkarDetailScreen(
                category = category,
                onBack   = { selectedCategory = null }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ÉCRAN 1 — Liste des catégories
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdhkarListScreen(
    shimX           : Float,
    onBack          : () -> Unit,
    onCategoryClick : (AdhkarCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)
                )
            )
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        AdhkarTopBar(title = "الأذكار والأدعية", subtitle = "Adhkar & Duas", onBack = onBack)

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Bismillah
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, QuranColors.GoldDim.copy(alpha = 0.4f)))))
                    Text("﷽", fontSize = 20.sp, color = QuranColors.GoldDim)
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(QuranColors.GoldDim.copy(alpha = 0.4f), Color.Transparent))))
                }
                Spacer(Modifier.height(4.dp))
            }

            itemsIndexed(ADHKAR_CATEGORIES) { _, category ->
                AdhkarCategoryRow(
                    category = category,
                    shimX    = shimX,
                    onClick  = { onCategoryClick(category) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        AdhkarBottomSpacer()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ÉCRAN 2 — Lecture des adhkar un par un
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdhkarDetailScreen(
    category : AdhkarCategory,
    onBack   : () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    // Reset le compteur à chaque changement d'index
    var repeatCount  by remember(currentIndex) { mutableStateOf(0) }

    val item       = category.items[currentIndex]
    val isLastItem = currentIndex == category.items.lastIndex
    val isDone     = repeatCount >= item.times

    // Progression globale : items complétés + fraction de l'item courant
    val globalProgress = (currentIndex.toFloat() +
            (repeatCount.toFloat() / item.times.toFloat()).coerceIn(0f, 1f)) /
            category.items.size.toFloat()

    val animGlobal by animateFloatAsState(globalProgress.coerceIn(0f, 1f), tween(400), label = "gp")
    val animCount  by animateFloatAsState(
        (repeatCount.toFloat() / item.times.toFloat()).coerceIn(0f, 1f), tween(250), label = "cnt"
    )

    val inf = rememberInfiniteTransition(label = "detail")
    val glowAlpha by inf.animateFloat(
        0.3f, 0.8f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), "g"
    )
    val pulse by inf.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse), "p"
    )

    fun goNext() {
        if (currentIndex < category.items.lastIndex) currentIndex++
        else onBack()
    }

    fun goPrev() {
        if (currentIndex > 0) currentIndex--
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)
                )
            )
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Top bar avec nom de la catégorie
        AdhkarTopBar(
            title       = category.titleArabic,
            subtitle    = category.titleFr,
            onBack      = onBack,
            accentColor = category.accentColor
        )

        // ── Barre de progression globale ──────────────────────────────────────
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))) {
                // Track
                Box(Modifier.fillMaxSize().background(QuranColors.PanelBorder.copy(alpha = 0.2f)))
                // Fill
                Box(
                    Modifier
                        .fillMaxWidth(animGlobal)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(category.accentColor.copy(alpha = 0.6f), category.accentColor)
                            )
                        )
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(category.accentColor.copy(alpha = 0.08f))
                        .border(0.5.dp, category.accentColor.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        item.source, fontSize = 9.sp,
                        color = category.accentColor.copy(alpha = 0.75f),
                        style = TextStyle(textDirection = TextDirection.Rtl)
                    )
                }
                // X / total
                Text(
                    "${currentIndex + 1} / ${category.items.size}",
                    fontSize = 10.sp,
                    color    = category.accentColor.copy(alpha = 0.55f),
                    letterSpacing = 1.sp
                )
            }
        }

        // ── Carte de l'adhkar (cliquable pour incrémenter) ────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 4.dp)
        ) {
            // Glow derrière la carte
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                category.accentColor.copy(alpha = 0.07f * glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E1200), Color(0xFF0C0700))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                category.accentColor.copy(alpha = glowAlpha * 0.7f),
                                category.accentColor.copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
                    .clickable {
                        // Tap = compter une répétition (si pas fini)
                        if (!isDone) repeatCount++
                        else goNext()
                    }
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                // Ornement
                Text("❖", fontSize = 13.sp, color = category.accentColor.copy(alpha = 0.35f))
                Spacer(Modifier.height(22.dp))

                // Texte arabe animé au changement d'adhkar
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (fadeIn(tween(320)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(200)) + slideOutVertically { -it / 4 })
                    },
                    label = "arabic"
                ) { idx ->
                    Text(
                        category.items[idx].arabic,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isDone) category.accentColor else QuranColors.GoldBright,
                        style      = TextStyle(textDirection = TextDirection.Rtl),
                        textAlign  = TextAlign.Center,
                        lineHeight = 38.sp,
                        modifier   = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── Compteur / badge done ──────────────────────────────────────
                if (item.times > 1) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress    = { 1f },
                            modifier    = Modifier.size(88.dp),
                            color       = category.accentColor.copy(alpha = 0.12f),
                            strokeWidth = 6.dp,
                        )
                        CircularProgressIndicator(
                            progress    = { animCount },
                            modifier    = Modifier.size(88.dp),
                            color       = category.accentColor,
                            strokeWidth = 6.dp,
                        )
                        if (isDone) {
                            Icon(
                                Icons.Default.Check, null,
                                tint     = category.accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$repeatCount",
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = category.accentColor
                                )
                                Text(
                                    "/ ${item.times}",
                                    fontSize = 9.sp,
                                    color    = category.accentColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    // Hint
                    if (!isDone) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(category.accentColor.copy(alpha = pulse))
                            )
                            Text(
                                "اضغط للتسبيح",
                                fontSize = 10.sp,
                                color    = category.accentColor.copy(alpha = 0.5f),
                                style    = TextStyle(textDirection = TextDirection.Rtl)
                            )
                        }
                    } else {
                        Text(
                            "اضغط للانتقال",
                            fontSize = 10.sp,
                            color    = category.accentColor.copy(alpha = 0.6f),
                            style    = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    }
                } else {
                    // Item × 1
                    if (isDone) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(category.accentColor.copy(alpha = 0.15f))
                                .border(1.dp, category.accentColor.copy(alpha = 0.55f), CircleShape)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                tint     = category.accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "اضغط للانتقال",
                            fontSize = 10.sp,
                            color    = category.accentColor.copy(alpha = 0.6f),
                            style    = TextStyle(textDirection = TextDirection.Rtl)
                        )
                    } else {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(category.accentColor.copy(alpha = 0.08f))
                                .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { repeatCount++ }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    Modifier.size(6.dp).clip(CircleShape)
                                        .background(category.accentColor.copy(alpha = pulse))
                                )
                                Text(
                                    "اضغط للإتمام",
                                    fontSize = 12.sp,
                                    color    = category.accentColor.copy(alpha = 0.75f),
                                    style    = TextStyle(textDirection = TextDirection.Rtl)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("❖", fontSize = 13.sp, color = category.accentColor.copy(alpha = 0.35f))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Boutons navigation Précédent / Suivant ────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Précédent
            NavButton(
                label       = "السابق",
                icon        = Icons.Default.KeyboardArrowRight,
                iconOnLeft  = true,
                enabled     = currentIndex > 0,
                accentColor = category.accentColor,
                modifier    = Modifier.weight(1f),
                onClick     = { goPrev() }
            )

            // Suivant / Terminer
            NavButton(
                label       = if (isLastItem && isDone) "إنهاء" else "التالي",
                icon        = if (isLastItem && isDone) Icons.Default.Check else Icons.Default.KeyboardArrowLeft,
                iconOnLeft  = false,
                enabled     = isDone,
                highlighted = isDone,
                accentColor = category.accentColor,
                modifier    = Modifier.weight(1f),
                onClick     = { goNext() }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Points d'avancement ───────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            category.items.forEachIndexed { idx, _ ->
                val isCurrent = idx == currentIndex
                val isPast    = idx < currentIndex
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isCurrent) 9.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> category.accentColor
                                isPast    -> category.accentColor.copy(alpha = 0.45f)
                                else      -> QuranColors.PanelBorder.copy(alpha = 0.25f)
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        AdhkarBottomSpacer()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CATEGORY ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AdhkarCategoryRow(
    category : AdhkarCategory,
    shimX    : Float,
    onClick  : () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "row")
    val glowAlpha by inf.animateFloat(
        0.2f, 0.55f,
        infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse), "g"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        category.accentColor.copy(alpha = glowAlpha),
                        category.accentColor.copy(alpha = 0.1f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        // Shimmer top strip
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            category.accentColor.copy(alpha = if (shimX in 0f..1f) 0.7f else 0f),
                            category.accentColor.copy(alpha = 0.4f * glowAlpha),
                            Color.Transparent
                        ),
                        startX = shimX * 500f,
                        endX   = shimX * 500f + 500f
                    )
                )
        )

        Row(
            Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icône
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(category.accentColor.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                category.accentColor.copy(alpha = 0.5f),
                                category.accentColor.copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 22.sp)
            }

            // Textes
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(category.accentColor.copy(alpha = 0.12f))
                        .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        category.titleFr.uppercase(),
                        fontSize      = 7.sp,
                        color         = category.accentColor.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp,
                        fontWeight    = FontWeight.Bold
                    )
                }
                Text(
                    category.titleArabic,
                    fontSize   = 17.sp,
                    color      = category.accentColor,
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text("${category.items.size} dhikr", fontSize = 9.sp, color = QuranColors.GoldDim)
            }

            // Flèche
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(category.accentColor.copy(alpha = 0.1f))
                    .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward, null,
                    tint     = category.accentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPOSANTS UTILITAIRES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NavButton(
    label       : String,
    icon        : androidx.compose.ui.graphics.vector.ImageVector,
    iconOnLeft  : Boolean,
    enabled     : Boolean,
    highlighted : Boolean = false,
    accentColor : Color,
    modifier    : Modifier = Modifier,
    onClick     : () -> Unit
) {
    val contentColor = if (enabled) accentColor else QuranColors.GoldDim.copy(alpha = 0.25f)
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (highlighted && enabled)
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.08f))
                    )
                else Brush.horizontalGradient(
                    listOf(Color(0xFF1C1100), Color(0xFF0E0800))
                )
            )
            .border(
                0.5.dp,
                if (enabled) accentColor.copy(alpha = if (highlighted) 0.6f else 0.3f)
                else QuranColors.PanelBorder.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (iconOnLeft) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = contentColor,
                style      = TextStyle(textDirection = TextDirection.Rtl)
            )
            if (!iconOnLeft) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AdhkarTopBar(
    title       : String,
    subtitle    : String,
    onBack      : () -> Unit,
    accentColor : Color = QuranColors.GoldBlaze
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2A1A04), QuranColors.AppBg, Color(0xFF2A1A04))
                )
            )
    ) {
        Box(
            Modifier.fillMaxWidth().height(0.5.dp).align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.6f),
                            accentColor.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = QuranColors.GoldDim)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    color      = accentColor,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    style      = TextStyle(textDirection = TextDirection.Rtl)
                )
                Text(
                    subtitle,
                    color         = QuranColors.GoldDim,
                    fontSize      = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontStyle     = FontStyle.Italic
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
fun AdhkarBottomSpacer() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1A0C00))))
    ) {
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}