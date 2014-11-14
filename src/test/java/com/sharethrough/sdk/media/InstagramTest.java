package com.sharethrough.sdk.media;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.RendererTest;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.test.util.TestAdView;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InstagramTest extends TestBase {
    private Creative creative;
    private BeaconService beaconService;
    private Instagram subject;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        beaconService = mock(BeaconService.class);

        subject = new Instagram(creative);
    }

    @Test
    public void thumbnailImageOverlaysInstagramIcon() throws Exception {
        assertThat(subject.getOverlayImageResourceId()).isEqualTo(R.drawable.instagram);
    }

    @Test
    public void firesClickoutBeacon() throws Exception {
        TestAdView adView = RendererTest.makeAdView();
        subject.fireAdClickBeacon(creative, adView, beaconService);
        verify(beaconService).adClicked("clickout", creative, adView.getAdView());
    }
}
