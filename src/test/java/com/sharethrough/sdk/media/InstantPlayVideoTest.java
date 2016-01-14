package com.sharethrough.sdk.media;

import android.media.MediaPlayer;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import com.sharethrough.sdk.*;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;
import com.sharethrough.sdk.dialogs.ShareableDialogTest;
import com.sharethrough.sdk.dialogs.VideoDialog;
import com.sharethrough.sdk.dialogs.WebViewDialogTest;
import org.junit.Before;
import org.junit.Test;
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
public class InstantPlayVideoTest extends TestBase {

    class InstantPlayVideoStub extends InstantPlayVideo {
        public InstantPlayVideoStub(Creative creative, BeaconService beaconService, VideoCompletionBeaconService videoCompletionBeaconService, int feedPosition) {
            super(creative, beaconService, videoCompletionBeaconService, feedPosition);
        }

        public SilentAutoplayBeaconTask instantiateSilentPlaybackTimerTask(VideoView videoView) {
            return new SilentAutoplayBeaconTask(videoView);
        }

        public VideoCompletionBeaconTask instantiateVideoCompletionBeaconTask(VideoView videoView) {
            return new VideoCompletionBeaconTask(videoView);
        }

        protected Timer getTimer(String timerName) {
            return mockedTimer;
        }
    }

    private InstantPlayVideoStub subject;
    private InstantPlayCreative mockedCreative;
    private BeaconService mockedBeaconService;
    private int feedPosition;
    private VideoCompletionBeaconService mockedVideoCompletionBeaconService;
    private Timer mockedTimer;

    @Before
    public void setUp() throws Exception {
        mockedCreative = mock(InstantPlayCreative.class);
        mockedBeaconService = mock(BeaconService.class);
        mockedVideoCompletionBeaconService = mock(VideoCompletionBeaconService.class);
        feedPosition = 5;
        mockedTimer = mock(Timer.class);
        when(mockedCreative.getMediaUrl()).thenReturn("http://ab.co");
        subject = new InstantPlayVideoStub(mockedCreative, mockedBeaconService, mockedVideoCompletionBeaconService, feedPosition);
    }

    @Test
    public void whenClicked_cancelSilentAutoPlayBeaconTimer() throws Exception {
        Timer mockedTimer = mock(Timer.class);
        subject.silentAutoPlayBeaconTimer = mockedTimer;
        InstantPlayVideo.SilentAutoplayBeaconTask mockedSilentAutoplayBeaconTask = mock(InstantPlayVideo.SilentAutoplayBeaconTask.class);
        subject.silentAutoplayBeaconTask = mockedSilentAutoplayBeaconTask;

        subject.wasClicked(new View(Robolectric.application), mock(BeaconService.class), feedPosition);
        verify(mockedTimer).cancel();
        verify(mockedSilentAutoplayBeaconTask).cancel();
    }

    @Test
    public void whenClicked_cancelVideoCompletionBeaconTimer() throws Exception {
        Timer mockedTimer = mock(Timer.class);
        subject.videoCompletionBeaconTimer = mockedTimer;
        InstantPlayVideo.VideoCompletionBeaconTask mockedVideoCompletionBeaconTask = mock(InstantPlayVideo.VideoCompletionBeaconTask.class);
        subject.videoCompletionBeaconTask = mockedVideoCompletionBeaconTask;

        subject.wasClicked(new View(Robolectric.application), mock(BeaconService.class), feedPosition);
        verify(mockedTimer).cancel();
        verify(mockedVideoCompletionBeaconTask).cancel();
    }

    @Test
    public void whenClick_pauseVideo() throws Exception {
        FrameLayout viewContainer = new FrameLayout(Robolectric.application);
        VideoView mockedVideoView = spy(new VideoView(Robolectric.application));
        mockedVideoView.setTag("SharethroughAutoPlayVideoView");
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.canPause()).thenReturn(true);
        doNothing().when(mockedVideoView).stopPlayback();
        when(mockedVideoView.getCurrentPosition()).thenReturn(3000);
        viewContainer.addView(mockedVideoView);
        subject.isVideoPrepared = true;

