package com.sharethrough.sdk;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class AdvertisingIdProvider {
    private String advertisingId;

    public AdvertisingIdProvider(final Context context) {
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (googlePlayServicesAvailable != ConnectionResult.SUCCESS) {
//            GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, (Activity) context, 0).show();
        } else {
            STRExecutorService.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AdvertisingIdClient.Info adInfo = getAdvertisingInfo(context);
                        advertisingId = adInfo.getId();
                        if (adInfo.isLimitAdTrackingEnabled()) {
                            advertisingId = null;
                        }
                    } catch (Exception e) {
                        Log.d("Sharethrough", "Google Play Advertising Id failure", e);
                    }
                }
            });
        }
        Log.i("Sharethrough", "advertising ID: " + advertisingId);
    }

    protected AdvertisingIdClient.Info getAdvertisingInfo(Context context){
        try {
            return AdvertisingIdClient.getAdvertisingIdInfo(context);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getAdvertisingId() {
        return advertisingId;
    }
}
