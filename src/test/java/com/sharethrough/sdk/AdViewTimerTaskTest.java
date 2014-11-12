package com.sharethrough.sdk;

import android.graphics.Rect;
import android.os.Handler;
import com.sharethrough.test.util.AdView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class AdViewTimerTaskTest {
    private BeaconService beaconService;
    private Creative creative;
    private AdView adView;
    private int visibleWidth;
    private int visibleHeight;
    private Provider<Date> dateProvider;
    private long now;
    private boolean isVisible;
    private AdViewTimerTask subject;
    private Sharethrough sharethrough;
    private Handler handler;

    @Before
    public void setUp() throws Exception {
        adView = mock(AdView.class);
        when(adView.getHeight()).thenReturn(100);
        when(adView.getWidth()).thenReturn(300);
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);
        now = 0;
        dateProvider = new Provider<Date>() {
            @Override
            public Date get() {
                return new Date(now);
            }
        };
        sharethrough = mock(Sharethrough.class);
        when(sharethrough.getAdCacheTimeInMilliseconds()).thenReturn((int) TimeUnit.SECONDS.toMillis(20));

        handler = mock(Handler.class);

        subject = new AdViewTimerTask(adView, creative, beaconService, dateProvider, sharethrough);
    }

    @Test
    public void firesABeaconJustOnce_whenItFirstBecomesHalfVisibleFor1ContinuousSecond() throws Exception {
        when(adView.isShown()).thenReturn(true);

        // invisible
        when(adView.getGlobalVisibleRect(any(Rect.class))).thenReturn(false);
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible but not 50%
        visibleWidth = adView.getWidth();
        visibleHeight = adView.getHeight() / 2 - 1;

        when(adView.getGlobalVisibleRect(any(Rect.class))).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                Rect r = (Rect) invocationOnMock.getArguments()[0];
                r.set(0, 0, visibleWidth, visibleHeight);
                return isVisible;
            }
        });
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible but not for a second yet
        visibleHeight = adView.getHeight() / 2 + 1;
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible but not 50%
        visibleHeight = adView.getHeight() / 2 - 1;
        now += 100;
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible but not for a continuous second yet
        visibleHeight = adView.getHeight() / 2 + 1;
        now += 900;
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // invisible
        isVisible = false;
        now += 100;
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible but not for a continuous second yet
        isVisible = true;
        visibleHeight = adView.getHeight() / 2 + 1;
        subject.run();
        verifyNoMoreInteractions(beaconService);

        // visible for a continuous second yet
        now += 1000;
        subject.run();
        verify(beaconService).adVisible(adView, creative);

        // still visible, still over a second, but no need for a duplicate beacon
        subject.run();
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void whenAdViewBecomesVisible_andBecomesInvisible_showsNewAdAfterCacheExpires() throws Exception {
        reset(sharethrough);

        makeAdViewVisible();

        isVisible = false;

        now += TimeUnit.SECONDS.toMillis(18);
        subject.run();
        verifyNoMoreInteractions(sharethrough);

        now += TimeUnit.SECONDS.toMillis(1);
        subject.run();
        verify(sharethrough).putCreativeIntoAdView(adView, NoOp.INSTANCE);

        now += TimeUnit.SECONDS.toMillis(1);
        subject.run();
        verifyNoMoreInteractions(sharethrough);
    }

    @Test
    public void whenCacheExpires_doesShowNewAdUntilCurrentAdBecomesInvisible() throws Exception {
        reset(sharethrough);

        makeAdViewVisible();
        now += TimeUnit.SECONDS.toMillis(19);
        subject.run();
        verifyNoMoreInteractions(sharethrough);

        isVisible = false;
        subject.run();
        verify(sharethrough).putCreativeIntoAdView(adView, NoOp.INSTANCE);
    }

    private void makeAdViewVisible() {
        when(adView.isShown()).thenReturn(true);

        isVisible = true;
        visibleWidth = adView.getWidth();
        visibleHeight = adView.getHeight();
        when(adView.getGlobalVisibleRect(any(Rect.class))).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                Rect r = (Rect) invocationOnMock.getArguments()[0];
                r.set(0, 0, visibleWidth, visibleHeight);
                return isVisible;
            }
        });
        subject.run();
        now += TimeUnit.SECONDS.toMillis(1);
        subject.run();
        verify(beaconService).adVisible(adView, creative);
    }
}