package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.*;
import android.webkit.TestWebSettings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;
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
import org.robolectric.util.ActivityController;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, WebViewDialogTest.MenuInflaterShadow.class})
public class WebViewDialogTest {

    private Creative creative;
    private WebViewDialog subject;
    private static final String URL = "http://www.ads.com/sharethrough";
    private ActivityController<Activity> activityController;
    private WebView webView;
    private ShadowWebView shadowWebView;

    @Before
    public void setUp() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 18);

        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("Title");
        when(creative.getShareUrl()).thenReturn("http://share.me/with/friends");
        when(creative.getMediaUrl()).thenReturn(URL);

        activityController = Robolectric.buildActivity(Activity.class).create().start().visible().resume();
        subject = new WebViewDialog(activityController.get(), creative);
        subject.show();

        webView = Misc.findViewOfType(WebView.class, (ViewGroup) subject.getWindow().getDecorView());
        shadowWebView = shadowOf(webView);
    }

    @Test
    public void hasWebView_showingMediaUrl() throws Exception {
        assertThat(shadowWebView.getLastLoadedUrl()).isEqualTo(URL);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void setsUpWebViewCorrectly() throws Exception {
        assertThat(shadowWebView.getWebChromeClient()).isNotNull();
        assertThat(shadowWebView.getWebViewClient().shouldOverrideUrlLoading(webView, "anything")).isFalse();
        WebSettings settings = webView.getSettings();
        assertThat(settings.getJavaScriptEnabled()).isTrue();
        assertThat(settings.getPluginState()).isEqualTo(WebSettings.PluginState.ON);
        assertThat(settings.getMediaPlaybackRequiresUserGesture()).isFalse();
    }

    @Test
    public void applicationPause_causesWebViewPause_soTheMusicStops() throws Exception {
        assertThat(shadowWebView.wasOnPauseCalled()).isFalse();
        activityController.pause();
        assertThat(shadowWebView.wasOnPauseCalled()).isTrue();

        assertThat(shadowWebView.wasOnResumeCalled()).isFalse();
        activityController.resume();
        assertThat(shadowWebView.wasOnResumeCalled()).isTrue();
    }

    @Test
    public void cancelingUnregistersFromLifecycleEvents_toAvoidMemoryLeaks() throws Exception {
        subject.cancel();

        assertThat(shadowWebView.wasOnPauseCalled()).isFalse();
        activityController.pause();
        assertThat(shadowWebView.wasOnPauseCalled()).isFalse();
    }

    @Test
    public void cancelingUnloadsTheWebpage_soTheMusicStops() throws Exception {
        subject.cancel();

        assertThat(shadowOf(webView).getLastLoadedUrl()).isEqualTo("about:");
    }

    @Test
    public void upButtonCancelsTheDialog() throws Exception {
        subject.onMenuItemSelected(-1, new TestMenuItem(android.R.id.home));
        assertThat(subject.isShowing()).isFalse();
    }

    @Test
    public void backButtonWhenWebViewCannotGoBack_cancels() throws Exception {
        subject.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertThat(subject.isShowing()).isFalse();
    }

    @Test
    public void backButtonWhenWebViewCanGoBack_goesBackInWebView() throws Exception {
        shadowWebView.setCanGoBack(true);
        subject.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        assertThat(subject.isShowing()).isTrue();
        assertThat(shadowOf(webView).getGoBackInvocations()).isEqualTo(1);
    }

    @Test
    public void sharing() throws Exception {
        assertThat(subject.getWindow().hasFeature(Window.FEATURE_ACTION_BAR)).isTrue();

        ArgumentCaptor<Intent> sharingIntentArgumentCapture = ArgumentCaptor.forClass(Intent.class);
        verify(MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER).setShareIntent(sharingIntentArgumentCapture.capture());
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Title http://share.me/with/friends");
        assertThat(sharingIntentArgumentCapture.getValue()).isEqualTo(sharingIntent);
    }

    @Implements(MenuInflater.class)
    public static class MenuInflaterShadow extends ShadowMenuInflater {
        public static ShareActionProvider LATEST_SHARE_ACTION_PROVIDER;

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Implementation
        @Override
        public void inflate(int resource, Menu menu) {
            super.inflate(resource, menu);

            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.menu_item_share) {
                    LATEST_SHARE_ACTION_PROVIDER = mock(ShareActionProvider.class);
                    item.setActionProvider(LATEST_SHARE_ACTION_PROVIDER);
                }
            }
        }
    }

    @Implements(WebView.class)
    public static class WebViewShadow extends ShadowWebView {
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