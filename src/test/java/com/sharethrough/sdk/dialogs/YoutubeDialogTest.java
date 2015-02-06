package com.sharethrough.sdk.dialogs;

import android.view.ViewGroup;
import android.webkit.WebView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.media.Youtube;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@Config(shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
public class YoutubeDialogTest extends TestBase {
    private Creative creative;
    private YoutubeDialog subject;
    private Youtube youtube;
    private BeaconService beaconService;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        youtube = mock(Youtube.class);
        when(youtube.getId()).thenReturn("ABC");
        creative = mock(Creative.class);
        when(creative.getMedia()).thenReturn(youtube);
        feedPosition = 5;

        beaconService = mock(BeaconService.class);

        subject = new YoutubeDialog(Robolectric.application, creative, beaconService, feedPosition);
        subject.show();
    }

    @Test
    public void instanceOfWebViewDialog_toPiggyBackOnSetupAndSharing() throws Exception {
        assertThat(subject).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void usesJSInterface() throws Exception {
        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        assertThat(shadowOf(webView).getJavascriptInterface("SharethroughYoutube")).isInstanceOf(VideoCompletionBeaconService.class);
    }

    @Test
    public void showsVideo() throws Exception {
        Robolectric.pauseMainLooper();

        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        ShadowWebView shadowWebView = shadowOf(webView);
        ShadowWebView.LoadDataWithBaseURL loadedWebData = shadowWebView.getLastLoadDataWithBaseURL();

        assertThat(loadedWebData.data).isEqualTo(Robolectric.application.getString(R.string.youtube_html).replace("YOUTUBE_ID", "ABC"));
        assertThat(loadedWebData.baseUrl).startsWith("https://www.youtube.com/");
        assertThat(loadedWebData.historyUrl).startsWith("https://www.youtube.com/");
        assertThat(loadedWebData.mimeType).isEqualTo("text/html");
    }
}