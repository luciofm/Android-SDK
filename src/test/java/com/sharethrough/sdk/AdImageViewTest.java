package com.sharethrough.sdk;

import android.content.Context;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.Mockito.*;

public class AdImageViewTest extends TestBase {

    @Mock Sharethrough sharethrough;
    @Mock Creative creative;
    @Mock BeaconService beaconService;
    @Mock Timer timer;
    private AdImageViewStub subject;
    private TestAdView adView;
    private int feedPosition = 0;

    class AdImageViewStub extends AdImageView {
        AdImageViewStub( Context context, Sharethrough sharethrough, Creative creative, IAdView adview, int feedPosition, BeaconService beaconService) {
            super(context, creative, adview, feedPosition, beaconService );
        }

        @Override
        protected Timer getTimer()
        {
            return timer;
        }
    }

    @Before
    public void setUp() throws Exception {
        adView = mock(TestAdView.class);
        when(adView.getAdView()).thenReturn(adView);
        when(adView.isShown()).thenReturn(true);
        subject = new AdImageViewStub(RuntimeEnvironment.application, sharethrough, creative, adView, feedPosition, beaconService);

    }

    @Test
    public void WhenAdImageViewIsAttached_VerifyThatAdViewTimerTaskIsScheduled() throws Exception {
        creative.renderedTime = Integer.MAX_VALUE;
        subject.onAttachedToWindow();
        verify(timer).schedule(any(TimerTask.class), anyInt(), anyInt());
    }
}