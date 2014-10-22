package com.sharethrough.sdk;

import com.sharethrough.test.Fixtures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    @Test
    public void doWithMediaUrl_callsMediaUrlHandlerFunctionwithProperUrl() throws Exception {
        Youtube subject = new Youtube("http://youtu.be/12345");
        ExecutorService executorService = mock(ExecutorService.class);
        Function function = mock(Function.class);

        subject.doWithMediaUrl(executorService, function);
        ArgumentCaptor<Runnable> argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(argumentCaptor.capture());
        Robolectric.addHttpResponseRule("GET", "http://gdata.youtube.com/feeds/api/videos?format=1&alt=json&q=12345",
                new TestHttpResponse(200, Fixtures.YOUTUBE_GDATA_RESPONSE));
        argumentCaptor.getValue().run();

        verify(function).apply("rtsp://cdn.youtube.com/video.3gp");
    }
}