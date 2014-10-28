package com.sharethrough.test;

import com.sharethrough.sdk.Misc;

import java.io.InputStream;

public class Fixtures {
    public static String getFile(String nameRelativeToClasspath) {
            InputStream stream = ClassLoader.getSystemResourceAsStream(nameRelativeToClasspath);
            return Misc.convertStreamToString(stream);
    }
}
