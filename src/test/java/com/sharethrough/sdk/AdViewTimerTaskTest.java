package com.sharethrough.sdk;

import android.graphics.Rect;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class AdViewTimerTaskTest {
    private BeaconService beaconService;
    private Creative creative;
    private AdView adView;

    @Before
    public void setUp() throws Exception {
        adView = mock(AdView.class);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
    }

    @Test
    public void firesABeaconJustOnce_whenItFirstBecomesVisible() throws Exception {
        AdViewTimerTask subject = new AdViewTimerTask(adView, creative, beaconService);

        when(adView.isShown()).thenReturn(true);
        when(adView.getGlobalVisibleRect(any(Rect.class))).thenReturn(true);

        subject.run();
        verify(beaconService).adVisible(adView, creative);

        subject.run();
        verifyNoMoreInteractions(beaconService);
    }
}