package com.sharethrough.sdk;

import android.net.Uri;
import android.util.Log;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

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

    @Override
    public void doWithMediaUrl(ExecutorService executorService, final Function<String, Void> mediaUrlHandler) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String endpoint = "http://gdata.youtube.com/feeds/api/videos?format=1&alt=json&q=" + getId();

                String json = null;
                try {
                    json = Misc.convertStreamToString(new DefaultHttpClient().execute(new HttpGet(endpoint)).getEntity().getContent());

                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
                    Response response = objectMapper.readValue(json.getBytes(), Response.class);

                    mediaUrlHandler.apply(response.feed.entry.get(0).mediaGroup.mediaContent.get(1).url);
                } catch (IOException e) {
                    Log.wtf("Sharethrough", "failed to get Youtube RTSP for " + url + " from " + json, e);
                }
            }
        });
    }

    public static class Response {
        @JsonProperty("feed") Feed feed;

        public static class Feed {
            @JsonProperty("entry") List<Entry> entry;

            public static class Entry {
                @JsonProperty("media$group") MediaGroup mediaGroup;

                public static class MediaGroup {
                    @JsonProperty("media$content") List<MediaContent> mediaContent;

                    public static class MediaContent {
                        @JsonProperty("url") String url;
                    }
                }
            }
        }
    }
}
