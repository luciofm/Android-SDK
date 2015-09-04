package com.sharethrough.sdk.media;

import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.VideoDialog;

import java.util.Timer;
import java.util.TimerTask;

public class AutoPlayVideo extends Media {
    private String videoViewTag = "SharethroughAutoPlayVideoView";
    private final Creative creative;

    private Object videoCompletedLock = new Object();
    private boolean isVideoPrepared = false;

    Timer silentAutoPlayBeaconTimer;
    PlaybackTimerTask silentAutoPlayBeaconTask;

    public AutoPlayVideo(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        silentAutoPlayBeaconTimer.cancel();

        VideoView videoView = (VideoView)view.findViewWithTag(videoViewTag);
        if (videoView.isPlaying() && videoView.canPause()) {
            videoView.pause();
        }

        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService, feedPosition), feedPosition, videoView.getCurrentPosition()).show();
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
        //TODO: change this to auto play relevent beacon.
        beaconService.adClicked("videoPlay", creative, adView.getAdView(), feedPosition, placement);

    }

    @Override
    public void swapMedia(final IAdView adView, ImageView thumbnailImage) {
        super.swapMedia(adView, thumbnailImage);

        thumbnailImage.setVisibility(View.INVISIBLE);

        final VideoView videoView = new VideoView(adView.getAdView().getContext().getApplicationContext());
        videoView.setTag(videoViewTag);

        FrameLayout thumbnailContainer = adView.getThumbnail();
        thumbnailContainer.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        Uri uri = Uri.parse(getCreative().getMediaUrl());
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (((VideoCreative)creative).hasVideoCompleted || creative.wasClicked()) return;

                mediaPlayer.setLooping(false);
                mediaPlayer.setVolume(0f, 0f);
                mediaPlayer.seekTo(((VideoCreative)creative).currentPosition);
                mediaPlayer.start();
                scheduleSilentAutoplayBeaconTask(videoView);
                isVideoPrepared = true;

            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                synchronized (videoCompletedLock) {
                    ((VideoCreative)creative).hasVideoCompleted = true;
                    silentAutoPlayBeaconTimer.cancel();
                }

                mediaPlayer.stop();
            }
        });

        adView.setScreenVisibilityListener(new IAdView.ScreenVisibilityListener() {
            @Override
            public void onScreen() {
                synchronized (videoCompletedLock) {
                    if (!isVideoPrepared || ((VideoCreative)creative).hasVideoCompleted || creative.wasClicked()) {
                        return;
                    }
                    if (!videoView.isPlaying()) {
                        videoView.start();
                        scheduleSilentAutoplayBeaconTask(videoView);
                        Logger.d("video play");
                    }
                }
            }

            @Override
            public void offScreen() {
                if (videoView.isPlaying() && videoView.canPause()) {
                    ((VideoCreative)creative).currentPosition = videoView.getCurrentPosition();
                    videoView.pause();
                    silentAutoPlayBeaconTimer.cancel();
                    Logger.d("video pause");
                }
            }
        } );
    }

    private void scheduleSilentAutoplayBeaconTask(VideoView videoView) {
        if (silentAutoPlayBeaconTimer != null) {
            silentAutoPlayBeaconTimer.cancel();
        }
        silentAutoPlayBeaconTimer = new Timer();
        silentAutoPlayBeaconTask = new PlaybackTimerTask(videoView);
        silentAutoPlayBeaconTimer.schedule(silentAutoPlayBeaconTask, 0, 1000);
    }

    public class PlaybackTimerTask extends TimerTask {
        private VideoView videoView;
        public PlaybackTimerTask (VideoView videoView) {
            this.videoView = videoView;
        }
        @Override
        public void run() {
            if (creative.wasClicked()) {
                return;
            }

            if (videoView!= null) {
                if (videoView.getCurrentPosition() >= 3000 && videoView.getCurrentPosition() < 4000) {
                    Logger.d("Danica 3 second");
                }
                else if (videoView.getCurrentPosition() >= 10000 && videoView.getCurrentPosition() <11000) {
                    Logger.d("Danica 10 second");

                }
                else if (videoView.getCurrentPosition() >= 11000) {
                    this.cancel();
                    Logger.d("Danica canceled");
                }
            }
        }
    }


    @Override
    public Creative getCreative() {
        return creative;
    }

    @Override
    public int getOverlayImageResourceId() {
        return R.drawable.non_yt_play;
    }
}
