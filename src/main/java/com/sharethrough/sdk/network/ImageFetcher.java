package com.sharethrough.sdk.network;

import android.util.Log;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Function;
import com.sharethrough.sdk.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ExecutorService;

public class ImageFetcher {
    private final ExecutorService executorService;
    private String key;

    public ImageFetcher(ExecutorService executorService, String key) {
        this.executorService = executorService;
        this.key = key;
    }

    public void fetchImage(final Response.Creative responseCreative, final Function<Creative, Void> creativeHandler, final URI apiUri) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DefaultHttpClient client = new DefaultHttpClient();

                    URI imageURI = URIUtils.resolve(apiUri, responseCreative.creative.thumbnailUrl);

                    HttpGet imageRequest = new HttpGet(imageURI);
                    Log.d("Sharethrough", "fetching image:\t" + imageURI.toString());
                    imageRequest.addHeader("User-Agent", AdFetcher.USER_AGENT);
                    HttpResponse imageResponse = client.execute(imageRequest);
                    if (imageResponse.getStatusLine().getStatusCode() == 200) {
                        InputStream imageContent = imageResponse.getEntity().getContent();
                        byte[] imageBytes = convertInputStreamToByteArray(imageContent);
                        Creative creative = new Creative(responseCreative, imageBytes, key);
                        creativeHandler.apply(creative);
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
}
