package com.sharethrough.sdk;

/**
 * Created by engineer on 9/4/15.
 */
public class VideoCreative extends Creative {
    public int currentPosition = 0;
    public boolean hasVideoCompleted = false;

    public VideoCreative (Response.Creative responseCreative) {
        super(responseCreative);
    }
}
