package com.sharethrough.sdk.beacons;

import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import static org.mockito.Mockito.*;

public class VideoCompletionBeaconServiceTest extends TestBase {
    private BeaconService beaconService;
    private VideoCompletionBeaconService subject;
    private Creative creative;

    @Before
    public void setUp() throws Exception {
        beaconService = mock(BeaconService.class);
        creative = mock(Creative.class);
        subject = new VideoCompletionBeaconService(Robolectric.application, creative, beaconService);
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