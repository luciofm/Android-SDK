package com.sharethrough.sdk;

import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Config(shadows = {
        AdvertisingIdProviderTest.MyGooglePlayServicesUtilShadow.class
})
public class AdvertisingIdProviderTest extends TestBase {
    private class AdvertisingIdProviderStub extends AdvertisingIdProvider{
        private AdvertisingIdClient.Info info;

        public AdvertisingIdProviderStub(Context context, String advertisingId, boolean limitedTrackingAvailable) {
            super(context);
            info = new AdvertisingIdClient.Info(advertisingId,limitedTrackingAvailable);
        }
    }

    @Before
    public void setUp() throws Exception {
        STRExecutorService.setExecutorService(mock(ExecutorService.class));
    }

    @Test
    public void whenGooglePlayServicesIsUnavailable() throws Exception {
        AdvertisingIdProviderStub subject = new AdvertisingIdProviderStub(RuntimeEnvironment.application, "test", true);
        assertThat(subject.getAdvertisingId()).isEqualTo(null);
    }

    @Test
    public void whenAdvertisingIdIsUnavailable() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        AdvertisingIdProviderStub subject = new AdvertisingIdProviderStub(RuntimeEnvironment.application, null, true);
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(STRExecutorService.getInstance()).execute(runnableArgumentCaptor.capture());
        Thread thread = new Thread(runnableArgumentCaptor.getValue());
        thread.start();
        thread.join();

        assertThat(subject.getAdvertisingId()).isEqualTo(null);
    }

    @Test
    public void whenLimitedAdTrackingIsEnabled() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        AdvertisingIdProviderStub subject = new AdvertisingIdProviderStub(RuntimeEnvironment.application, null, true);
        com.sharethrough.test.util.Misc.runLast(STRExecutorService.getInstance());
        assertThat(subject.getAdvertisingId()).isEqualTo(null);
    }

    @Test
    public void whenAdvertisingIdIsAvailableForUse() throws Exception {
        MyGooglePlayServicesUtilShadow.IS_AVAILABLE = true;
        AdvertisingIdProviderStub subject = new AdvertisingIdProviderStub(RuntimeEnvironment.application, "0u812", false);

        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(STRExecutorService.getInstance()).execute(runnableArgumentCaptor.capture());
        Thread thread = new Thread(runnableArgumentCaptor.getValue());
        thread.start();
        thread.join();

        assertThat(subject.getAdvertisingId()).isEqualTo("0u812");
    }

    @Implements(GooglePlayServicesUtil.class)
    public static class MyGooglePlayServicesUtilShadow {
        public static boolean IS_AVAILABLE;

        @Implementation
        public static int isGooglePlayServicesAvailable(Context context) {
            return IS_AVAILABLE ? ConnectionResult.SUCCESS : ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
        }
    }
}