        subject.wasClicked(viewContainer, mock(BeaconService.class), feedPosition);
        verify(mockedVideoView).stopPlayback();
        assertThat(subject.isVideoPrepared).isFalse();
        assertThat(subject.isVideoPlaying).isFalse();
    }

    @Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
    @Test
    public void whenClicked_opensAutoPlayVideoDialog() throws Exception {
        subject.wasClicked(new View(Robolectric.application), mock(BeaconService.class), feedPosition);
        assertThat(ShadowDialog.getLatestDialog()).isInstanceOf(VideoDialog.class);
    }

    @Test
    public void firesAdClickedBeacon() throws Exception {
        Creative creative = mock(Creative.class);
        int feedPosition = 5;
        BeaconService beaconService = mock(BeaconService.class);

        FrameLayout viewContainer = new FrameLayout(Robolectric.application);
        VideoView mockedVideoView = spy(new VideoView(Robolectric.application));
        mockedVideoView.setTag("SharethroughAutoPlayVideoView");
        when(mockedVideoView.getCurrentPosition()).thenReturn(3000);
        viewContainer.addView(mockedVideoView);

        IAdView mockedAdView = mock(IAdView.class);
        when(mockedAdView.getAdView()).thenReturn(viewContainer);

        subject.fireAdClickBeacon(creative, mockedAdView, beaconService, feedPosition);
        verify(beaconService).adClicked("videoPlay", creative, mockedAdView.getAdView(), feedPosition);
        verify(beaconService).autoplayVideoEngagement(viewContainer.getContext(), creative, 3000, feedPosition);
    }

    @Test
    public void whenRendered_setThumbnailImageInvisible() throws Exception {
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
    public void whenRendered_setScreenVisiblityListenerOnAdView() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout mockedThumbnailContainer = mock(FrameLayout.class);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(mockedThumbnailContainer);

        subject.wasRendered(mockedIAdView, mockedImageView);
        verify(mockedIAdView).setScreenVisibilityListener(any(IAdView.ScreenVisibilityListener.class));
    }

    @Test
    public void whenRendered_videoIsPrepared() throws Exception {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);

        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedCreative.getCurrentPosition()).thenReturn(4000);
        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.isVideoPrepared = false;
        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        MediaPlayer mockedMediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnPreparedListener().onPrepared(mockedMediaPlayer);

        verify(mockedMediaPlayer).setLooping(false);
        verify(mockedMediaPlayer).setVolume(0f, 0f);
        assertThat(videoView.isPlaying()).isFalse();
        assertThat(subject.isVideoPrepared).isTrue();

    }

    @Test
    public void whenVideoCompleted_beaconTimersCancelledAndMediaPlayStopped() {
        ImageView mockedImageView = mock(ImageView.class);
        IAdView mockedIAdView = mock(BasicAdView.class);
        FrameLayout thumbnailContainer = new FrameLayout(Robolectric.application);
        when(mockedIAdView.getAdView()).thenReturn(new FrameLayout(Robolectric.application));
        when(mockedIAdView.getThumbnail()).thenReturn(thumbnailContainer);

        when(mockedCreative.isVideoCompleted()).thenReturn(false);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedCreative.getCurrentPosition()).thenReturn(4000);
        subject.wasRendered(mockedIAdView, mockedImageView);
        subject.isVideoPrepared = false;
        Timer mockedSilentAutoplayBeaconTimer = mock(Timer.class);
        subject.silentAutoPlayBeaconTimer = mockedSilentAutoplayBeaconTimer;
        Timer mockedVideoCompletionTimer = mock(Timer.class);
        subject.videoCompletionBeaconTimer = mockedVideoCompletionTimer;
        InstantPlayVideo.SilentAutoplayBeaconTask mockedSilentAutoplayBeaconTask = mock(InstantPlayVideo.SilentAutoplayBeaconTask.class);
        subject.silentAutoplayBeaconTask = mockedSilentAutoplayBeaconTask;
        InstantPlayVideo.VideoCompletionBeaconTask mockedVideoCompletionBeaconTask = mock(InstantPlayVideo.VideoCompletionBeaconTask.class);
        subject.videoCompletionBeaconTask = mockedVideoCompletionBeaconTask;

        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        MediaPlayer mockedMediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnCompletionListener().onCompletion(mockedMediaPlayer);

        verify(mockedSilentAutoplayBeaconTimer).cancel();
        verify(mockedVideoCompletionTimer).cancel();
        verify(mockedSilentAutoplayBeaconTask).cancel();
        verify(mockedVideoCompletionBeaconTask).cancel();
        verify(mockedCreative).setVideoCompleted(true);
        assertThat(videoView.isPlaying()).isFalse();
        assertThat(subject.isVideoPrepared).isFalse();

        //verify videoViewDuration beacon sent
        verify(mockedBeaconService).videoViewDuration(Robolectric.application, mockedCreative, videoView.getCurrentPosition(), true, feedPosition);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, videoView.getCurrentPosition(), feedPosition, true);
        verifyNoMoreInteractions(mockedBeaconService);
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
        subject.isVideoPrepared = true;
        Timer mockedSilentAutoplayBeaconTimer = mock(Timer.class);
        subject.silentAutoPlayBeaconTimer = mockedSilentAutoplayBeaconTimer;
        Timer mockedVideoCompletionBeaconTimer = mock(Timer.class);
        subject.videoCompletionBeaconTimer = mockedVideoCompletionBeaconTimer;
        InstantPlayVideo.SilentAutoplayBeaconTask mockedSilentAutoplayBeaconTask = mock(InstantPlayVideo.SilentAutoplayBeaconTask.class);
        subject.silentAutoplayBeaconTask = mockedSilentAutoplayBeaconTask;
        InstantPlayVideo.VideoCompletionBeaconTask mockedVideoCompletionBeaconTask = mock(InstantPlayVideo.VideoCompletionBeaconTask.class);
        subject.videoCompletionBeaconTask = mockedVideoCompletionBeaconTask;

        mockedIAdView.getScreenVisibilityListener().onScreen();
        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        assertThat(videoView.isPlaying()).isTrue();
        verify(mockedSilentAutoplayBeaconTimer).cancel();
        verify(mockedVideoCompletionBeaconTimer).cancel();
        verify(mockedVideoCompletionBeaconTask).cancel();
        verify(mockedSilentAutoplayBeaconTask).cancel();
        assertThat(subject.silentAutoPlayBeaconTimer).isNotEqualTo(mockedSilentAutoplayBeaconTimer);
        assertThat(subject.silentAutoplayBeaconTask).isNotEqualTo(null);
        assertThat(subject.videoCompletionBeaconTimer).isNotEqualTo(null);
        verify(mockedTimer).scheduleAtFixedRate(subject.silentAutoplayBeaconTask, 1000, 1000);
        verify(mockedTimer).scheduleAtFixedRate(subject.videoCompletionBeaconTask, 1000, 1000);
        assertThat(subject.isVideoPlaying).isTrue();
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
        subject.isVideoPrepared = true;
        subject.isVideoPlaying = false;

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
        subject.isVideoPrepared = true;

        Timer mockedSilentAutoplayBeaconTimer = mock(Timer.class);
        subject.silentAutoPlayBeaconTimer = mockedSilentAutoplayBeaconTimer;
        Timer mockedVideoCompletionBeaconTimer = mock(Timer.class);
        subject.videoCompletionBeaconTimer = mockedVideoCompletionBeaconTimer;
        InstantPlayVideo.SilentAutoplayBeaconTask mockedSilentAutoplayBeaconTask = mock(InstantPlayVideo.SilentAutoplayBeaconTask.class);
        subject.silentAutoplayBeaconTask = mockedSilentAutoplayBeaconTask;
        InstantPlayVideo.VideoCompletionBeaconTask mockedVideoCompletionBeaconTask = mock(InstantPlayVideo.VideoCompletionBeaconTask.class);
        subject.videoCompletionBeaconTask = mockedVideoCompletionBeaconTask;

        VideoView videoView = (VideoView)thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView");
        videoView.start();

        mockedIAdView.getScreenVisibilityListener().offScreen();

        //verify videoViewDuration beacon sent
        verify(mockedBeaconService).videoViewDuration(Robolectric.application, mockedCreative, videoView.getCurrentPosition(), true, feedPosition);
        verifyNoMoreInteractions(mockedBeaconService);

        //verify current position saved
        verify(mockedCreative).setCurrentPosition(any(Integer.class));

        //verify timers/tasks cancelled
        verify(mockedSilentAutoplayBeaconTimer).cancel();
        verify(mockedVideoCompletionBeaconTimer).cancel();
        verify(mockedSilentAutoplayBeaconTask).cancel();
        verify(mockedVideoCompletionBeaconTask).cancel();

        //verify video paused and video view removed
        assertThat(videoView.isPlaying()).isFalse();
        assertThat((VideoView) thumbnailContainer.findViewWithTag("SharethroughAutoPlayVideoView")).isNull();

        //verify video state set properly
        assertThat(subject.isVideoPrepared).isFalse();
        assertThat(subject.isVideoPlaying).isFalse();
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCreativeWasClicked_doNothing() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(true);
        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verifyNoMoreInteractions(mockedBeaconService);
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCurrentPositionIs3Seconds_fire3SecondBeacon() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(3011);
        when(mockedVideoView.getContext()).thenReturn(Robolectric.application);

        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 3000, feedPosition, false);
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCurrentPositionIs7Seconds_fire3SecondBeacon() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(7012);
        when(mockedVideoView.getDuration()).thenReturn(10000);
        when(mockedVideoView.getContext()).thenReturn(Robolectric.application);

        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 3000, feedPosition, false);
        verifyNoMoreInteractions(mockedBeaconService);
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCurrentPositionIs10Seconds_fire10SecondBeacon() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(10012);
        when(mockedVideoView.getDuration()).thenReturn(30000);
        when(mockedVideoView.getContext()).thenReturn(Robolectric.application);

        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 3000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 10000, feedPosition, false);
        verifyNoMoreInteractions(mockedBeaconService);
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCurrentPositionIs15Seconds_fire15SecondBeacon() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(15012);
        when(mockedVideoView.getDuration()).thenReturn(30000);
        when(mockedVideoView.getContext()).thenReturn(Robolectric.application);

        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 3000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 10000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 15000, feedPosition, false);
        verifyNoMoreInteractions(mockedBeaconService);
    }

    @Test
    public void whenExecutingSilentAutoplayTimerTaskAndCurrentPositionIs30Seconds_fire30SecondBeacon() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedCreative.wasClicked()).thenReturn(false);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(30012);
        when(mockedVideoView.getDuration()).thenReturn(40000);
        when(mockedVideoView.getContext()).thenReturn(Robolectric.application);

        InstantPlayVideo.SilentAutoplayBeaconTask playbackTimerTask = subject.instantiateSilentPlaybackTimerTask(mockedVideoView);
        playbackTimerTask.run();
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 3000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 10000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 15000, feedPosition, false);
        verify(mockedBeaconService).silentAutoPlayDuration(Robolectric.application, mockedCreative, 30000, feedPosition, false);
        verifyNoMoreInteractions(mockedBeaconService);
    }


    @Test
    public void whenExecutingVideoCompletionBeacon_callsTimeUpdate() {
        VideoView mockedVideoView = mock(VideoView.class);
        when(mockedVideoView.isPlaying()).thenReturn(true);
        when(mockedVideoView.getCurrentPosition()).thenReturn(1234);
        when(mockedVideoView.getDuration()).thenReturn(20000);
        InstantPlayVideo.VideoCompletionBeaconTask videoCompletionBeaconTask = subject.instantiateVideoCompletionBeaconTask(mockedVideoView);
        videoCompletionBeaconTask.run();
        verify(mockedVideoCompletionBeaconService).timeUpdate(1234, 20000);
    }
}