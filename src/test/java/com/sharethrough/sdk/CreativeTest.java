package com.sharethrough.sdk;

import com.sharethrough.sdk.media.Clickout;
import com.sharethrough.sdk.media.Youtube;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class CreativeTest {

    private static final byte[] IMAGE_BYTES = new byte[0];
    private Creative subject;
    private Response.Creative responseCreative;

    @Before
    public void setUp() throws Exception {
        responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        subject = new Creative(responseCreative, IMAGE_BYTES, "placement key");
    }

    @Test
    public void getMedia_Youtube() throws Exception {
        responseCreative.creative.action = "video";
        assertThat(subject.getMedia()).isInstanceOf(Youtube.class);
    }

    @Test
    public void getMedia_Clickout() throws Exception {
        responseCreative.creative.action = "clickout";
        assertThat(subject.getMedia()).isInstanceOf(Clickout.class);
    }

    @Test
    public void getMedia_EverythingElse() throws Exception {
        responseCreative.creative.action = "something else";
        assertThat(subject.getMedia()).isInstanceOf(Clickout.class);
    }
}