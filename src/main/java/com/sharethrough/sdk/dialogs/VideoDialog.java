package com.sharethrough.sdk.dialogs;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;

public class VideoDialog extends ShareableDialog {
    private Creative creative;
    private final boolean isLooping;
    private VideoView videoView;

    public VideoDialog(Context context, Creative creative, BeaconService beaconService, boolean isLooping) {
        super(context, android.R.style.Theme_Black, beaconService);
        this.creative = creative;
        this.isLooping = isLooping;
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
            public void onPrepared(MediaPlayer mediaPlayer) {
                findViewById(R.id.progress_spinner).setVisibility(View.GONE);
                videoView.start();
                mediaPlayer.setLooping(isLooping);
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
