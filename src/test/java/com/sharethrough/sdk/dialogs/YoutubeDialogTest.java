package com.sharethrough.sdk.dialogs;

import android.view.ViewGroup;
import android.webkit.WebView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.media.Youtube;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, WebViewDialogTest.MenuInflaterShadow.class})
public class YoutubeDialogTest {
    private Creative creative;
    private YoutubeDialog subject;
    private Youtube youtube;

    @Before
    public void setUp() throws Exception {
        youtube = mock(Youtube.class);
        when(youtube.getId()).thenReturn("ABC");
        creative = mock(Creative.class);
        when(creative.getMedia()).thenReturn(youtube);

        subject = new YoutubeDialog(Robolectric.application, creative);
        subject.show();
    }

    @Test
    public void instanceOfWebViewDialog_toPiggyBackOnSetupAndSharing() throws Exception {
        assertThat(subject).isInstanceOf(WebViewDialog.class);
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