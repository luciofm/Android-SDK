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
    public static String advertisingId;
    private static boolean hasRetrievedAdvertisingId = false;

    public AdvertisingIdProvider(final Context context) {
        if (hasRetrievedAdvertisingId) {
            return;
        }

        hasRetrievedAdvertisingId = true;
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
            STRExecutorService.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                        advertisingId = adInfo.getId();
                        if (adInfo.isLimitAdTrackingEnabled()) {
                            advertisingId = null;
                        }
                        Logger.d("Google Advertising ID: %s", advertisingId);
                    } catch (IOException e) {
                        Logger.d("Fail to retrieve Google Play Advertising Id", e);
                    } catch (GooglePlayServicesNotAvailableException e) {
                        Logger.d("Fail to retrieve Google Play Advertising Id", e);
                    } catch (GooglePlayServicesRepairableException e) {
                        Logger.d("Fail to retrieve Google Play Advertising Id", e);
                    }
                }
            });
        }
    }

    public static String getAdvertisingId() {
        return advertisingId;
    }
}
