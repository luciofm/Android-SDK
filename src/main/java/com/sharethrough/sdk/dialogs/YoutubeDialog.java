package com.sharethrough.sdk.dialogs;

import android.content.Context;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.media.Youtube;

public class YoutubeDialog extends WebViewDialog {
    private String youtubeId;

    public YoutubeDialog(Context context, Creative creative, BeaconService beaconService, int feedPosition, String id) {
        super(context, creative, beaconService, feedPosition);
        this.youtubeId = id;
    }

    @Override
    protected void loadPage() {
        String html = getContext().getString(R.string.youtube_html).replace("YOUTUBE_ID", youtubeId);
        String baseUrl = "https://www.youtube.com/str/" + youtubeId;
        webView.addJavascriptInterface(new VideoCompletionBeaconService(getContext(), creative, beaconService, feedPosition), "SharethroughYoutube");
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF8", baseUrl);
    }
}