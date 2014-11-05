package com.sharethrough.sdk;

import android.graphics.Rect;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class AdViewTimerTaskTest {
    private BeaconService beaconService;
    private Creative creative;
    private AdView adView;
    private int visibleWidth;
    private int visibleHeight;

    @Before
    public void setUp() throws Exception {
        adView = mock(AdView.class);
        when(adView.getHeight()).thenReturn(100);
        when(adView.getWidth()).thenReturn(300);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
    }

    @Test
    public void firesABeaconJustOnce_whenItFirstBecomesHalfVisible() throws Exception {
        AdViewTimerTask subject = new AdViewTimerTask(adView, creative, beaconService);

        when(adView.isShown()).thenReturn(true);

        when(adView.getGlobalVisibleRect(any(Rect.class))).thenReturn(false);
        subject.run();
        verifyNoMoreInteractions(beaconService);

        when(adView.getGlobalVisibleRect(any(Rect.class))).thenReturn(true);

        visibleWidth = adView.getWidth();
        visibleHeight = adView.getHeight() / 2 - 1;

        when(adView.getGlobalVisibleRect(any(Rect.class))).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                Rect r = (Rect) invocationOnMock.getArguments()[0];
                r.set(0, 0, visibleWidth, visibleHeight);
                return true;
            }
        });
        subject.run();
        verifyNoMoreInteractions(beaconService);

        visibleHeight = adView.getHeight() / 2 + 1;
        subject.run();
        verify(beaconService).adVisible(adView, creative);

        subject.run();
        verifyNoMoreInteractions(beaconService);
    }
}