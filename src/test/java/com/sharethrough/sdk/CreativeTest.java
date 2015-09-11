package com.sharethrough.sdk;

import com.sharethrough.sdk.media.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;

public class CreativeTest extends TestBase {

    private static final byte[] IMAGE_BYTES = new byte[0];
    private static final byte[] LOGO_BYTES = new byte[0];
    private Creative subject;
    private Response.Creative responseCreative;

    @Before
    public void setUp() throws Exception {
        responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.beacon = new Response.Creative.CreativeInner.Beacon();
        subject = new Creative(responseCreative);
    }

    @Test
    public void getType_Youtube() throws Exception {
        responseCreative.creative.action = "video";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.YOUTUBE);
    }

    @Test
    public void getType_Vine() throws Exception {
        responseCreative.creative.action = "vine";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.VINE);
    }

    @Test
    public void getType_HostedVideo() throws Exception {
        responseCreative.creative.action = "hosted-video";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.HOSTEDVIDEO);
    }

    @Test
    public void getType_Instagram() throws Exception {
        responseCreative.creative.action = "instagram";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.INSTAGRAM);
    }

    @Test
    public void getType_Pinterest() throws Exception {
        responseCreative.creative.action = "pinterest";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.PINTEREST);
    }

    @Test
    public void getType_Clickout() throws Exception {
        responseCreative.creative.action = "clickout";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.CLICKOUT);
    }

    @Test
    public void getMedia_EverythingElse() throws Exception {
        responseCreative.creative.action = "something else";
        assertThat(subject.getType()).isEqualTo(Creative.CreativeType.CLICKOUT);
    }

    @Test
    public void getPlayBeacon_ReturnsAllPlayBeacons() throws Exception {
        ArrayList<String> playBeacons = new ArrayList<>();
        playBeacons.add("BeaconOne");
        playBeacons.add("BeaconTwo");
        responseCreative.creative.beacon.play = playBeacons;

        assertThat(subject.getPlayBeacons()).isEqualTo(playBeacons);
    }

    @Test
    public void getVisibleBeacon_ReturnsAllVisbleBeacons() throws Exception {
        ArrayList<String> visibleBeacons = new ArrayList<>();
        visibleBeacons.add("BeaconOne");
        visibleBeacons.add("BeaconTwo");
        responseCreative.creative.beacon.visible = visibleBeacons;

        assertThat(subject.getVisibleBeacons()).isEqualTo(visibleBeacons);
    }

    @Test
    public void getClickBeacon_ReturnsAllClickBeacons() throws Exception {
        ArrayList<String> clickBeacons = new ArrayList<>();
        clickBeacons.add("BeaconOne");
        clickBeacons.add("BeaconTwo");
        responseCreative.creative.beacon.click = clickBeacons;

        assertThat(subject.getClickBeacons()).isEqualTo(clickBeacons);
    }

    @Test
    public void getCustomEngagementUrl_returnsCustomEngagmentURl() {
        responseCreative.creative.customEngagementLabel = "label";
        responseCreative.creative.customEngagementUrl = "custom/url";

        assertThat(subject.getCustomEngagementUrl()).isEqualTo("http://custom/url");
        assertThat(subject.getCustomEngagementLabel()).isEqualTo("label");
    }

    @Test
    public void getRelativeMediaUrl_returnsAbsoluteMediaUrl() {
        responseCreative.creative.mediaUrl = "//media.url/";
        assertThat(subject.getMediaUrl()).isEqualToIgnoringCase("http://media.url");
    }

    @Test
    public void getAbsoluteMediaUrl_returnsAbsoluteMediaUrl() {
        responseCreative.creative.mediaUrl = "http://media.url/";
        assertThat(subject.getMediaUrl()).isEqualToIgnoringCase("http://media.url");
    }
}