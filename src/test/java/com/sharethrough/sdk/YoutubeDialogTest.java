package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import com.sharethrough.test.util.Misc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class YoutubeDialogTest {
    private Creative creative;
    private YoutubeDialog subject;
    private Youtube youtube;
    private VideoView videoView;
    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("Title");
        when(creative.getDescription()).thenReturn("Description");
        when(creative.getAdvertiser()).thenReturn("Advertiser");
        when(creative.getThumbnailImage(any(Context.class))).thenReturn(mock(Bitmap.class));
        when(creative.getShareUrl()).thenReturn("http://share.me/with/friends");
        youtube = mock(Youtube.class);
        when(creative.getMedia()).thenReturn(youtube);
        videoView = mock(VideoView.class);
        executorService = mock(ExecutorService.class);
        subject = new YoutubeDialog(Robolectric.application, creative, executorService, new Provider<VideoView>() {
            @Override
            public VideoView get() {
                return videoView;
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void showsTitleAdvertiserAndDescription() throws Exception {
        ArrayList<View> foundViews = new ArrayList<View>();

        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Title", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);

        foundViews.clear();
        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Description", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);

        foundViews.clear();
        subject.getWindow().getDecorView().findViewsWithText(foundViews, "Advertiser", View.FIND_VIEWS_WITH_TEXT);
        assertThat(foundViews).hasSize(1);
    }

    @Test
    public void showsVideo() throws Exception {
        Robolectric.pauseMainLooper();

        ArgumentCaptor<Function> functionArgumentCaptor = ArgumentCaptor.forClass(Function.class);
        verify(youtube).doWithMediaUrl(eq(executorService), functionArgumentCaptor.capture());

        String rtspUrl = "rtsp://1.2.3/456.3gp";
        functionArgumentCaptor.getValue().apply(rtspUrl);

        verify(videoView).setMediaController(any(MediaController.class));
        verifyNoMoreInteractions(videoView);
        Robolectric.unPauseMainLooper();

        verify(videoView).setVideoPath(rtspUrl);
        verify(videoView).start();

        ViewGroup rootView = (ViewGroup) subject.getWindow().getDecorView().getRootView();
        assertThat(Misc.findViewOfType(VideoView.class, rootView) == videoView).isTrue();
    }

    @Test
    public void sharing() throws Exception {
        ImageView shareButton = Misc.findViewOfType(ImageView.class, (ViewGroup) subject.getWindow().getDecorView().getRootView());
        assertThat(shadowOf(shareButton).getImageResourceId()).isEqualTo(android.R.drawable.ic_menu_share);

        shareButton.performClick();
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Title http://share.me/with/friends");
        assertThat(shadowOf(Robolectric.application).getNextStartedActivity()).isEqualTo(Intent.createChooser(sharingIntent, "Share with"));
    }
}