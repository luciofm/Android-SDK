package com.sharethrough.sdk;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.concurrent.ExecutorService;

public class YoutubeDialog extends Dialog {
    public YoutubeDialog(Context context, final Creative creative, ExecutorService executorService, Provider<VideoView> videoViewProvider) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(linearLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView shareButton = new ImageView(context);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, creative.getTitle() + " " + creative.getShareUrl());
                v.getContext().startActivity(Intent.createChooser(sharingIntent, "Share with"));
            }
        });
        shareButton.setImageResource(android.R.drawable.ic_menu_share);
        linearLayout.addView(shareButton);

        TextView title = new TextView(context);
        title.setText(creative.getTitle());
        linearLayout.addView(title);

        TextView description = new TextView(context);
        description.setText(creative.getDescription());
        linearLayout.addView(description);

        TextView advertiser = new TextView(context);
        advertiser.setText(creative.getAdvertiser());
        linearLayout.addView(advertiser);

        final VideoView videoView = videoViewProvider.get();
        videoView.setMediaController(new MediaController(context));
        creative.getMedia().doWithMediaUrl(executorService, new Function<String, Void>() {
            @Override
            public Void apply(final String rtspUrl) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        videoView.setVideoPath(rtspUrl);
                        videoView.start();
                        linearLayout.addView(videoView);
                    }
                });
                return null;
            }
        });
    }
}
