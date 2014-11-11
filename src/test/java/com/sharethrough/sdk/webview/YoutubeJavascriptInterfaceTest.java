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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

    @Test
    public void firesBeaconWhenFirstQuarterHasPlayed_onlyOnce() throws Exception {
        subject.timeUpdate(0.9, 4.0);
        verifyNoMoreInteractions(beaconService);
        subject.timeUpdate(1.1, 4.0);
        verify(beaconService).videoPlayed(Robolectric.application, creative, 25);
        subject.timeUpdate(1.1, 4.0);
        verifyNoMoreInteractions(beaconService);
    }
}