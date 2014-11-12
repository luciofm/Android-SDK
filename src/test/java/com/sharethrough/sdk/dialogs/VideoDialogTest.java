package com.sharethrough.sdk.dialogs;

import android.media.MediaPlayer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.sharethrough.test.util.Misc.findViewOfType;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, shadows = {WebViewDialogTest.WebViewShadow.class, ShareableDialogTest.MenuInflaterShadow.class})
public class VideoDialogTest {

    private VideoDialog subject;
    private Creative creative;
    private String videoUrl;
    private VideoView videoView;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        videoUrl = "http://ab.co/video.mp4";
        when(creative.getMediaUrl()).thenReturn(videoUrl);
        subject = new VideoDialog(Robolectric.application, creative, mock(BeaconService.class), false);
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
    public void playerLoopsWhenToldTo() throws Exception {
        MediaPlayer mediaPlayer = mock(MediaPlayer.class);
        shadowOf(videoView).getOnPreparedListener().onPrepared(mediaPlayer);
        verify(mediaPlayer).setLooping(false);

        subject = new VideoDialog(Robolectric.application, creative, mock(BeaconService.class), true);
        subject.show();

        videoView = findViewOfType(VideoView.class, (ViewGroup) subject.getWindow().getDecorView());
        shadowOf(videoView).getOnPreparedListener().onPrepared(mediaPlayer);
        verify(mediaPlayer).setLooping(true);
    }
}