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
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.IAdView;
import com.sharethrough.sdk.Placement;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.VideoDialog;

import java.util.Timer;

public class AutoPlayVideo extends Media {
    private final Creative creative;
    boolean isPlaying = false;

    public AutoPlayVideo(Creative creative) {
        this.creative = creative;
    }

    @Override
    public void wasClicked(View view, BeaconService beaconService, int feedPosition) {
        new VideoDialog(view.getContext(), creative, beaconService, false, new Timer(), new VideoCompletionBeaconService(view.getContext(), creative, beaconService, feedPosition), feedPosition).show();
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
        thumbnailContainer.addView(vw, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        Uri uri = Uri.parse(getCreative().getMediaUrl());
        vw.setVideoURI(uri);

        vw.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(false);
                mp.setVolume(0f, 0f);
                mp.start();
            }
        });

        vw.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                adView.setScreenListener(null);
                mp.stop();
            }
        });

        adView.setScreenListener( new IAdView.ScreenListener() {
            @Override
            public void onScreen() {
                if (!vw.isPlaying()) {
                    vw.start();
                }
            }

            @Override
            public void offScreen() {
                if (vw.isPlaying() && vw.canPause()) {
                    vw.pause();
                }
            }
        } );


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
