package com.sharethrough.sdk.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.VideoView;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;

import java.util.Timer;
import java.util.TimerTask;

public class VideoDialog extends ShareableDialog {
    private final boolean isLooping;
    private final Timer timer;
    private final VideoCompletionBeaconService videoBeacons;
    private Creative creative;
    private VideoView videoView;
    private int videoStartPosition = 0;

    public VideoDialog(Context context, Creative creative, BeaconService beaconService, boolean isLooping, Timer timer, VideoCompletionBeaconService videoBeacons, int feedPosition, int videoStartPosition) {
        super(context, android.R.style.Theme_Black, beaconService, feedPosition);
        this.creative = creative;
        this.isLooping = isLooping;
        this.timer = timer;
        this.videoBeacons = videoBeacons;
        this.videoStartPosition = videoStartPosition;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_dialog);

        final View playButton = findViewById(R.id.play_button);

        videoView = (VideoView) findViewById(R.id.video);
        final String mediaUrl = creative.getMediaUrl();
        videoView.setVideoURI(Uri.parse(mediaUrl));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {
                if( videoView.canSeekForward() && videoStartPosition > 0 )
                {
                    videoView.seekTo(videoStartPosition);
                }
                videoView.start();
                mediaPlayer.setLooping(isLooping);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        videoView.bringToFront();
                        playButton.bringToFront();
                    }
                });
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            videoBeacons.timeUpdate(videoView.getCurrentPosition(), videoView.getDuration());
                        } catch (Throwable tx) {
                            Log.e("Sharethrough", "video beacons error", tx);
                        }
                    }
                }, 1000, 1000);
                setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        timer.cancel();
                    }
                });
            }
        });
        ((View) videoView.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    beaconService.videoViewDuration(videoView.getContext(), creative, videoView.getCurrentPosition(), false, feedPosition);
                    playButton.setVisibility(View.VISIBLE);
                } else {
                    videoView.start(); // oddly, resume and start do the opposite of what we'd expect
                    playButton.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            beaconService.videoViewDuration(videoView.getContext(), creative, videoView.getCurrentPosition(), false, feedPosition);
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Creative getCreative() {
        return creative;
    }
}
