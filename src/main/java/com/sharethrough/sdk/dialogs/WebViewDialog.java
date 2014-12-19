package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BaseActivityLifecycleCallbacks;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;

public class WebViewDialog extends ShareableDialog {
    protected final Creative creative;
    protected WebView webView;
    private BaseActivityLifecycleCallbacks lifecycleCallbacks;

    public WebViewDialog(Context context, Creative creative, BeaconService beaconService) {
        super(context, android.R.style.Theme_Black, beaconService);
        this.creative = creative;
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
                return false;
            }
        });

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
}
