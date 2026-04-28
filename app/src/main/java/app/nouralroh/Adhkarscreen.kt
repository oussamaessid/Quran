package app.nouralroh

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.compose.BackHandler


data class AdhkarSubCategory(
    val titleArabic : String,
    val titleFr     : String,
    val icon        : String,
    val items       : List<AdhkarItem>
)

data class AdhkarCategory(
    val titleArabic   : String,
    val titleFr       : String,
    val icon          : String,
    val accentColor   : Color,
    val items         : List<AdhkarItem>        = emptyList(),
    val subCategories : List<AdhkarSubCategory> = emptyList()
) {
    val hasSubCategories get() = subCategories.isNotEmpty()
}

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
            AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)", "الإخلاص", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي ٱلۡعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)", "الفلق", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)", "الناس", 3),
            AdhkarItem("أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير، رب أسألك خير ما في هذا اليوم وخير ما بعده، وأعوذ بك من شر ما في هذا اليوم وشر ما بعده، رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر", "مسلم", 1),
            AdhkarItem("اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور", "أبو داود", 1),
            AdhkarItem("اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي فاغفر لي فإنه لا يغفر الذنوب إلا أنت", "سيد الاستغفار – البخاري", 1),
            AdhkarItem("بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم", "أبو داود والترمذي", 3),
            AdhkarItem("سبحان الله وبحمده، عدد خلقه، ورضا نفسه، وزنة عرشه، ومداد كلماته", "مسلم", 3),
            AdhkarItem("رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً", "أبو داود والترمذي", 3),
            AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 10),
            AdhkarItem("أستغفر الله العظيم وأتوب إليه", "الترمذي", 100),
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار المساء",
        titleFr     = "Soir",
        icon        = "🌙",
        accentColor = QuranColors.GoldBright,
        items = listOf(
            AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
            AdhkarItem("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "مسلم", 1),
            AdhkarItem("اللَّهُمَّ بِكَ أَمْسَيْنَا وَبِكَ أَصْبَحْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ وَإِلَيْكَ الْمَصِير", "أبو داود", 1),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)", "الإخلاص", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي ٱلۡعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)", "الفلق", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)", "الناس", 3),
            AdhkarItem("أعوذ بكلمات الله التامات من شر ما خلق", "مسلم", 3),
            AdhkarItem("بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم", "أبو داود والترمذي", 3),
            AdhkarItem("رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً", "أبو داود والترمذي", 3),
            AdhkarItem("يا حي يا قيوم برحمتك أستغيث أصلح لي شأني كله ولا تكلني إلى نفسي طرفة عين", "الحاكم", 1),
            AdhkarItem("اللهم إني أسألك العفو والعافية في الدنيا والآخرة", "ابن ماجه", 1),
            AdhkarItem("سبحان الله وبحمده\nسبحان الله العظيم", "البخاري ومسلم", 100),
            AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 10),
            AdhkarItem("اللهم صل وسلم وبارك على نبينا محمد", "حديث", 10),
            AdhkarItem("أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه", "أبو داود والترمذي", 3),
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار النوم",
        titleFr     = "Sommeil",
        icon        = "🌟",
        accentColor = QuranColors.Gold,
        items = listOf(
            AdhkarItem("بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "البخاري", 1),
            AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
            AdhkarItem("آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ وَالْمُؤْمِنُونَ ۚ كُلٌّ آمَنَ بِاللَّهِ وَمَلَائِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ لَا نُفَرِّقُ بَيْنَ أَحَدٍ مِّن رُّسُلِهِ ۚ وَقَالُوا سَمِعْنَا وَأَطَعْنَا ۖ غُفْرَانَكَ رَبَّنَا وَإِلَيْكَ الْمَصِيرُ ۞ لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا ۚ لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ ۗ رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا ۚ رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِن قَبْلِنَا ۚ رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ ۖ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا ۚ أَنتَ مَوْلَانَا فَانصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ", "البقرة: ٢٨٥-٢٨٦", 1),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)", "الإخلاص", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي ٱلۡعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)", "الفلق", 3),
            AdhkarItem("بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)", "الناس", 3),
            AdhkarItem("بِاسْمِكَ رَبِّـي وَضَعْـتُ جَنْـبي، وَبِكَ أَرْفَعُـه، فَإِن أَمْسَـكْتَ نَفْسـي فارْحَـمْها، وَإِنْ أَرْسَلْتَـها فاحْفَظْـها بِمـا تَحْفَـظُ بِه عِبـادَكَ الصّـالِحـين", "البخاري ومسلم", 1),
            AdhkarItem("اللّهُـمَّ أَسْـلَمْتُ نَفْـسي إِلَـيْكَ، وَفَوَّضْـتُ أَمْـري إِلَـيْكَ، وَوَجَّـهْتُ وَجْـهي إِلَـيْكَ، وَأَلْـجَـاْتُ ظَهـري إِلَـيْكَ، رَغْبَـةً وَرَهْـبَةً إِلَـيْكَ، لا ملْجَـأَ وَلا مَنْـجـا مِنْـكَ إِلاّ إِلَـيْكَ، آمَنْـتُ بِكِتـابِكَ الّـذي أَنْزَلْـتَ وَبِنَبِـيِّـكَ الّـذي أَرْسَلْـت", "البخاري ومسلم", 1),
            AdhkarItem("اللّهُـمَّ قِنـي عَذابَـكَ يَـوْمَ تَبْـعَثُ عِبـادَك", "أبو داود والترمذي", 3),
            AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
            AdhkarItem("اللهم رب السماوات السبع وما أظلّت، ورب الأرضين وما أقلّت، ورب الشياطين وما أضلّت، كن لي جارًا من خلقك كُلِّهم جميعا أن يفرط علي أحد منهم أو أن يبغي علي، عز جارك، وجل ثناؤك ولا إله غيرك", "الترمذي", 1),
        )
    ),

    AdhkarCategory(
        titleArabic   = "أذكار الصلاة",
        titleFr       = "Après la prière",
        icon          = "🤲",
        accentColor   = QuranColors.GoldWarm,
        subCategories = listOf(

            AdhkarSubCategory(
                titleArabic = "أذكار عامة",
                titleFr     = "Toutes prières",
                icon        = "🕌",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير، اللهم لا مانع لما أعطيت، ولا معطي لما منعت، ولا ينفع ذا الجد منك الجد", "البخاري ومسلم", 1),
                    AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُو الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
                    AdhkarItem("سورة الإخلاص:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلْ هُوَ اللَّهُ أَحَدٌ (1) اللَّهُ الصَّمَدُ (2) لَمْ يَلِدْ وَلَمْ يُولَدْ (3) وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ (4)", "الإخلاص", 3),
                    AdhkarItem("سورة الفلق:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُلۡ أَعُوذُ بِرَبِّ ٱلۡفَلَق (1) مِن شَرِّ مَا خَلَقَ (2) وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ (3) وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِي الْعُقَدِ (4) وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ (5)", "الفلق", 3),
                    AdhkarItem("سورة الناس:\nبِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ. قُل أَعُوذُ بِرَبِّ ٱلنَّاسِ (1) مَلِكِ ٱلنَّاسِ (2) إِلَٰهِ ٱلنَّاسِ (3) مِن شَرِّ ٱلۡوَسۡوَاسِ ٱلۡخَنَّاسِ (4) ٱلَّذِي يُوَسۡوِسُ فِي صُدُورِ ٱلنَّاسِ (5) مِنَ ٱلۡجِنَّةِ وَٱلنَّاسِ (6)", "الناس", 3),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 1),
                    AdhkarItem("اللهم أعني على ذكرك وشكرك وحسن عبادتك", "أبو داود والنسائي", 1),
                )
            ),

            AdhkarSubCategory(
                titleArabic = "بعد الفجر",
                titleFr     = "Après Fajr",
                icon        = "🌅",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُو الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد يحيي ويميت وهو على كل شيء قدير", "الترمذي", 10),
                    AdhkarItem("اللهم إني أسألك علمًا نافعًا، ورزقًا طيبًا، وعملًا متقبلًا", "ابن ماجه – خاص بالفجر", 1),
                    AdhkarItem("اللهم إني أعوذ بك من الهم والحزن، وأعوذ بك من العجز والكسل، وأعوذ بك من الجبن والبخل، وأعوذ بك من غلبة الدين وقهر الرجال", "البخاري", 1),
                )
            ),

            AdhkarSubCategory(
                titleArabic = "بعد الظهر",
                titleFr     = "Après Dhuhr",
                icon        = "☀️",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 1),
                    AdhkarItem("اللهم اغفر لي ما قدمت وما أخرت، وما أسررت وما أعلنت، وما أسرفت، وما أنت أعلم به مني، أنت المقدم وأنت المؤخر، لا إله إلا أنت", "مسلم", 1),
                    AdhkarItem("اللهم صل وسلم وبارك على نبينا محمد", "حديث", 10),
                )
            ),

            AdhkarSubCategory(
                titleArabic = "بعد العصر",
                titleFr     = "Après Asr",
                icon        = "🌤️",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 1),
                    AdhkarItem("اللهم إني أعوذ بك من البخل، وأعوذ بك من الجبن، وأعوذ بك من أن أُرَدَّ إلى أرذل العمر، وأعوذ بك من فتنة الدنيا، وأعوذ بك من عذاب القبر", "البخاري", 1),
                )
            ),

            AdhkarSubCategory(
                titleArabic = "بعد المغرب",
                titleFr     = "Après Maghrib",
                icon        = "🌇",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُو الْعَلِيُّ الْعَظِيمُ", "البقرة: ٢٥٥", 1),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد يحيي ويميت وهو على كل شيء قدير", "الترمذي", 10),
                    AdhkarItem("اللهم أجرني من النار", "أبو داود – خاص بالمغرب والفجر", 7),
                )
            ),

            AdhkarSubCategory(
                titleArabic = "بعد العشاء",
                titleFr     = "Après Isha",
                icon        = "🌙",
                items = listOf(
                    AdhkarItem("أستغفر الله", "مسلم", 3),
                    AdhkarItem("اللهم أنت السلام ومنك السلام، تبارك يا ذا الجلال والإكرام", "مسلم", 1),
                    AdhkarItem("سبحان الله\nالحمد لله\nالله أكبر", "البخاري ومسلم", 33),
                    AdhkarItem("لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير", "البخاري ومسلم", 1),
                    AdhkarItem("اللهم إني أسألك الجنة، وأعوذ بك من النار", "أبو داود", 1),
                    AdhkarItem("اللهم صل وسلم وبارك على نبينا محمد", "حديث", 10),
                    AdhkarItem("اللهم اغفر لي ذنبي كله، دقه وجله، وأوله وآخره، وعلانيته وسره", "مسلم", 1),
                )
            ),
        )
    ),

    AdhkarCategory(
        titleArabic = "دعاء القنوت",
        titleFr     = "Dua Qunoot",
        icon        = "🕌",
        accentColor = Color(0xFF6BBFFF),
        items = listOf(
            AdhkarItem("اللَّهُمَّ اهْدِنِي فِيمَنْ هَدَيْتَ، وَعَافِنِي فِيمَنْ عَافَيْتَ، وَتَوَلَّنِي فِيمَنْ تَوَلَّيْتَ، وَبَارِكْ لِي فِيمَا أَعْطَيْتَ، وَقِنِي شَرَّ مَا قَضَيْتَ، فَإِنَّكَ تَقْضِي وَلَا يُقْضَى عَلَيْكَ، وَإِنَّهُ لَا يَذِلُّ مَنْ وَالَيْتَ، وَلَا يَعِزُّ مَنْ عَادَيْتَ، تَبَارَكْتَ رَبَّنَا وَتَعَالَيْتَ", "قنوت الوتر – أبو داود والترمذي", 1),
            AdhkarItem("اللَّهُمَّ إِنَّا نَسْتَعِينُكَ وَنَسْتَغْفِرُكَ، وَنُؤْمِنُ بِكَ وَنَتَوَكَّلُ عَلَيْكَ، وَنُثْنِي عَلَيْكَ الْخَيْرَ، وَنَشْكُرُكَ وَلَا نَكْفُرُكَ، وَنَخْلَعُ وَنَتْرُكُ مَنْ يَفْجُرُكَ", "قنوت الفجر – البيهقي", 1),
            AdhkarItem("اللَّهُمَّ إِيَّاكَ نَعْبُدُ، وَلَكَ نُصَلِّي وَنَسْجُدُ، وَإِلَيْكَ نَسْعَى وَنَحْفِدُ، نَرْجُو رَحْمَتَكَ وَنَخْشَى عَذَابَكَ، إِنَّ عَذَابَكَ بِالْكَافِرِينَ مُلْحِقٌ، اللَّهُمَّ إِنَّا نَسْتَعِينُكَ وَنَسْتَغْفِرُكَ وَنُثْنِي عَلَيْكَ وَلَا نَكْفُرُكَ", "قنوت الفجر – البيهقي", 1),
            AdhkarItem("اللَّهُمَّ عَذِّبِ الْكَفَرَةَ الَّذِينَ يَصُدُّونَ عَنْ سَبِيلِكَ، وَيُكَذِّبُونَ رُسُلَكَ، وَاجْعَلْ عَلَيْهِمْ رِجْزَكَ وَعَذَابَكَ، اللَّهُمَّ عَذِّبِ الْكَفَرَةَ أَهْلَ الْكِتَابِ", "قنوت النوازل – البخاري ومسلم", 1),
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار الوضوء",
        titleFr     = "Ablutions",
        icon        = "💧",
        accentColor = Color(0xFF4FC3F7),
        items = listOf(
            AdhkarItem("بِسْمِ اللَّهِ", "قبل الوضوء – أبو داود", 1),
            AdhkarItem("اللَّهُمَّ اغْفِرْ لِي ذَنْبِي وَوَسِّعْ لِي فِي دَارِي وَبَارِكْ لِي فِي رِزْقِي", "أثناء الوضوء", 1),
            AdhkarItem("أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ", "بعد الوضوء – مسلم", 1),
            AdhkarItem("اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ", "بعد الوضوء – الترمذي", 1),
            AdhkarItem("سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ", "بعد الوضوء – النسائي", 1),
        )
    ),

    AdhkarCategory(
        titleArabic   = "أذكار المسجد",
        titleFr       = "Mosquée",
        icon          = "⛪",
        accentColor   = Color(0xFF81C784),
        subCategories = listOf(
            AdhkarSubCategory(
                titleArabic = "دخول المسجد",
                titleFr     = "Entrée",
                icon        = "🕌",
                items = listOf(
                    AdhkarItem("أَعُوذُ بِاللَّهِ الْعَظِيمِ، وَبِوَجْهِهِ الْكَرِيمِ، وَسُلْطَانِهِ الْقَدِيمِ، مِنَ الشَّيْطَانِ الرَّجِيمِ", "أبو داود", 1),
                    AdhkarItem("بِسْمِ اللَّهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ اغْفِرْ لِي ذُنُوبِي وَافْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", "مسلم", 1),
                    AdhkarItem("اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ", "مسلم", 1),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "الخروج من المسجد",
                titleFr     = "Sortie",
                icon        = "🌙",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ، وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ اغْفِرْ لِي ذُنُوبِي وَافْتَحْ لِي أَبْوَابَ فَضْلِكَ", "مسلم", 1),
                    AdhkarItem("اللَّهُمَّ اعْصِمْنِي مِنَ الشَّيْطَانِ الرَّجِيمِ", "ابن ماجه", 1),
                )
            ),
        )
    ),

    AdhkarCategory(
        titleArabic   = "أذكار البيت",
        titleFr       = "Maison",
        icon          = "🏡",
        accentColor   = Color(0xFFA5D6A7),
        subCategories = listOf(
            AdhkarSubCategory(
                titleArabic = "دخول البيت",
                titleFr     = "Entrée",
                icon        = "🚪",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا", "أبو داود", 1),
                    AdhkarItem("اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ الْمَوْلِجِ وَخَيْرَ الْمَخْرَجِ، بِسْمِ اللَّهِ وَلَجْنَا وَبِسْمِ اللَّهِ خَرَجْنَا وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا", "أبو داود", 1),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "الخروج من البيت",
                titleFr     = "Sortie",
                icon        = "🌿",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "أبو داود والترمذي", 1),
                    AdhkarItem("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ", "أبو داود والترمذي", 1),
                )
            ),
        )
    ),

    AdhkarCategory(
        titleArabic   = "أذكار السفر",
        titleFr       = "Voyage",
        icon          = "✈️",
        accentColor   = Color(0xFF64B5F6),
        subCategories = listOf(
            AdhkarSubCategory(
                titleArabic = "عند الخروج للسفر",
                titleFr     = "Départ",
                icon        = "🚪",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "أبو داود والترمذي", 1),
                    AdhkarItem("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ أَنْ أَضِلَّ أَوْ أُضَلَّ، أَوْ أَزِلَّ أَوْ أُزَلَّ، أَوْ أَظْلِمَ أَوْ أُظْلَمَ، أَوْ أَجْهَلَ أَوْ يُجْهَلَ عَلَيَّ", "أبو داود والترمذي", 1),
                    AdhkarItem("اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ وَالْخَلِيفَةُ فِي الْأَهْلِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ وَعْثَاءِ السَّفَرِ وَكَآبَةِ الْمَنْظَرِ وَسُوءِ الْمُنْقَلَبِ فِي الْمَالِ وَالْأَهْلِ", "مسلم", 1),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "عند ركوب المركوب",
                titleFr     = "En montant",
                icon        = "🚗",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ", "أبو داود", 1),
                    AdhkarItem("سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ، اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ", "مسلم", 1),
                    AdhkarItem("اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا", "مسلم", 3),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "عند الرجوع من السفر",
                titleFr     = "Retour",
                icon        = "🏠",
                items = listOf(
                    AdhkarItem("آيِبُونَ، تَائِبُونَ، عَابِدُونَ، لِرَبِّنَا حَامِدُونَ", "البخاري ومسلم", 1),
                    AdhkarItem("اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَهَا وَخَيْرَ مَا فِيهَا وَخَيْرَ مَا جُمِعَتْ لَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّهَا وَشَرِّ مَا فِيهَا وَشَرِّ مَا جُمِعَتْ لَهُ", "البخاري ومسلم", 1),
                )
            ),
        )
    ),

    AdhkarCategory(
        titleArabic = "التَّشَهُّد",   // corrigé ici
        titleFr     = "Le Tashahhud",
        icon        = "🙏",
        accentColor = Color(0xFFFFCC80),
        items = listOf(
            AdhkarItem(
                "التَّحِيَّاتُ لِلَّهِ وَالصَّلَوَاتُ وَالطَّيِّبَاتُ، السَّلَامُ عَلَيْكَ أَيُّهَا النَّبِيُّ وَرَحْمَةُ اللَّهِ وَبَرَكَاتُهُ، السَّلَامُ عَلَيْنَا وَعَلَى عِبَادِ اللَّهِ الصَّالِحِينَ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
                "التشهد – البخاري ومسلم",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ، وَبَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
                "الصلاة الإبراهيمية – البخاري",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ وَمِنْ عَذَابِ النَّارِ وَمِنْ فِتْنَةِ الْمَحْيَا وَالْمَمَاتِ وَمِنْ شَرِّ فِتْنَةِ الْمَسِيحِ الدَّجَّالِ",
                "التشهد الأخير – مسلم",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ وَارْحَمْنِي إِنَّكَ أَنْتَ الْغَفُورُ الرَّحِيمُ",
                "التشهد الأخير – البخاري ومسلم",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
                "أبو داود – التشهد",
                1
            ),
            AdhkarItem(
                "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْجُبْنِ وَالْبُخْلِ وَأَعُوذُ بِكَ مِنْ أَنْ أُرَدَّ إِلَى أَرْذَلِ الْعُمُرِ وَأَعُوذُ بِكَ مِنْ فِتْنَةِ الدُّنْيَا وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ",
                "البخاري – التشهد",
                1
            ),
        )
    ),

    AdhkarCategory(
        titleArabic   = "أذكار الطعام",
        titleFr       = "Repas",
        icon          = "🍽️",
        accentColor   = Color(0xFFFFAB91),
        subCategories = listOf(
            AdhkarSubCategory(
                titleArabic = "قبل الطعام",
                titleFr     = "Avant le repas",
                icon        = "🤲",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ", "البخاري ومسلم", 1),
                    AdhkarItem("بِسْمِ اللَّهِ وَعَلَى بَرَكَةِ اللَّهِ", "أبو داود", 1),
                    AdhkarItem("اللَّهُمَّ بَارِكْ لَنَا فِيمَا رَزَقْتَنَا وَقِنَا عَذَابَ النَّارِ", "ابن السني", 1),
                    AdhkarItem("بِسْمِ اللَّهِ فِي أَوَّلِهِ وَآخِرِهِ", "عند النسيان في البداية – أبو داود", 1),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "بعد الطعام",
                titleFr     = "Après le repas",
                icon        = "✅",
                items = listOf(
                    AdhkarItem("الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ", "أبو داود والترمذي", 1),
                    AdhkarItem("الْحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ غَيْرَ مَكْفِيٍّ وَلَا مُوَدَّعٍ وَلَا مُسْتَغْنًى عَنْهُ رَبَّنَا", "البخاري", 1),
                    AdhkarItem("اللَّهُمَّ أَطْعِمْ مَنْ أَطْعَمَنِي وَاسْقِ مَنْ سَقَانِي", "مسلم – للضيف", 1),
                )
            ),
            AdhkarSubCategory(
                titleArabic = "عند الشرب",
                titleFr     = "Boisson",
                icon        = "💧",
                items = listOf(
                    AdhkarItem("بِسْمِ اللَّهِ", "أبو داود", 1),
                    AdhkarItem("الْحَمْدُ لِلَّهِ الَّذِي سَقَانَا عَذْبًا فُرَاتًا بِرَحْمَتِهِ وَلَمْ يَجْعَلْهُ مِلْحًا أُجَاجًا بِذُنُوبِنَا", "ابن السني", 1),
                )
            ),
        )
    ),

    AdhkarCategory(
        titleArabic = "أذكار متفرقة",
        titleFr     = "Divers",
        icon        = "✨",
        accentColor = Color(0xFFCE93D8),
        items = listOf(
            AdhkarItem("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "البخاري ومسلم", 1),
            AdhkarItem("حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ", "أبو داود", 7),
            AdhkarItem("اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ", "ابن ماجه", 1),
            AdhkarItem("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَأَعُوذُ بِكَ مِنَ الْعَجْزِ وَالْكَسَلِ، وَأَعُوذُ بِكَ مِنَ الْجُبْنِ وَالْبُخْلِ، وَأَعُوذُ بِكَ مِنْ غَلَبَةِ الدَّيْنِ وَقَهْرِ الرِّجَالِ", "البخاري", 1),
            AdhkarItem("اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ", "مسلم", 1),
            AdhkarItem("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "البخاري ومسلم", 100),
            AdhkarItem("لَا إِلَهَ إِلَّا اللَّهُ", "البخاري ومسلم", 100),
            AdhkarItem("اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا", "ابن حبان", 1),
            AdhkarItem("اللَّهُمَّ إِنِّي أَسْأَلُكَ الثَّبَاتَ فِي الْأَمْرِ وَالْعَزِيمَةَ عَلَى الرُّشْدِ وَأَسْأَلُكَ شُكْرَ نِعْمَتِكَ وَحُسْنَ عِبَادَتِكَ", "النسائي", 1),
            AdhkarItem("رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ", "أبو داود والترمذي", 100),
            AdhkarItem("اللَّهُمَّ مُصَرِّفَ الْقُلُوبِ صَرِّفْ قُلُوبَنَا عَلَى طَاعَتِكَ", "مسلم", 1),
            AdhkarItem("اللَّهُمَّ آتِ نَفْسِي تَقْوَاهَا وَزَكِّهَا أَنْتَ خَيْرُ مَنْ زَكَّاهَا أَنْتَ وَلِيُّهَا وَمَوْلَاهَا", "مسلم", 1),
        )
    ),

    AdhkarCategory(
        titleArabic = "أدعية قرآنية",
        titleFr     = "Duas coraniques",
        icon        = "📖",
        accentColor = QuranColors.GoldAccent,
        items = listOf(
            AdhkarItem("رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "البقرة: ٢٠١", 1),
            AdhkarItem("رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا", "البقرة: ٢٨٦", 1),
            AdhkarItem("رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي يَفْقَهُوا قَوْلِي", "طه: ٢٥-٢٨", 1),
            AdhkarItem("رَبِّ زِدْنِي عِلْمًا", "طه: ١١٤", 1),
            AdhkarItem("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "آل عمران: ١٧٣", 1),
            AdhkarItem("لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ", "الأنبياء: ٨٧", 1),
            AdhkarItem("رَبِّ إِنِّي لِمَا أَنزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ", "القصص: ٢٤", 1),
            AdhkarItem("رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا", "الفرقان: ٧٤", 1),
            AdhkarItem("رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَىٰ وَالِدَيَّ وَأَنْ أَعْمَلَ صَالِحًا تَرْضَاهُ وَأَصْلِحْ لِي فِي ذُرِّيَّتِي ۖ إِنِّي تُبْتُ إِلَيْكَ وَإِنِّي مِنَ الْمُسْلِمِينَ", "الأحقاف: ١٥", 1),
            AdhkarItem("رَبَّنَا اغْفِرْ لَنَا وَلِإِخْوَانِنَا الَّذِينَ سَبَقُونَا بِالْإِيمَانِ وَلَا تَجْعَلْ فِي قُلُوبِنَا غِلًّا لِّلَّذِينَ آمَنُوا رَبَّنَا إِنَّكَ رَءُوفٌ رَّحِيمٌ", "الحشر: ١٠", 1),
        )
    ),
)

@Composable
fun AdhkarScreen(onBack: () -> Unit) {

    var selectedCategory    by remember { mutableStateOf<AdhkarCategory?>(null) }
    var selectedSubCategory by remember { mutableStateOf<AdhkarSubCategory?>(null) }

    // ✅ FIX: Conserver les dernières valeurs non-null pendant les transitions
    var lastCategory    by remember { mutableStateOf<AdhkarCategory?>(null) }
    var lastSubCategory by remember { mutableStateOf<AdhkarSubCategory?>(null) }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != null) lastCategory = selectedCategory
    }
    LaunchedEffect(selectedSubCategory) {
        if (selectedSubCategory != null) lastSubCategory = selectedSubCategory
    }

    BackHandler(enabled = true) {
        when {
            selectedSubCategory != null -> selectedSubCategory = null
            selectedCategory    != null -> selectedCategory    = null
            else                        -> onBack()
        }
    }

    // ✅ shimX déclaré ici, accessible partout dans AdhkarScreen
    val inf = rememberInfiniteTransition(label = "adhkar")
    val shimX by inf.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "shim"
    )

    val screenState = when {
        selectedSubCategory != null -> "detail_sub"
        selectedCategory    != null -> if (selectedCategory!!.hasSubCategories) "subcategory" else "detail"
        else                        -> "list"
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            if (targetState != "list") {
                (slideInHorizontally { it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { -it } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally { -it } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(200)))
            }
        },
        label = "adhkar_nav"
    ) { state ->
        when (state) {
            "list" -> AdhkarListScreen(
                shimX = shimX,
                onBack = onBack,
                onCategoryClick = {
                    lastCategory = it
                    selectedCategory = it
                }
            )

            "subcategory" -> AdhkarSubCategoryScreen(
                shimX = shimX,
                category = lastCategory!!,
                onBack = { selectedCategory = null },
                onSubCategoryClick = {
                    lastSubCategory = it
                    selectedSubCategory = it
                }
            )

            "detail" -> AdhkarDetailScreen(
                titleArabic = lastCategory!!.titleArabic,
                titleFr     = lastCategory!!.titleFr,
                accentColor = lastCategory!!.accentColor,
                items       = lastCategory!!.items,
                onBack      = { selectedCategory = null }
            )

            "detail_sub" -> AdhkarDetailScreen(
                titleArabic = lastSubCategory!!.titleArabic,
                titleFr     = lastSubCategory!!.titleFr,
                accentColor = lastCategory!!.accentColor,
                items       = lastSubCategory!!.items,
                onBack      = { selectedSubCategory = null }
            )
        }
    }
}

