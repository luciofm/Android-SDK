package com.sharethrough.sdk.media;

import android.content.Context;
import android.widget.VideoView;

/**
 * Created by newuser on 9/17/15.
 */
public class AutoplayVideoView extends VideoView {
    public AutoplayVideoView(Context context) {
        super(context);
    }

    /*public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            System.out.println("danica reset");
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState  = STATE_IDLE;
        }
    }*/
}
