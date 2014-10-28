package com.sharethrough.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class YoutubeTest {

    @Test
    public void canGetIdFromShortUrl() throws Exception {
        assertThat(new Youtube("http://youtu.be/12345").getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpUrl() throws Exception {
        assertThat(new Youtube("http://youtube.com/watch?v=12345&autoplay=true").getId()).isEqualTo("12345");
        assertThat(new Youtube("http://youtube.com/watch?autoplay=true&v=12345").getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromRegularHttpsUrl() throws Exception {
        assertThat(new Youtube("https://youtube.com/watch?v=12345&autoplay=true").getId()).isEqualTo("12345");
        assertThat(new Youtube("https://youtube.com/watch?autoplay=true&v=12345").getId()).isEqualTo("12345");
    }

    @Test
    public void canGetIdFromEmbedUrl() throws Exception {
        assertThat(new Youtube("http://www.youtube.com/embed/12345?autoplay=1&vq=small").getId()).isEqualTo("12345");
    }
}