package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.View;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sharethrough {
    public static final String API_URL_PREFIX = "http://btlr.sharethrough.com/v3?placement_key=";
    static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    public static final String USER_AGENT = System.getProperty("http.agent");
    private String key;
    private List<Creative> availableCreatives = new ArrayList<Creative>();
    private List<IAdView> waitingAdViews = Collections.synchronizedList(new ArrayList<IAdView>());

    public Sharethrough(String key) {
        this(EXECUTOR_SERVICE, key);
    }

    public Sharethrough(final ExecutorService executorService, final String key) {
        if (key == null) throw new KeyRequiredException("placement_key is required");
        this.key = key;

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = API_URL_PREFIX + key;
                    final URI uri = URI.create(urlString);
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet(uri);
                    request.addHeader("User-Agent", USER_AGENT);
                    InputStream content = client.execute(request).getEntity().getContent();
                    // TODO: handle errors
                    Response response = mapper.readValue(content, Response.class);

                    for (final Response.Creative responseCreative : response.creatives) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DefaultHttpClient client = new DefaultHttpClient();

                                    URI imageURI = URIUtils.resolve(uri, responseCreative.creative.thumbnailUrl);

                                    HttpGet imageRequest = new HttpGet(imageURI);
                                    Log.d("Sharethrough", imageURI.toString());
                                    imageRequest.addHeader("User-Agent", USER_AGENT);
                                    HttpResponse imageResponse = client.execute(imageRequest);
                                    if (imageResponse.getStatusLine().getStatusCode() == 200) {
                                        InputStream imageContent = imageResponse.getEntity().getContent();
                                        byte[] imageBytes = convertInputStreamToByteArray(imageContent);
                                        Creative creative = new Creative(responseCreative, imageBytes);
                                        if (waitingAdViews.size() > 0) {
                                            IAdView adView = waitingAdViews.remove(0);
                                            creative.putIntoAdView(adView);
                                        } else {
                                            availableCreatives.add(creative);
                                        }
                                    } else {
                                        Log.wtf("Sharethrough", "failed to load image from url: " + imageURI + " ; server said: " + imageResponse.getStatusLine().getStatusCode() + "\t" + imageResponse.getStatusLine().getReasonPhrase());
                                    }
                                } catch (IOException e) {
                                    Log.wtf("Sharethrough", "failed to load image from url: " + responseCreative.creative.thumbnailUrl, e);
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    // TODO: log more thoroughly
                    Log.wtf("Sharethrough", "failed to get ads for key " + key, e);
                }
            }
        });
    }

    private byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(inputStream.available());

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public <V extends View & IAdView> void putCreativeIntoAdView(V adView) {
        if (availableCreatives.size() > 0) {
            availableCreatives.remove(0).putIntoAdView(adView);
        } else {
            waitingAdViews.add(adView);
        }
    }
}
