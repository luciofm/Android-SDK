package com.sharethrough.sdk;

import android.net.Uri;

public class Youtube implements Creative.Media {
    public static final String EMBED = "/embed/";
    private final String url;

    public Youtube(String url) {
        this.url = url;
    }

    public String getId() {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();

        if ("youtu.be".equals(host)) {
            return uri.getPath().substring(1);
        } else if (uri.getPath().startsWith(EMBED)) {
            return uri.getPath().substring(EMBED.length());
        } else {
            return uri.getQueryParameter("v");
        }
    }
}
