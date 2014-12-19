package com.sharethrough.sdk;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.concurrent.ExecutorService;

public class AdvertisingIdProvider {
    private String advertisingId;

    public AdvertisingIdProvider(final Context context, ExecutorService executorService, final String defaultRandomAdvertisingId) {
        advertisingId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (googlePlayServicesAvailable != ConnectionResult.SUCCESS) {
//            GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, (Activity) context, 0).show();
        } else {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                        advertisingId = adInfo.getId();
                        if (adInfo.isLimitAdTrackingEnabled()) {
                            advertisingId = defaultRandomAdvertisingId;
                        }
                    } catch (Exception e) {
                        Log.d("Sharethrough", "Google Play Advertising Id failure", e);
                    }
                }
            });
        }
        Log.i("Sharethrough", "advertising ID: " + advertisingId);
    }

    public String getAdvertisingId() {
        return advertisingId;
    }
}
