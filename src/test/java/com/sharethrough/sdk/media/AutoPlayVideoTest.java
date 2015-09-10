package com.sharethrough.sdk.media;

import android.media.MediaPlayer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.robolectric.Robolectric.shadowOf;

import java.util.Timer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by dshei on 9/7/15.
 */
public class AutoPlayVideoTest extends TestBase {
    @Mock
    private Placement placement;

    class AutoPlayVideoStub extends AutoPlayVideo {
        public AutoPlayVideoStub (Creative creative) {
            super(creative);
        }

        public void setTimer(Timer timer) {
            this.silentAutoPlayBeaconTimer = timer;
        }

        public void setTask(PlaybackTimerTask task) {
            this.silentAutoPlayBeaconTask = task;
        }

        public void setIsVideoPrepared(boolean isVideoPrepared) {
            this.isVideoPrepared = isVideoPrepared;
        }

        public void setVideoPrepared(boolean isVideoPrepared) {
            this.isVideoPrepared = isVideoPrepared;
        }

        public boolean isVideoPrepared() {
            return isVideoPrepared;
        }

    }

    private AutoPlayVideoStub subject;
    private VideoCreative mockedCreative;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        mockedCreative = mock(VideoCreative.class);
        when(mockedCreative.getMediaUrl()).thenReturn("http://ab.co");
        subject = new AutoPlayVideoStub(mockedCreative);
        feedPosition = 5;
    }

    @Test
    public void whenClicked_cancelSilentAutoPlayBeaconTimer() throws Exception {
        Timer mockedTimer = mock(Timer.class);
        subject.setTimer(mockedTimer);

        subject.wasClicked(new View(Robolectric.application), mock(BeaconService.class), feedPosition);
        verify(mockedTimer).cancel();
    }

    @Test
    public void whenClick_pauseVideo() throws Exception {
        FrameLayout viewContainer = new FrameLayout(Robolectric.application);
        VideoView mockedVideoView = spy(new VideoView(Robolectric.application));
        mockedVideoView.setTag("SharethroughAutoPlayVideoView");
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.canPause()).thenReturn(true);
        doNothing().when(mockedVideoView).pause();
        when(mockedVideoView.getCurrentPosition()).thenReturn(3000);
        viewContainer.addView(mockedVideoView);

        subject.wasClicked(viewContainer, mock(BeaconService.class), feedPosition);
        verify(mockedVideoView).pause();
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void whenClicked_opensAutoPlayVideoDialog() throws Exception {
        subject.wasClicked(new View(Robolectric.application), mock(BeaconService.class), feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(VideoDialog.class);
    }

    @Test
    public void whenSwapMedia_setThumbnailImageInvisible() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(new FrameLayout(Robolectric.application));

        subject.wasRendered(mockedIAdView, mockedImageView);
        verify(mockedImageView).setVisibility(View.INVISIBLE);
    }

    /*@Test
    public void whenSwapMedia_insertVideoViewIntoThumbnailContainer() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout mockedThumbnailContainer = mock(FrameLayout.class);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(mockedThumbnailContainer);

        subject.swapMedia(mockedIAdView, mockedImageView);
        verify(mockedThumbnailContainer).addView(any(VideoView.class), any(FrameLayout.LayoutParams.class));
    }*/

    @Test
    public void whenSwapMedia_setScreenVisiblityListenerOnAdView() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout mockedThumbnailContainer = mock(FrameLayout.class);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(mockedThumbnailContainer);

        subject.wasRendered(mockedIAdView, mockedImageView);
        verify(mockedIAdView).setScreenVisibilityListener(any(IAdView.ScreenVisibilityListener.class));
    }

    @Test
    public void whenSwapMedia_videoAutoPlays() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);

        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedCreative.getCurrentPosition()).thenReturn(4000);
        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.setIsVideoPrepared(false);
        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        MediaPlayer mockedMediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnPreparedListener().onPrepared(mockedMediaPlayer);

        verify(mockedMediaPlayer).setLooping(false);
        verify(mockedMediaPlayer).setVolume(0f, 0f);
        verify(mockedMediaPlayer).seekTo(4000);
        verify(mockedMediaPlayer).start();
        assertThat(subject.isVideoPrepared()).isTrue();

    }

    @Test
    public void whenVideoCompleted_silentAutoPlayBeaconTimerCancelledAndMediaPlayStopped() {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);

        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedCreative.getCurrentPosition()).thenReturn(4000);
        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.setIsVideoPrepared(false);
        Timer mockedTimer = mock(Timer.class);
        subject.setTimer(mockedTimer);

        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        MediaPlayer mockedMediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnCompletionListener().onCompletion(mockedMediaPlayer);

        verify(mockedTimer).cancel();
        verify(mockedCreative).setVideoCompleted(true);
        verify(mockedMediaPlayer).stop();
    }

    @Test
    public void whenVideoOnScreenBeforeClicked_videoPlays() {
        ImageView mockedImageView = mock(ImageView.class);

        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);

        IAdView mockedIAdView = spy(new BasicAdView(Robolectric.application));
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);
        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(false);

        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.setIsVideoPrepared(true);

        mockedIAdView.getScreenVisibilityListener().onScreen();
        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        assertThat(videoView.isPlaying()).isTrue();
    }

    @Test
    public void whenVideoOnScreenAfterClicked_videoPaused() {
        ImageView mockedImageView = mock(ImageView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);

        IAdView mockedIAdView = spy(new BasicAdView(Robolectric.application));
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);
        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(true);

        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.setIsVideoPrepared(true);

        mockedIAdView.getScreenVisibilityListener().onScreen();
        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        assertThat(videoView.isPlaying()).isFalse();
    }

    @Test
    public void whenVideoOffScreen_videoPauses() {
        ImageView mockedImageView = mock(ImageView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);

        IAdView mockedIAdView = spy(new BasicAdView(Robolectric.application));
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);

        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.setIsVideoPrepared(true);

        Timer mockedTimer = mock(Timer.class);
        subject.setTimer(mockedTimer);

        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        videoView.start();
        mockedIAdView.getScreenVisibilityListener().offScreen();
        assertThat(videoView.isPlaying()).isFalse();
        verify(mockedTimer).cancel();
        verify(mockedCreative).setCurrentPosition(any(Integer.class));
    }
}