package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.TestWebSettings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowWebView;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@Config(emulateSdk = 18, shadows = {YoutubeDialogTest.MyWebViewShadow.class})
@RunWith(RobolectricTestRunner.class)
public class YoutubeDialogTest {
    private Creative creative;
    private YoutubeDialog subject;
    private Youtube youtube;

    @Before
    public void setUp() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 18);

        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("Title");
        when(creative.getDescription()).thenReturn("Description");
        when(creative.getAdvertiser()).thenReturn("Advertiser");
        when(creative.getThumbnailImage()).thenReturn(mock(Bitmap.class));
        when(creative.getShareUrl()).thenReturn("http://share.me/with/friends");
        youtube = mock(Youtube.class);
        when(youtube.getId()).thenReturn("ABC");
        when(creative.getMedia()).thenReturn(youtube);
        subject = new YoutubeDialog(Robolectric.application, creative);
        subject.show();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void showsTitleAdvertiserAndDescription() throws Exception {
        ArrayList<View> foundViews = new ArrayList<View>();

        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Title", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);

        foundViews.clear();
        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Description", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);

        foundViews.clear();
        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Advertiser", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
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

        assertThat(shadowWebView.getWebChromeClient()).isNotNull();
        assertThat(shadowWebView.getWebViewClient().shouldOverrideUrlLoading(webView, "anything")).isFalse();
        WebSettings settings = webView.getSettings();
        assertThat(settings.getJavaScriptEnabled()).isTrue();
        assertThat(settings.getPluginState()).isEqualTo(WebSettings.PluginState.ON);
        assertThat(settings.getMediaPlaybackRequiresUserGesture()).isFalse();
    }

    @Test
    public void cancelingUnloadsTheWebpage_soTheMusicStops() throws Exception {
        subject.cancel();

        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        assertThat(shadowOf(webView).getLastLoadedUrl()).isEqualTo("about:");
    }

    @Test
    public void sharing() throws Exception {
        ImageView shareButton = Misc.findViewOfType(ImageView.class, (ViewGroup) subject.getWindow().getDecorView().getRootView());
        assertThat(shadowOf(shareButton).getImageResourceId()).isEqualTo(android.R.drawable.ic_menu_share);

        shareButton.performClick();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Title http://share.me/with/friends");
        assertThat(shadowOf(Robolectric.application).getNextStartedActivity()).isEqualTo(Intent.createChooser(sharingIntent, "Share with"));
    }

    @Implements(WebView.class)
    public static class MyWebViewShadow extends ShadowWebView {
        private WebSettings webSettings = new MyTestWebSettings();

        @Override
        public WebSettings getSettings() {
            return webSettings;
        }
    }

    public static class MyTestWebSettings extends TestWebSettings {
        private boolean mediaPlaybackRequiresUserGesture = true;
        private PluginState pluginState = null;

        @Override
        public void setMediaPlaybackRequiresUserGesture(boolean require) {
            mediaPlaybackRequiresUserGesture = require;
        }

        @Override
        public boolean getMediaPlaybackRequiresUserGesture() {
            return mediaPlaybackRequiresUserGesture;
        }

        @Override
        public synchronized void setPluginState(PluginState state) {
            pluginState = state;
        }

        @Override
        public synchronized PluginState getPluginState() {
            return pluginState;
        }
    }
}