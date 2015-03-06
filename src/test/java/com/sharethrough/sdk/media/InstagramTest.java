package com.sharethrough.sdk.media;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InstagramTest extends TestBase {
    private Creative creative;
    private BeaconService beaconService;
    private Instagram subject;
    private int feedPosition;
    @Mock private Placement placement;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);

        subject = new Instagram(creative);
        feedPosition = 5;
    }

    @Test
    public void thumbnailImageOverlaysInstagramIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.instagram);
    }

    @Test
    public void firesClickoutBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService, feedPosition, placement);
        verify(beaconService).adClicked("clickout", creative, adView.getAdView(), feedPosition, placement);
    }
}
