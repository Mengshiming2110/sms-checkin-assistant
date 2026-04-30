# SmsCheckIn ProGuard Rules

# Keep broadcast receivers (registered in AndroidManifest)
-keep class com.pengxh.smscheckin.SmsReceiver { *; }
-keep class com.pengxh.smscheckin.BootReceiver { *; }
-keep class com.pengxh.smscheckin.DailyReportReceiver { *; }
-keep class com.pengxh.smscheckin.CheckInWidget { *; }

# Keep services
-keep class com.pengxh.smscheckin.CheckInForegroundService { *; }
-keep class com.pengxh.smscheckin.NotificationMonitorService { *; }

# Keep the companion object in SmsReceiver (accessed by other components)
-keep class com.pengxh.smscheckin.SmsReceiver$Companion { *; }

# Keep data classes or classes used in SharedPreferences serialization
-keepclassmembers class com.pengxh.smscheckin.** { *; }
