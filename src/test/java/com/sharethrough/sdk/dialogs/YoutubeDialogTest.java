package com.sharethrough.sdk.dialogs;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.media.Youtube;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowWebView;

import static com.sharethrough.sdk.dialogs.ShareableDialogTest.MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Config(shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
public class YoutubeDialogTest extends TestBase {
    private Creative creative;
    private YoutubeDialog subject;
    private Youtube youtube;
    private BeaconService beaconService;
    private int feedPosition;
    private String youtubeId;

    @Before
    public void setUp() throws Exception {
        youtube = mock(Youtube.class);
        creative = mock(Creative.class);
        feedPosition = 5;
        youtubeId = "youtubeId";

        beaconService = mock(BeaconService.class);

        subject = new YoutubeDialog(RuntimeEnvironment.application, creative, beaconService, feedPosition, youtubeId);
        subject.show();
    }

    @Test
    public void instanceOfWebViewDialog_toPiggyBackOnSetupAndSharing() throws Exception {
        assertThat(subject).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void usesJSInterface() throws Exception {
        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        assertThat(Shadows.shadowOf(webView).getJavascriptInterface("SharethroughYoutube")).isInstanceOf(VideoCompletionBeaconService.class);
    }

    @Test
    public void showsVideo() throws Exception {
        ShadowLooper.pauseMainLooper();

        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        ShadowWebView shadowWebView = Shadows.shadowOf(webView);
        ShadowWebView.LoadDataWithBaseURL loadedWebData = shadowWebView.getLastLoadDataWithBaseURL();

        assertThat(loadedWebData.data).isEqualTo(RuntimeEnvironment.application.getString(R.string.youtube_html).replace("YOUTUBE_ID", "youtubeId"));
        assertThat(loadedWebData.baseUrl).startsWith("https://www.youtube.com/");
        assertThat(loadedWebData.historyUrl).startsWith("https://www.youtube.com/");
        assertThat(loadedWebData.mimeType).isEqualTo("text/html");
    }
}