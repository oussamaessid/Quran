# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.batoulapps.adhan.** { *; }

# Gson parses PageJson/SurahJson/AyahJson/WordJson by reflection on field names
# (no @SerializedName). Without this, R8 renames the fields in release builds and
# Gson silently returns null/empty data for every Quran page — works in debug,
# breaks only in the minified Play Store build.
-keep class app.nouralroh.data.PageJson { *; }
-keep class app.nouralroh.data.SurahJson { *; }
-keep class app.nouralroh.data.AyahJson { *; }
-keep class app.nouralroh.data.WordJson { *; }