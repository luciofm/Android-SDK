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
    private String videoViewTag;
    private final Creative creative;

    private Object lock = new Object();
    boolean hasAdBeenClicked = true;
    boolean videoCompleted = false;

    Timer timer = new Timer();
    PlaybackTimerTask silentAutoPlayBeaconTask;


    public AutoPlayVideo(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        hasAdBeenClicked = true;
        timer.cancel();

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

        FrameLayout thumbnailContainer = adView.getThumbnail();

        final VideoView vw = new VideoView(adView.getAdView().getContext().getApplicationContext());
        videoViewTag = "SharethroughAutoPlayVideoView";
        vw.setTag(videoViewTag);


        Uri uri = Uri.parse(getCreative().getMediaUrl());
        vw.setVideoURI(uri);

        vw.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(false);
                mp.setVolume(0f, 0f);
                mp.start();
                //adView.getThumbnail().addView(vw, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

            }
        });

        vw.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                synchronized (lock) {
                    videoCompleted = true;
                }

                mp.stop();
            }
        });

        silentAutoPlayBeaconTask = new PlaybackTimerTask(vw);
        timer.schedule(silentAutoPlayBeaconTask, 0, 1000);

        adView.setScreenListener( new IAdView.ScreenListener() {
            @Override
            public void onScreen() {
                synchronized (lock) {
                    if( videoCompleted || hasAdBeenClicked ){
                        return;
                    }
                    if (!vw.isPlaying()) {
                        vw.start();
                        timer.schedule(silentAutoPlayBeaconTask, 0, 1000);
                    }
                }
            }

            @Override
            public void offScreen() {
                synchronized (lock) {
                    if (vw.isPlaying() && vw.canPause()) {
                        vw.pause();
                        timer.cancel();
                    }
                }
            }
        } );


    }

    public class PlaybackTimerTask extends TimerTask {
        private VideoView videoView;
        public PlaybackTimerTask (VideoView videoView) {
            this.videoView = videoView;
        }
        @Override
        public void run() {
            if (hasAdBeenClicked) {
                return;
            }

            if (videoView!= null) {
                Logger.d("Danica %d", videoView.getCurrentPosition());
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
