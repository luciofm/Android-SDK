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
    protected String videoViewTag = "SharethroughAutoPlayVideoView";
    protected final Creative creative;
    protected BeaconService beaconService;
    protected int feedPosition;

    protected Object videoCompletedLock = new Object();
    protected boolean isVideoPrepared = false;

    protected Timer silentAutoPlayBeaconTimer;
    protected PlaybackTimerTask silentAutoPlayBeaconTask;

    public AutoPlayVideo(Creative creative, BeaconService beaconService, int feedPosition) {
        this.creative = creative;
        this.beaconService = beaconService;
        this.feedPosition = feedPosition;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        if (silentAutoPlayBeaconTimer != null) {
            silentAutoPlayBeaconTimer.cancel();
        }

        int currentPosition = 0;
        VideoView videoView = (VideoView)view.findViewWithTag(videoViewTag);
        if (videoView != null && videoView.isPlaying() && videoView.canPause()) {
            videoView.pause();
            currentPosition = videoView.getCurrentPosition();
        }

        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService, feedPosition), feedPosition, currentPosition).show();
    }

    @Override
    public void fireAdClickBeacon(Creative creative, IAdView adView, BeaconService beaconService, int feedPosition, Placement placement) {
        beaconService.adClicked("videoPlay", creative, adView.getAdView(), feedPosition, placement);

        int currentPosition = 0;
        VideoView videoView = (VideoView)adView.getAdView().findViewWithTag(videoViewTag);
        if (videoView != null) {
            currentPosition = videoView.getCurrentPosition();
        }
        beaconService.autoplayVideoEngagement(adView.getAdView().getContext(), creative, currentPosition, feedPosition);
    }

    @Override
    public void wasRendered(final IAdView adView, ImageView thumbnailImage) {
        super.wasRendered(adView, thumbnailImage);
        thumbnailImage.setVisibility(View.INVISIBLE);
        addVideoPlayerAndSetListeners(adView, thumbnailImage);
    }

    private void addVideoPlayerAndSetListeners(final IAdView adView, ImageView thumbnailImage) {
        final VideoView videoView = new VideoView(adView.getAdView().getContext().getApplicationContext());
        videoView.setTag(videoViewTag);

        FrameLayout thumbnailContainer = adView.getThumbnail();
        thumbnailContainer.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        Uri uri = Uri.parse(getCreative().getMediaUrl());
        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (((VideoCreative)creative).isVideoCompleted() || creative.wasClicked()) return;

                mediaPlayer.setLooping(false);
                mediaPlayer.setVolume(0f, 0f);
                mediaPlayer.seekTo(((VideoCreative)creative).getCurrentPosition());
                mediaPlayer.start();
                scheduleSilentAutoplayBeaconTask(videoView);
                isVideoPrepared = true;

            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                synchronized (videoCompletedLock) {
                    ((VideoCreative)creative).setVideoCompleted(true);
                    silentAutoPlayBeaconTimer.cancel();
                }

                mediaPlayer.stop();
            }
        });

        adView.setScreenVisibilityListener(new IAdView.ScreenVisibilityListener() {
            @Override
            public void onScreen() {
                synchronized (videoCompletedLock) {
                    if (!isVideoPrepared || ((VideoCreative)creative).isVideoCompleted() || creative.wasClicked()) {
                        return;
                    }
                    if (!videoView.isPlaying()) {
                        videoView.start();
                        scheduleSilentAutoplayBeaconTask(videoView);
                        Logger.d("danica video play");
                    }
                }
            }

            @Override
            public void offScreen() {
                if (videoView.isPlaying() && videoView.canPause()) {
                    ((VideoCreative)creative).setCurrentPosition(videoView.getCurrentPosition());
                    videoView.pause();
                    silentAutoPlayBeaconTimer.cancel();
                    Logger.d("danica video pause");
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
                    beaconService.silentAutoPlayDuration(videoView.getContext(), creative, 3000, feedPosition);
                    Logger.d("Danica 3 second");
                }
                else if (videoView.getCurrentPosition() >= 10000 && videoView.getCurrentPosition() <11000) {
                    beaconService.silentAutoPlayDuration(videoView.getContext(), creative, 10000, feedPosition);
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
