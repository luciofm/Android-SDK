Android-SDK
===========

Sharethrough's SDK to publish and monetize Android apps

PROGUARD
===========

If you are using proguard, you will need to add the following lines

```
-keep public class com.sharethrough.sdk.** {
   public *;
}

#this is only required for DFP
-keep public class com.google.android.gms.ads.** {
   public *;
}

#this is only required for DFP
-keep public class com.google.ads.** {
   public *;
}

-dontwarn com.squareup.okhttp.**
```

##Documentation
Documentation for Sharethrough's Android SDK can be found at http://developers.sharethrough.com/android/
