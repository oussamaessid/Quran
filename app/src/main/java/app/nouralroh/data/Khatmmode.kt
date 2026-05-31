package app.nouralroh.data

enum class KhatmMode(
    val labelAr   : String,
    val labelEn   : String,
    val totalUnits: Int,
    val maxPerDay : Int,
    val defaultPpd: Int          // suggested slider starting value
) {
    PAGE("صفحة",    "Pages", 604, 50,  20),
    HIZB("حزب",    "Hizb",   60,  4,   1),
    RUB ("ربع حزب","Rub",   240, 16,   4)
}