@Composable
fun AdhkarListScreen(
    shimX           : Float,
    onBack          : () -> Unit,
    onCategoryClick : (AdhkarCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)))
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        AdhkarTopBar(title = "الأذكار والأدعية", subtitle = "Adhkar & Duas", onBack = onBack)

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
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
                AdhkarCategoryRow(category = category, shimX = shimX, onClick = { onCategoryClick(category) })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        AdhkarBottomSpacer()
    }
}

@Composable
fun AdhkarSubCategoryScreen(
    shimX              : Float,
    category           : AdhkarCategory,
    onBack             : () -> Unit,
    onSubCategoryClick : (AdhkarSubCategory) -> Unit
) {
    val inf = rememberInfiniteTransition(label = "sub")
    val glowAlpha by inf.animateFloat(
        0.2f, 0.6f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), "g"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)))
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        AdhkarTopBar(
            title       = category.titleArabic,
            subtitle    = category.titleFr,
            onBack      = onBack,
            accentColor = category.accentColor
        )

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, category.accentColor.copy(alpha = 0.3f)))))
                    Text(category.icon, fontSize = 18.sp)
                    Box(Modifier.weight(1f).height(0.5.dp).background(
                        Brush.horizontalGradient(listOf(category.accentColor.copy(alpha = 0.3f), Color.Transparent))))
                }
                Spacer(Modifier.height(4.dp))
            }

            itemsIndexed(category.subCategories) { _, sub ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
                        .border(1.dp,
                            Brush.verticalGradient(listOf(
                                category.accentColor.copy(alpha = glowAlpha),
                                category.accentColor.copy(alpha = 0.1f)
                            )),
                            RoundedCornerShape(18.dp))
                        .clickable { onSubCategoryClick(sub) }
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(1.5.dp).align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            .background(Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    category.accentColor.copy(alpha = if (shimX in 0f..1f) 0.6f else 0f),
                                    category.accentColor.copy(alpha = 0.3f * glowAlpha),
                                    Color.Transparent
                                ),
                                startX = shimX * 500f, endX = shimX * 500f + 500f
                            ))
                    )
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.radialGradient(listOf(category.accentColor.copy(alpha = 0.18f), Color.Transparent)))
                                .border(1.dp, category.accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sub.icon, fontSize = 20.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(sub.titleArabic, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                color = category.accentColor, style = TextStyle(textDirection = TextDirection.Rtl))
                            Text(sub.titleFr, fontSize = 9.sp, color = QuranColors.GoldDim,
                                letterSpacing = 0.8.sp, fontStyle = FontStyle.Italic)
                            Text("${sub.items.size} dhikr", fontSize = 8.sp,
                                color = QuranColors.GoldDim.copy(alpha = 0.6f))
                        }
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(category.accentColor.copy(alpha = 0.1f))
                                .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowForward, null,
                                tint = category.accentColor.copy(alpha = 0.7f), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        AdhkarBottomSpacer()
    }
}

