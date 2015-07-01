package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BaseActivityLifecycleCallbacks;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.media.Article;

import java.util.Calendar;

public class WebViewDialog extends ShareableDialog {
    protected final Creative creative;
    protected WebView webView;
    private BaseActivityLifecycleCallbacks lifecycleCallbacks;
    private String originalHost="";
    private boolean timeInViewBeaconHasFired = false;
    protected long startTimeInArticle;

    public WebViewDialog(Context context, Creative creative, BeaconService beaconService, int feedPosition) {
        super(context, R.style.SharethroughBlackTheme, beaconService, feedPosition);
        this.creative = creative;
    }

    private boolean isArticleType() {
        return creative.getMedia() instanceof Article;
    }

    protected void fireTimeInViewBeacon(){
        fireTimeInViewBeacon(Calendar.getInstance().getTimeInMillis());
    }
    protected void fireTimeInViewBeacon(long endTime) {
        //we only fire this beacon if it is an article type media
        if(isArticleType()) {
            //we only fire this beacon once per webview
            if (!timeInViewBeaconHasFired) {
                timeInViewBeaconHasFired = true;
                //fire beacon
                long totalTimeSpent = endTime - startTimeInArticle;
                totalTimeSpent = totalTimeSpent >= 0? totalTimeSpent:0;
                beaconService.fireArticleDurationForAd(getContext(),creative, totalTimeSpent);
            }
        }

    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        webView = (WebView) findViewById(R.id.web);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //is user navigating away from the original content?
                if (url != null && false == url.isEmpty()) {
                    String newHost = Uri.parse(url).getHost();
                    if (newHost != null && false == newHost.equals(originalHost)) {
                        fireTimeInViewBeacon();
                    }
                }
                return false;
            }
        });

        if(creative.getMediaUrl() != null){
            originalHost = Uri.parse(creative.getMediaUrl()).getHost();
        }

        loadPage();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settings.setPluginState(WebSettings.PluginState.ON);
        }

        lifecycleCallbacks = new BaseActivityLifecycleCallbacks() {
            @Override
            public void onActivityPaused(Activity activity) {
                fireTimeInViewBeacon();
                webView.onPause();
            }

            @Override
            public void onActivityResumed(Activity activity) {
                webView.onResume();
            }
        };

        final Application applicationContext = (Application) getContext().getApplicationContext();
        applicationContext.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                webView.loadUrl("about:");
                applicationContext.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
            }
        });
    }

    protected void loadPage() {
        startTimeInArticle = Calendar.getInstance().getTimeInMillis();
        webView.loadUrl(creative.getMediaUrl());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        fireTimeInViewBeacon();
                        cancel();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected Creative getCreative() {
        return creative;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        super.onMenuItemSelected(featureId, item);
        if (item.getItemId() == android.R.id.home) {
            fireTimeInViewBeacon();
        }
        return true;
    }
}
