package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.*;
import android.webkit.TestWebSettings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ShareActionProvider;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowMenuInflater;
import org.robolectric.shadows.ShadowWebView;
import org.robolectric.tester.android.view.TestMenuItem;

import static junit.framework.Assert.fail;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@Config(emulateSdk = 18, shadows = {YoutubeActivityTest.MyWebViewShadow.class, YoutubeActivityTest.MyMenuItemShadow.class})
@RunWith(RobolectricTestRunner.class)
public class YoutubeActivityTest {
    private static ShareActionProvider shareActionProvider;
    private Creative creative;
    private YoutubeActivity subject;
    private Youtube youtube;

    @Before
    public void setUp() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 18);
        shareActionProvider = mock(ShareActionProvider.class);

        Response.Creative responseCreative = new Response.Creative();
        responseCreative.creative = new Response.Creative.CreativeInner();
        responseCreative.creative.title = "Title";
        responseCreative.creative.description = "Description.";
        responseCreative.creative.advertiser = "Advertiser";
        responseCreative.creative.shareUrl = "http://share.me/with/friends";
        responseCreative.creative.mediaUrl = "http://youtu.be/ABC";
        creative = new Creative(responseCreative, new byte[] {});

        Intent intent = new Intent("").putExtra(YoutubeActivity.CREATIVE, creative);
        subject = Robolectric.buildActivity(YoutubeActivity.class).withIntent(intent).create().start().visible().resume().get();
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
    public void pausingActivity_pausesTheWebView_soTheMusicStops() throws Exception {
        ShadowWebView shadowWebView = shadowOf(Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView()));
        assertThat(shadowWebView.wasOnPauseCalled()).isFalse();

        subject.onPause();
        assertThat(shadowWebView.wasOnPauseCalled()).isTrue();
    }

    @Test
    public void upButtonFinishesTheActivity() throws Exception {
        subject.onMenuItemSelected(-1, new TestMenuItem(android.R.id.home));
        assertThat(subject).isFinishing();
    }

    @Test
    public void backButtonWhenWebViewCannotGoBack_finishesTheActivity() throws Exception {
        subject.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertThat(subject).isFinishing();
    }

    @Test
    public void backButtonWhenWebViewCanGoBack_goesBackInWebView() throws Exception {
        WebView webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        shadowOf(webView).setCanGoBack(true);
        subject.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertThat(subject).isNotFinishing();
        assertThat(shadowOf(webView).getGoBackInvocations()).isEqualTo(1);
    }

    @Test
    public void sharing() throws Exception {
        assertThat(subject.getWindow().hasFeature(Window.FEATURE_ACTION_BAR)).isTrue();

        ArgumentCaptor<Intent> sharingIntentArgumentCapture = ArgumentCaptor.forClass(Intent.class);
        verify(shareActionProvider).setShareIntent(sharingIntentArgumentCapture.capture());
        Intent sharingIntent = new Intent(Intent.ACTION_SEND)
            .setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Title http://share.me/with/friends");
        assertThat(sharingIntentArgumentCapture.getValue()).isEqualTo(sharingIntent);
    }

    @Implements(MenuInflater.class)
    public static class MyMenuItemShadow extends ShadowMenuInflater {
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Implementation
        @Override
        public void inflate(int resource, Menu menu) {
            super.inflate(resource, menu);

            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.menu_item_share) {
                    item.setActionProvider(shareActionProvider);
                }
            }
        }
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