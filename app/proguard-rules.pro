-keep class com.example.autoclickhelper.** { *; }
-keepclassmembers class com.example.autoclickhelper.** { *; }

-keep public class android.accessibilityservice.AccessibilityService
-keep public class android.accessibilityservice.GestureDescription
-keep public class android.view.accessibility.AccessibilityNodeInfo

-dontwarn android.accessibilityservice.**
-dontwarn android.view.accessibility.**