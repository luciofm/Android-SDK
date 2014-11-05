package com.sharethrough.sdk;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.bytecode.RobolectricInternals;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, shadows = {
        AdvertisingIdProviderTest.MySettingsSecureShadow.class,
        AdvertisingIdProviderTest.MyAdvertisingIdClientShadow.class,
        AdvertisingIdProviderTest.MyGooglePlayServicesUtilShadow.class,
        AdvertisingIdProviderTest.MyAdIdClientInfoShadow.class,
})
public class AdvertisingIdProviderTest {

    private ExecutorService executorService;
    private final String defaultId = "L00MB@";

    @Before
    public void setUp() throws Exception {
        executorService = mock(ExecutorService.class);
    }

    @Test
    public void whenGooglePlayServicesIsUnavailable() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = false;
        AdvertisingIdProvider subject = new AdvertisingIdProvider(Robolectric.application, executorService, defaultId);
        assertThat(subject.getAdvertisingId()).isEqualTo(MySettingsSecureShadow.DEVICE_ID);
    }

    @Test
    public void whenAdvertisingIdIsUnavailable() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        MyAdvertisingIdClientShadow.ADVERTISING_ID = null;
        AdvertisingIdProvider subject = new AdvertisingIdProvider(Robolectric.application, executorService, defaultId);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        Thread thread = new Thread(runnableArgumentCaptor.getValue());
        thread.start();
        thread.join();

        assertThat(subject.getAdvertisingId()).isEqualTo(MySettingsSecureShadow.DEVICE_ID);
    }

    @Test
    public void whenLimitedAdTrackingIsEnabled() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        MyAdvertisingIdClientShadow.IS_LIMITED_AD_TRACKING_ENABLED = true;
        AdvertisingIdProvider subject = new AdvertisingIdProvider(Robolectric.application, executorService, defaultId);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();

        assertThat(subject.getAdvertisingId()).isEqualTo(defaultId);
    }

    @Test
    public void whenAdvertisingIdIsAvailableForUse() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        MyAdvertisingIdClientShadow.IS_LIMITED_AD_TRACKING_ENABLED = false;
        MyAdvertisingIdClientShadow.ADVERTISING_ID = "0u812";

        AdvertisingIdProvider subject = new AdvertisingIdProvider(Robolectric.application, executorService, defaultId);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        Thread thread = new Thread(runnableArgumentCaptor.getValue());
        thread.start();
        thread.join();

        assertThat(subject.getAdvertisingId()).isEqualTo(MyAdvertisingIdClientShadow.ADVERTISING_ID);
    }

    @Implements(Settings.Secure.class)
    public static class MySettingsSecureShadow {
        public static final String DEVICE_ID = "badf00d";

        @Implementation
        public static String getString(ContentResolver contentResolver, String identifier) {
            if (Settings.Secure.ANDROID_ID.equals(identifier)) return DEVICE_ID;
            return null;
        }
    }

    @Implements(AdvertisingIdClient.class)
    public static class MyAdvertisingIdClientShadow {
        public static String ADVERTISING_ID = "0u812";
        public static boolean IS_LIMITED_AD_TRACKING_ENABLED;


        @Implementation
        public static AdvertisingIdClient.Info getAdvertisingIdInfo(Context context) {
            return RobolectricInternals.newInstance(AdvertisingIdClient.Info.class,
                    new Class[]{String.class, boolean.class},
                    new Object[]{ADVERTISING_ID, IS_LIMITED_AD_TRACKING_ENABLED});
        }
    }

    @Implements(GooglePlayServicesUtil.class)
    public static class MyGooglePlayServicesUtilShadow {
        public static boolean IS_AVAILABLE;

        @Implementation
        public static int isGooglePlayServicesAvailable(Context context) {
            return IS_AVAILABLE ? ConnectionResult.SUCCESS : ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
        }
    }

    @Implements(AdvertisingIdClient.Info.class)
    public static class MyAdIdClientInfoShadow {

        @Implementation
        public String getId() {
            String advertisingId = MyAdvertisingIdClientShadow.ADVERTISING_ID;
            if (advertisingId == null) {
                throw new RuntimeException("advertising ID is 'unavailable'");
            }
            return advertisingId;
        }

        @Implementation
        public boolean isLimitAdTrackingEnabled() {
            return MyAdvertisingIdClientShadow.IS_LIMITED_AD_TRACKING_ENABLED;
        }
    }
}