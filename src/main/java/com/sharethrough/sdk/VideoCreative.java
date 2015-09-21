package com.sharethrough.sdk;

/**
 * Created by engineer on 9/4/15.
 */
public class VideoCreative extends Creative {
    private int currentPosition = 0;
    private boolean isVideoCompleted = false;

    public boolean isVideoCompleted() {
        return isVideoCompleted;
    }

    public void setVideoCompleted(boolean isVideoCompleted) {
        this.isVideoCompleted = isVideoCompleted;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }
    public VideoCreative (Response.Creative responseCreative) {
        super(responseCreative);
    }
}
