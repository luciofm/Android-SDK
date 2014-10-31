package com.sharethrough.sdk.dialogs;

import android.content.Context;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.media.Youtube;

public class YoutubeDialog extends WebViewDialog {
    public YoutubeDialog(Context context, Creative creative) {
        super(context, creative);
    }

    @Override
    protected void loadPage() {
        String youtubeId = ((Youtube) creative.getMedia()).getId();
        String html = getContext().getString(R.string.youtube_html).replace("YOUTUBE_ID", youtubeId);
        String baseUrl = "https://www.youtube.com/str/" + youtubeId;
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF8", baseUrl);
    }
}