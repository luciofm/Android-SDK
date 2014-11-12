package com.sharethrough.sdk.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.webview.VideoCompletionBeaconService;

import java.util.Timer;
import java.util.TimerTask;

public class VideoDialog extends ShareableDialog {
    private Creative creative;
    private final boolean isLooping;
    private final Timer timer;
    private VideoView videoView;
    private final VideoCompletionBeaconService videoBeacons;

    public VideoDialog(Context context, Creative creative, BeaconService beaconService, boolean isLooping, Timer timer, VideoCompletionBeaconService videoBeacons) {
        super(context, android.R.style.Theme_Black, beaconService);
        this.creative = creative;
        this.isLooping = isLooping;
        this.timer = timer;
        this.videoBeacons = videoBeacons;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_dialog);
        videoView = (VideoView) findViewById(R.id.video);
        final String mediaUrl = creative.getMediaUrl();
        Log.d("Sharethrough", "loading video from: " + mediaUrl);
        videoView.setVideoURI(Uri.parse(mediaUrl));
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mediaPlayer) {
                findViewById(R.id.progress_spinner).setVisibility(View.GONE);
                videoView.start();
                mediaPlayer.setLooping(isLooping);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        videoBeacons.timeUpdate(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
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
                View playButton = findViewById(R.id.play_button);
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playButton.setVisibility(View.VISIBLE);
                } else {
                    videoView.start(); // oddly, resume and start do the opposite of what we'd expect
                    playButton.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected Creative getCreative() {
        return creative;
    }
}