@Composable
fun AdhkarDetailScreen(
    titleArabic : String,
    titleFr     : String,
    accentColor : Color,
    items       : List<AdhkarItem>,
    onBack      : () -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var repeatCount  by remember(currentIndex) { mutableStateOf(0) }

    val item       = items[currentIndex]
    val isLastItem = currentIndex == items.lastIndex
    val isDone     = repeatCount >= item.times

    val globalProgress = (currentIndex.toFloat() +
            (repeatCount.toFloat() / item.times.toFloat()).coerceIn(0f, 1f)) /
            items.size.toFloat()

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

    fun goNext() { if (currentIndex < items.lastIndex) currentIndex++ else onBack() }
    fun goPrev() { if (currentIndex > 0) currentIndex-- }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(QuranColors.AppBg, Color(0xFF1A0C00), QuranColors.AppBg)))
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        AdhkarTopBar(title = titleArabic, subtitle = titleFr, onBack = onBack, accentColor = accentColor)

        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxSize().background(QuranColors.PanelBorder.copy(alpha = 0.2f)))
                Box(
                    Modifier.fillMaxWidth(animGlobal).fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.6f), accentColor)))
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(
                    Modifier.clip(RoundedCornerShape(5.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(item.source, fontSize = 9.sp, color = accentColor.copy(alpha = 0.75f),
                        style = TextStyle(textDirection = TextDirection.Rtl))
                }
                Text("${currentIndex + 1} / ${items.size}", fontSize = 10.sp,
                    color = accentColor.copy(alpha = 0.55f), letterSpacing = 1.sp)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 18.dp, vertical = 4.dp)) {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                    .background(Brush.radialGradient(listOf(
                        accentColor.copy(alpha = 0.07f * glowAlpha), Color.Transparent
                    )))
            )

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1E1200), Color(0xFF0C0700))))
                    .border(1.dp,
                        Brush.verticalGradient(listOf(
                            accentColor.copy(alpha = glowAlpha * 0.7f),
                            accentColor.copy(alpha = 0.1f)
                        )),
                        RoundedCornerShape(26.dp))
                    .clickable { if (!isDone) repeatCount++ else goNext() }
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❖", fontSize = 13.sp, color = accentColor.copy(alpha = 0.35f))
                Spacer(Modifier.height(22.dp))

                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (fadeIn(tween(320)) + slideInVertically { it / 4 }) togetherWith
                                (fadeOut(tween(200)) + slideOutVertically { -it / 4 })
                    },
                    label = "arabic"
                ) { idx ->
                    Text(
                        items[idx].arabic,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isDone) accentColor else QuranColors.GoldBright,
                        style      = TextStyle(textDirection = TextDirection.Rtl),
                        textAlign  = TextAlign.Center,
                        lineHeight = 38.sp,
                        modifier   = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(28.dp))

                if (item.times > 1) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(88.dp),
                            color = accentColor.copy(alpha = 0.12f), strokeWidth = 6.dp)
                        CircularProgressIndicator(progress = { animCount }, modifier = Modifier.size(88.dp),
                            color = accentColor, strokeWidth = 6.dp)
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(30.dp))
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$repeatCount", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                Text("/ ${item.times}", fontSize = 9.sp, color = accentColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!isDone) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(accentColor.copy(alpha = pulse)))
                            Text("اضغط للتسبيح", fontSize = 10.sp, color = accentColor.copy(alpha = 0.5f),
                                style = TextStyle(textDirection = TextDirection.Rtl))
                        }
                    } else {
                        Text("اضغط للانتقال", fontSize = 10.sp, color = accentColor.copy(alpha = 0.6f),
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    }
                } else {
                    if (isDone) {
                        Box(
                            Modifier.clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("اضغط للانتقال", fontSize = 10.sp, color = accentColor.copy(alpha = 0.6f),
                            style = TextStyle(textDirection = TextDirection.Rtl))
                    } else {
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.08f))
                                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { repeatCount++ }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(accentColor.copy(alpha = pulse)))
                                Text("اضغط للإتمام", fontSize = 12.sp, color = accentColor.copy(alpha = 0.75f),
                                    style = TextStyle(textDirection = TextDirection.Rtl))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("❖", fontSize = 13.sp, color = accentColor.copy(alpha = 0.35f))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavButton(
                label       = "السابق",
                icon        = Icons.Default.KeyboardArrowLeft,
                iconOnLeft  = true,
                enabled     = currentIndex > 0,
                accentColor = accentColor,
                modifier    = Modifier.weight(1f),
                onClick     = { goPrev() }
            )
            NavButton(
                label       = if (isLastItem && isDone) "إنهاء" else "التالي",
                icon        = if (isLastItem && isDone) Icons.Default.Check else Icons.Default.KeyboardArrowRight,
                iconOnLeft  = false,
                enabled     = isDone,
                highlighted = isDone,
                accentColor = accentColor,
                modifier    = Modifier.weight(1f),
                onClick     = { goNext() }
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            items.forEachIndexed { idx, _ ->
                val isCurrent = idx == currentIndex
                val isPast    = idx < currentIndex
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .size(if (isCurrent) 9.dp else 5.dp)
                        .clip(CircleShape)
                        .background(when {
                            isCurrent -> accentColor
                            isPast    -> accentColor.copy(alpha = 0.45f)
                            else      -> QuranColors.PanelBorder.copy(alpha = 0.25f)
                        })
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        AdhkarBottomSpacer()
    }
}


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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1E1000), Color(0xFF0E0800))))
            .border(1.dp,
                Brush.verticalGradient(listOf(
                    category.accentColor.copy(alpha = glowAlpha),
                    category.accentColor.copy(alpha = 0.1f)
                )),
                RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Box(
            Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        category.accentColor.copy(alpha = if (shimX in 0f..1f) 0.7f else 0f),
                        category.accentColor.copy(alpha = 0.4f * glowAlpha),
                        Color.Transparent
                    ),
                    startX = shimX * 500f, endX = shimX * 500f + 500f
                ))
        )
        Row(
            Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.radialGradient(listOf(category.accentColor.copy(alpha = 0.2f), Color.Transparent)))
                    .border(1.dp,
                        Brush.linearGradient(listOf(
                            category.accentColor.copy(alpha = 0.5f),
                            category.accentColor.copy(alpha = 0.1f)
                        )),
                        RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 22.sp)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier.clip(RoundedCornerShape(5.dp))
                        .background(category.accentColor.copy(alpha = 0.12f))
                        .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(category.titleFr.uppercase(), fontSize = 7.sp,
                        color = category.accentColor.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                }
                Text(category.titleArabic, fontSize = 17.sp, color = category.accentColor,
                    fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                val countLabel = if (category.hasSubCategories)
                    "${category.subCategories.size} classes"
                else
                    "${category.items.size} dhikr"
                Text(countLabel, fontSize = 9.sp, color = QuranColors.GoldDim)
            }

            Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .background(category.accentColor.copy(alpha = 0.1f))
                    .border(0.5.dp, category.accentColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, null,
                    tint = category.accentColor.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

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
                    Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.22f), accentColor.copy(alpha = 0.08f)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFF1C1100), Color(0xFF0E0800)))
            )
            .border(0.5.dp,
                if (enabled) accentColor.copy(alpha = if (highlighted) 0.6f else 0.3f)
                else QuranColors.PanelBorder.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (iconOnLeft) Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = contentColor,
                style = TextStyle(textDirection = TextDirection.Rtl))
            if (!iconOnLeft) Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AdhkarTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    accentColor: Color = QuranColors.GoldBlaze
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A1A04), QuranColors.AppBg, Color(0xFF2A1A04))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = QuranColors.GoldDim)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, style = TextStyle(textDirection = TextDirection.Rtl))
                Text(subtitle, color = QuranColors.GoldDim, fontSize = 9.sp, letterSpacing = 1.5.sp, fontStyle = FontStyle.Italic)
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
fun AdhkarBottomSpacer() {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1A0C00))))
    ) {
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}