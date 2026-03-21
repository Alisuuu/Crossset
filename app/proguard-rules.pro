# Preserve Shizuku for reflection access in SettingsManager
-keep class rikka.shizuku.Shizuku { *; }

# Preserve Enums used with valueOf
-keepclassmembers enum com.alisu.crosssset.SettingsTable { *; }
-keepclassmembers enum com.alisu.crosssset.RiskLevel { *; }

# Preserve HistoryItem for JSON serialization/deserialization
-keep class com.alisu.crosssset.HistoryItem { *; }
-keep class com.alisu.crosssset.SettingsItem { *; }

# General Android support for R8
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable