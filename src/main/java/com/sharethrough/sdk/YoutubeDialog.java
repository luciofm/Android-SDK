package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.sharethrough.android.sdk.R;

public class YoutubeDialog extends Dialog {
    public YoutubeDialog(Context context, final Creative creative) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(linearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView shareButton = new ImageView(context);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, creative.getTitle() + " " + creative.getShareUrl());
                v.getContext().startActivity(Intent.createChooser(sharingIntent, "Share with"));
            }
        });
        shareButton.setImageResource(android.R.drawable.ic_menu_share);
        linearLayout.addView(shareButton);

        TextView title = new TextView(context);
        title.setText(creative.getTitle());
        linearLayout.addView(title);

        TextView description = new TextView(context);
        description.setText(creative.getDescription());
        linearLayout.addView(description);

        TextView advertiser = new TextView(context);
        advertiser.setText(creative.getAdvertiser());
        linearLayout.addView(advertiser);

        linearLayout.addView(showVideo(context, creative));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private WebView showVideo(Context context, Creative creative) {
        final WebView webView = new WebView(context);

        String youtubeId = ((Youtube) creative.getMedia()).getId();
        String html = context.getString(R.string.youtube_html).replace("YOUTUBE_ID", youtubeId);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        String baseUrl = "https://www.youtube.com/str/" + youtubeId;
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF8", baseUrl);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            settings.setPluginState(WebSettings.PluginState.ON);
        }

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                webView.loadUrl("about:");
            }
        });

        return webView;
    }
}