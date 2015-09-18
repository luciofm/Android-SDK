package com.sharethrough.sdk.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
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
    protected VideoCompletionBeaconService videoCompletionBeaconService;
    protected int feedPosition;

    protected Object videoStateLock = new Object();
    protected boolean isVideoPrepared = false;
    protected boolean isVideoPlaying = false;

    protected Timer silentAutoPlayBeaconTimer;
    protected SilentAutoplayBeaconTask silentAutoplayBeaconTask;
    protected Timer videoCompletionBeaconTimer;
    protected VideoCompletionBeaconTask videoCompletionBeaconTask;

    final Handler handler = new Handler(Looper.getMainLooper());

    public AutoPlayVideo(Creative creative, BeaconService beaconService, VideoCompletionBeaconService videoCompletionBeaconService, int feedPosition) {
        this.creative = creative;
        this.beaconService = beaconService;
        this.videoCompletionBeaconService = videoCompletionBeaconService;
        this.feedPosition = feedPosition;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        cancelSilentAutoplayBeaconTask();
        cancelVideoCompletionBeaconTask();

        int currentPosition = 0;
        VideoView videoView = (VideoView)view.findViewWithTag(videoViewTag);

        synchronized (videoStateLock) {
            if (videoView != null && isVideoPrepared && videoView.isPlaying() && videoView.canPause()) {
                videoView.stopPlayback();
                currentPosition = videoView.getCurrentPosition();
                isVideoPrepared = false;
                isVideoPlaying = false;
            }
        }

        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), videoCompletionBeaconService, feedPosition, currentPosition).show();
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
        thumbnailImage.setVisibility(View.INVISIBLE);
        addVideoPlayerToAdViewAndSetListeners(adView, thumbnailImage);
    }

    protected void addVideoPlayerToAdViewAndSetListeners(final IAdView adView, final ImageView thumbnailImage) {
        final VideoView videoView = getVideoView(adView.getAdView().getContext().getApplicationContext());
        videoView.setTag(videoViewTag);

        final FrameLayout thumbnailContainer = adView.getThumbnail();
        thumbnailContainer.addView(videoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        Uri uri = Uri.parse(getCreative().getMediaUrl());
        videoView.setVideoURI(uri);
        videoView.setBackgroundResource(android.R.color.transparent);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                if (((VideoCreative)creative).isVideoCompleted() || creative.wasClicked()) return;
                videoView.seekTo(((VideoCreative)creative).getCurrentPosition());
                mediaPlayer.setLooping(false);
                mediaPlayer.setVolume(0f, 0f);
                Logger.d("VideoView prepared: %s", creative.getTitle());
                isVideoPrepared = true;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                synchronized (videoStateLock) {
                    ((VideoCreative) creative).setVideoCompleted(true);
                    cancelSilentAutoplayBeaconTask();
                    cancelVideoCompletionBeaconTask();
                    videoView.pause();
                    Logger.d("VideoView pause: %s", creative.getTitle());
                    isVideoPrepared = false;
                    isVideoPlaying = false;
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Logger.d("Autoplay MediaPlayer error, error code: %d, extra: %d", what, extra);
                return false;
            }
        });

        adView.setScreenVisibilityListener(new IAdView.ScreenVisibilityListener() {
            @Override
            public void onScreen() {
                synchronized (videoStateLock) {
                    if (creative.wasClicked() || ((VideoCreative)creative).isVideoCompleted()) return;
                    if (isVideoPrepared && !isVideoPlaying) {
                        videoView.start();
                        Logger.d("VideoView start: %s", creative.getTitle());
                        isVideoPlaying = true;
                        scheduleSilentAutoplayBeaconTask(videoView);
                        scheduleVideoCompletionBeaconTask(videoView);
                    }
                }
            }

            @Override
            public void offScreen() {
                synchronized (videoStateLock) {
                    if (isVideoPrepared && videoView.isPlaying() && videoView.canPause()) {
                        ((VideoCreative) creative).setCurrentPosition(videoView.getCurrentPosition());
                        cancelSilentAutoplayBeaconTask();
                        cancelVideoCompletionBeaconTask();
                        videoView.pause();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                thumbnailContainer.removeView(videoView);
                            }
                        });
                        Logger.d("VideoView stop playback: %s", creative.getTitle());
                        isVideoPrepared = false;
                        isVideoPlaying = false;
                    }
                }
            }
        });
    }

    protected void scheduleVideoCompletionBeaconTask(final VideoView videoView) {
        cancelVideoCompletionBeaconTask();

        videoCompletionBeaconTimer = getTimer("VideoCompletionBeaconTimer for " + creative);
        videoCompletionBeaconTask = new VideoCompletionBeaconTask(videoView);
        videoCompletionBeaconTimer.scheduleAtFixedRate(videoCompletionBeaconTask, 1000, 1000);
    }

    protected void cancelVideoCompletionBeaconTask() {
        if (videoCompletionBeaconTimer != null && videoCompletionBeaconTask != null) {
            videoCompletionBeaconTimer.cancel();
            videoCompletionBeaconTask.cancel();
            videoCompletionBeaconTimer.purge();
        }
    }

    protected void scheduleSilentAutoplayBeaconTask(VideoView videoView) {
        cancelSilentAutoplayBeaconTask();

        silentAutoPlayBeaconTimer = getTimer("SilentAutoplayBeaconTimer for " + creative);
        silentAutoplayBeaconTask = new SilentAutoplayBeaconTask(videoView);
        silentAutoPlayBeaconTimer.scheduleAtFixedRate(silentAutoplayBeaconTask, 1000, 1000);
    }

    protected void cancelSilentAutoplayBeaconTask() {
        if (silentAutoplayBeaconTask != null && silentAutoPlayBeaconTimer != null) {
            silentAutoPlayBeaconTimer.cancel();
            silentAutoplayBeaconTask.cancel();
            silentAutoPlayBeaconTimer.purge();
        }
    }

    public class VideoCompletionBeaconTask extends TimerTask {
        private VideoView videoView;
        private boolean isCancelled;
        public VideoCompletionBeaconTask (VideoView videoView) {
            this.videoView = videoView;
        }

        @Override
        public void run() {
            if (isCancelled) return;

            try {
                if (videoView != null && videoView.isPlaying()) {
                    videoCompletionBeaconService.timeUpdate(videoView.getCurrentPosition(), videoView.getDuration());
                }
            } catch (Throwable tx) {
                Logger.w("Video percentage beacon error: %s", tx.toString());
            }
        }

        @Override
        public boolean cancel() {
            isCancelled = true;
            return super.cancel();
        }
    }

    public class SilentAutoplayBeaconTask extends TimerTask {
        private VideoView videoView;
        private boolean isCancelled;

        public SilentAutoplayBeaconTask (VideoView videoView) {
            this.videoView = videoView;
        }
        @Override
        public void run() {
            if (isCancelled) return;

            if (creative.wasClicked()) return;

            if (videoView == null) {
                cancel();
                return;
            }

            if (videoView.isPlaying()) {
                if (videoView.getCurrentPosition() >= 3000 && videoView.getCurrentPosition() < 4000) {
                    beaconService.silentAutoPlayDuration(videoView.getContext(), creative, 3000, feedPosition);
                } else if (videoView.getCurrentPosition() >= 10000 && videoView.getCurrentPosition() < 11000) {
                    beaconService.silentAutoPlayDuration(videoView.getContext(), creative, 10000, feedPosition);
                } else if (videoView.getCurrentPosition() >= 11000) {
                    cancel();
                }
            }
        }

        @Override
        public boolean cancel() {
            isCancelled = true;
            return super.cancel();
        }
    }

    protected Timer getTimer(String timerName) {
        return new Timer(timerName);
    }

    protected VideoView getVideoView(Context context) {
        return new VideoView(context);
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
