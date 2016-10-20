package com.sharethrough.sdk;

/**
 * Created by engineer on 9/4/15.
 */
public class InstantPlayCreative extends Creative {
    private int currentPosition = 0;
    private boolean isVideoCompleted = false;

    public InstantPlayCreative(String networkType, String className, String mrid) {
        super(networkType, className, mrid);
    }

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
}
