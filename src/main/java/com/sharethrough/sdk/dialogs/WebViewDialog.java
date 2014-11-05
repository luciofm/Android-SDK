package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BaseActivityLifecycleCallbacks;
import com.sharethrough.sdk.Creative;

public class WebViewDialog extends Dialog {
    protected final Creative creative;
    protected WebView webView;
    private BaseActivityLifecycleCallbacks lifecycleCallbacks;

    public WebViewDialog(Context context, Creative creative) {
        super(context, android.R.style.Theme_Black);
        this.creative = creative;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
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
                Log.d("Sharethrough", "onPause:" + activity);
                webView.onPause();
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Log.d("Sharethrough", "onResume:" + activity);
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
        Log.d("Sharethrough", "WebView showing: " + creative.getMediaUrl());
        webView.loadUrl(creative.getMediaUrl());
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(getContext().getResources().getColor(android.R.color.transparent)));

        new MenuInflater(getContext()).inflate(R.menu.share_menu, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        ShareActionProvider shareActionProvider = (ShareActionProvider) item.getActionProvider();
        shareActionProvider.setShareIntent(new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, creative.getTitle() + " " + creative.getShareUrl()));

        // Return true to display menu
        return super.onCreateOptionsMenu(menu);
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
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            cancel();
            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }
}