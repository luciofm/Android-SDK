package com.sharethrough.sdk.webview;

import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class YoutubeJavascriptInterfaceTest extends TestCase {
    private BeaconService beaconService;
    private YoutubeJavascriptInterface subject;
    private Creative creative;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        beaconService = mock(BeaconService.class);
        creative = mock(Creative.class);
        subject = new YoutubeJavascriptInterface(Robolectric.application, creative, beaconService);
    }

    public void timeUpdate_before25PercentPlayed_firesNoBeacons() throws Exception {
        subject.timeUpdate(0.9, 4.0);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void firesBeaconWhenFirstQuarterHasPlayed_onlyOnce() throws Exception {
        subject.timeUpdate(1.1, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void timeUpdate_whenHalfPlayed_firesCorrectBeacons() throws Exception {
        subject.timeUpdate(1.9, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        verifyNoMoreInteractions(beaconService);
        subject.timeUpdate(2.0, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 50);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void timeUpdate_when75PercentPlayed_firesCorrectBeacons() throws Exception {
        subject.timeUpdate(2.99, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 50);
        verifyNoMoreInteractions(beaconService);
        subject.timeUpdate(3.00, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 75);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void timeUpdate_when95PercentPlayed_firesCorrectBeacons() throws Exception {
        subject.timeUpdate(9.49, 10.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 50);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 75);
        verifyNoMoreInteractions(beaconService);
        subject.timeUpdate(9.5, 10.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 95);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void timeUpdate_onlyFiresBeaconsOnce() throws Exception {
        subject.timeUpdate(3.99, 4.0);
        subject.timeUpdate(3.99, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 50);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 75);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 95);
        verifyNoMoreInteractions(beaconService);
    }
}