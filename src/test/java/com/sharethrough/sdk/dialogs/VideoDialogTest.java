package com.sharethrough.sdk.dialogs;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import com.sharethrough.sdk.beacons.VideoCompletionBeaconService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Timer;
import java.util.TimerTask;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@Config(shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
public class VideoDialogTest extends TestBase {

    private VideoDialog subject;
    private Creative creative;
    private String videoUrl;
    private VideoView videoView;
    private Timer timer;
    private VideoCompletionBeaconService videoBeacons;
    private BeaconService beaconService;
    private int feedPosition;
    private int currentPosition;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        feedPosition = 5;
        currentPosition = 0;
        videoUrl = "http://ab.co/video.mp4";
        when(creative.getMediaUrl()).thenReturn(videoUrl);
        timer = mock(Timer.class);
        videoBeacons = mock(VideoCompletionBeaconService.class);
        beaconService = mock(BeaconService.class);
        subject = new VideoDialog(Robolectric.application, creative, beaconService, false, timer, videoBeacons, feedPosition, currentPosition);
        subject.show();
        videoView = findViewOfType(VideoView.class, (ViewGroup) subject.getWindow().getDecorView());
    }

    @Test
    public void isShareable() throws Exception {
        assertThat(subject).isInstanceOf(ShareableDialog.class);
    }

    @Test
    public void hasAVideoView() throws Exception {
        assertThat(videoView).isNotNull();
    }

    @Test
    public void videoViewHasCorrectContent() throws Exception {
        assertThat(shadowOf(videoView).getVideoURIString()).isEqualTo(videoUrl);
    }

    @Test
    public void videoAutoPlays() throws Exception {
        shadowOf(videoView).getOnPreparedListener().onPrepared(mock(MediaPlayer.class));
        assertThat(videoView.isPlaying()).isTrue();
    }

    @Test
    public void videoPausesAndResumesOnClick() throws Exception {
        shadowOf(videoView).getOnPreparedListener().onPrepared(mock(MediaPlayer.class));

        ((View) videoView.getParent()).performClick();
        assertThat(videoView.isPlaying()).isFalse();

        ((View) videoView.getParent()).performClick();
        assertThat(videoView.isPlaying()).isTrue();
    }

    @Test
    public void whenPausedSendsVideoViewDurationBeacon() throws Exception {
        shadowOf(videoView).getOnPreparedListener().onPrepared(mock(MediaPlayer.class));

        ((View) videoView.getParent()).performClick();
        assertThat(videoView.isPlaying()).isFalse();
        verify(beaconService).videoViewDuration(videoView.getContext(), creative, videoView.getCurrentPosition(), false, feedPosition);
    }

    @Test
    public void whenDialogDismissedSendsVideoViewDurationBeacon() throws Exception {
        MenuItem mockedMenuItem = mock(MenuItem.class);
        when(mockedMenuItem.getItemId()).thenReturn(android.R.id.home);
        subject.onMenuItemSelected(0, mockedMenuItem);

        verify(beaconService).videoViewDuration(videoView.getContext(), creative, videoView.getCurrentPosition(), false, feedPosition);
    }

    @Test
    public void playerLoopsWhenToldTo() throws Exception {
        MediaPlayer mediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnPreparedListener().onPrepared(mediaPlayer);
        verify(mediaPlayer).setLooping(false);

        subject = new VideoDialog(Robolectric.application, creative, mock(BeaconService.class), true, timer, videoBeacons, feedPosition, currentPosition);
        subject.show();

        videoView = findViewOfType(VideoView.class, (ViewGroup) subject.getWindow().getDecorView());
        shadowOf(videoView).getOnPreparedListener().onPrepared(mediaPlayer);
        verify(mediaPlayer).setLooping(true);
    }

    @Test
    public void firesVideoCompletionBeacons() throws Exception {
        shadowOf(videoView).getOnPreparedListener().onPrepared(mock(MediaPlayer.class));

        ArgumentCaptor<TimerTask> timerTaskArgumentCaptor = ArgumentCaptor.forClass(TimerTask.class);
        verify(timer).scheduleAtFixedRate(timerTaskArgumentCaptor.capture(), anyInt(), anyInt());
        timerTaskArgumentCaptor.getValue().run();

        verify(videoBeacons).timeUpdate(videoView.getCurrentPosition(), videoView.getDuration());

        subject.cancel();
        verify(timer).cancel();
    }

    @Test
    public void bringsVideoViewAndPlayButtonToFrontWhenVideoIsPrepared_butNotImmediately_toAllowVideoToStartPlayingFirst() {
        Robolectric.pauseMainLooper();
        shadowOf(videoView).getOnPreparedListener().onPrepared(mock(MediaPlayer.class));

        FrameLayout container = (FrameLayout) subject.findViewById(R.id.container);
        assertThat(container.getChildAt(0).getId()).isEqualTo(R.id.video);
        assertThat(container.getChildAt(1).getId()).isEqualTo(R.id.progress_spinner);

        Robolectric.unPauseMainLooper();

        assertThat(container.getChildAt(0).getId()).isEqualTo(R.id.progress_spinner);
        assertThat(container.getChildAt(1).getId()).isEqualTo(R.id.video);
        assertThat(container.getChildAt(2).getId()).isEqualTo(R.id.play_button);
    }
}