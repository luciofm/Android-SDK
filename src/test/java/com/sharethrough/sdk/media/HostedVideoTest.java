package com.sharethrough.sdk.media;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class HostedVideoTest {
    @Test
    public void overlaysImage() throws Exception {
        assertThat(new HostedVideo(mock(Creative.class), mock(BeaconService.class)).getOverlayImageResourceId()).isEqualTo(R.drawable.hosted_video);
    }
}