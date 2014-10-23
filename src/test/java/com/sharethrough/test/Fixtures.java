package com.sharethrough.test;

import com.sharethrough.sdk.Misc;

import java.io.InputStream;

public class Fixtures {
    public static final String YOUTUBE_GDATA_RESPONSE = getFile("assets/youtube_gdata_response.json");

    public static String getFile(String nameRelativeToClasspath) {
            InputStream stream = ClassLoader.getSystemResourceAsStream(nameRelativeToClasspath);
            return Misc.convertStreamToString(stream);
    }
}
