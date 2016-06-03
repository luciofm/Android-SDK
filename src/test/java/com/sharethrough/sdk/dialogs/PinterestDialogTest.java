package com.sharethrough.sdk.dialogs;

import android.webkit.CookieManager;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PinterestDialogTest extends TestBase {

    private Creative creative;
    private PinterestDialog subject;
    private String url;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        feedPosition = 5;
        creative = mock(Creative.class);
        url = "http://ab.co/";
        when(creative.getMediaUrl()).thenReturn(url);
        subject = new PinterestDialog(RuntimeEnvironment.application, creative, mock(BeaconService.class), feedPosition);
    }

    @Test
    public void isBasicallyAWebViewDialog() throws Exception {
        assertThat(subject).isInstanceOf(WebViewDialog.class);
    }

    @Test
    public void setsCookieToStayInBrowserInsteadOfInterstitialToInstallApp() throws Exception {
        assertThat(CookieManager.getInstance().getCookie(url)).isEqualTo("stay_in_browser=1");
    }
}