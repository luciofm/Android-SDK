package com.sharethrough.sdk.network;

import android.util.Log;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.Response;
import com.sharethrough.sdk.STRExecutorService;
import com.sharethrough.sdk.Sharethrough;
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
    private String key;

    public ImageFetcher(String key) {
        this.key = key;
    }

    public void fetchCreativeImages(final URI apiURI, final Response.Creative responseCreative, final Callback creativeHandler) {
        STRExecutorService.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DefaultHttpClient client = new DefaultHttpClient();

                    URI imageURI = URIUtils.resolve(apiURI, responseCreative.creative.thumbnailUrl);
                    HttpGet imageRequest = new HttpGet(imageURI);
                    imageRequest.addHeader("User-Agent", Sharethrough.USER_AGENT);
                    HttpResponse imageResponse = client.execute(imageRequest);
                    if (imageResponse.getStatusLine().getStatusCode() == 200) {
                        InputStream imageContent = imageResponse.getEntity().getContent();
                        byte[] imageBytes = convertInputStreamToByteArray(imageContent);
                        byte[] logoBytes = downloadBrandLogo(responseCreative.creative.brandLogoUrl);
                        Creative creative = new Creative(responseCreative, imageBytes, logoBytes, key);
                        creativeHandler.success(creative);
                    } else {
                        Log.e("Sharethrough", "failed to load image from url: " + imageURI + " ; server said: " + imageResponse.getStatusLine().getStatusCode() + "\t" + imageResponse.getStatusLine().getReasonPhrase());
                        creativeHandler.failure();
                    }
                } catch (IOException e) {
                    Log.e("Sharethrough", "failed to load image from url: " + responseCreative.creative.thumbnailUrl, e);
                    creativeHandler.failure();
                }
            }
        });
    }

    private byte[] downloadBrandLogo(final String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            StringBuilder brandLogoUrlBuilder = new StringBuilder();
            if (!url.contains("http")) {
                brandLogoUrlBuilder.append("http:");
            }
            brandLogoUrlBuilder.append(url);
            final String brandLogoUrl = brandLogoUrlBuilder.toString();

            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet imageRequest = new HttpGet(brandLogoUrl);
            imageRequest.addHeader("User-Agent", Sharethrough.USER_AGENT);
            HttpResponse imageResponse = client.execute(imageRequest);
            if (imageResponse.getStatusLine().getStatusCode() == 200) {
                InputStream imageContent = imageResponse.getEntity().getContent();
                byte[] imageBytes = convertInputStreamToByteArray(imageContent);
                return imageBytes;
            } else {
                Log.e("Sharethrough", "failed to load brand image from url: " + brandLogoUrl + " ; server said: " + imageResponse.getStatusLine().getStatusCode() + "\t" + imageResponse.getStatusLine().getReasonPhrase());
            }
        } catch (Exception e) {
            Log.e("Sharethrough", "failed to load brand image from url: " + url, e);
        }
        return null;
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

    public interface Callback {
        public void success(Creative value);

        public void failure();
    }
}
