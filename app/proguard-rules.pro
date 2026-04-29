# LeanCloud
-keep class cn.leancloud.** { *; }
-keep class io.reactivex.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
