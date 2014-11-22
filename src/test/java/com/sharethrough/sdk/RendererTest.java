package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.sdk.media.Media;
import com.sharethrough.sdk.test.SharethroughTestRunner;
import com.sharethrough.test.util.TestAdView;
import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowView;

import java.util.List;
import java.util.Timer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
@RunWith(SharethroughTestRunner.class)
public class RendererTest {

    private Renderer subject;
    private Creative creative;
    private Bitmap bitmap;
    private Media media;
    private MyTestAdView adView;
    private Timer timer;
    private BeaconService beaconService;
    private Sharethrough sharethrough;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("title");
        when(creative.getDescription()).thenReturn("description");
        when(creative.getAdvertiser()).thenReturn("advertiser");
        when(creative.getTitle()).thenReturn("title");
        bitmap = mock(Bitmap.class);
        when(creative.makeThumbnailImage()).thenReturn(bitmap);
        media = mock(Media.class);
        when(media.getCreative()).thenReturn(creative);
        when(creative.getMedia()).thenReturn(media);

        beaconService = mock(BeaconService.class);

        adView = makeAdView();
        timer = mock(Timer.class);
        sharethrough = mock(Sharethrough.class);

        subject = new Renderer();
    }

    @Test
    public void onUIthread_callsCallback_andShowsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        Robolectric.pauseMainLooper();

        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        verifyNoMoreInteractions(adView.getTitle());
        assertThat(adView.adReady_wasCalled).isFalse();

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();

        assertThat(adView.adReady_wasCalled).isTrue();

        verify(adView.getTitle()).setText("title");
        verify(adView.getDescription()).setText("description");
        verify(adView.getAdvertiser()).setText("advertiser");
        ArgumentCaptor<View> thumbnailViewCaptor = ArgumentCaptor.forClass(View.class);
        verify(adView.getThumbnail(), atLeastOnce()).addView(thumbnailViewCaptor.capture(), any(FrameLayout.LayoutParams.class));

        ImageView thumbnailImageView = (ImageView) thumbnailViewCaptor.getAllValues().get(0);
        BitmapDrawable bitmapDrawable = (BitmapDrawable) thumbnailImageView.getDrawable();
        assertThat(bitmapDrawable.getBitmap()).isEqualTo(bitmap);

        verifyNoMoreInteractions(media);
        shadowLooper.runOneTask();
        verify(media).overlayThumbnail(adView, thumbnailImageView);
    }

    @Test
    public void firesImpressionBeaconOnlyOnce() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
        verify(beaconService).adReceived(any(Context.class), eq(creative));
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    @Config(shadows = {MyImageViewShadow.class, MyViewShadow.class})
    public void usesAdViewTimerTask() throws Exception {
        final List<View> addedChildren = Lists.newArrayList();

        final FrameLayout mockThumbnail = adView.getThumbnail();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                View viewBeingAdded = (View) invocationOnMock.getArguments()[0];
                addedChildren.add(viewBeingAdded);

                if (viewBeingAdded instanceof ImageView) {
                    MyImageViewShadow myImageViewShadow = (MyImageViewShadow) shadowOf(viewBeingAdded);
                    if (myImageViewShadow.onAttachStateListener != null) {
                        myImageViewShadow.onAttachStateListener.onViewAttachedToWindow(viewBeingAdded);
                    }
                }
                return null;
            }
        }).when(mockThumbnail).addView(any(View.class), any(FrameLayout.LayoutParams.class));

        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ArgumentCaptor<AdViewTimerTask> timerTaskArgumentCaptor = ArgumentCaptor.forClass(AdViewTimerTask.class);
        verify(timer).schedule(timerTaskArgumentCaptor.capture(), anyLong(), anyLong());
        AdViewTimerTask timerTask = timerTaskArgumentCaptor.getValue();
        assertThat(timerTask.getAdView()).isSameAs(adView);

        assertThat(timerTask.isCancelled()).isFalse();
        for (View addedChild : addedChildren) {
            if (addedChild instanceof ImageView) {
                MyImageViewShadow myImageViewShadow = (MyImageViewShadow) shadowOf(addedChild);
                if (myImageViewShadow.onAttachStateListener != null) {
                    myImageViewShadow.onAttachStateListener.onViewDetachedFromWindow(addedChild);
                    addedChild.removeOnAttachStateChangeListener(myImageViewShadow.onAttachStateListener);
                }
            }
        }
        assertThat(timerTask.isCancelled()).isTrue();
        verify(timer).cancel();
        verify(timer).purge();
        for (View addedChild : addedChildren) {
            if (addedChild instanceof ImageView) {
                MyImageViewShadow myImageViewShadow = (MyImageViewShadow) shadowOf(addedChild);
                assertThat(myImageViewShadow.onAttachStateListener).isNull();
            }
        }
    }

    @Test
    public void whenAdIsClicked_firesMediaBeacon_andMediaClickListener() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        adView.performClick();

        verify(media).wasClicked(adView, beaconService);
        verify(media).fireAdClickBeacon(creative, adView, beaconService);
    }

    @Test
    public void whenDescriptionIsNull_NothingBadHappens() throws Exception {
        adView.description = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
    }

    @Test
    public void whenHandlerPosts_ifOriginalAdViewHasBeenRecycled_doesNothing() throws Exception {
        Robolectric.pauseMainLooper();

        Creative creative1 = mock(Creative.class);
        subject.putCreativeIntoAdView(adView, creative1, beaconService, sharethrough, timer);
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        verifyNoMoreInteractions(creative1);
        verifyNoMoreInteractions(creative);

        Robolectric.unPauseMainLooper();

        verifyNoMoreInteractions(creative1);
        verify(creative).getTitle();
    }

    public static MyTestAdView makeAdView() {
        return new MyTestAdView(Robolectric.application);
    }

    public static class MyTestAdView extends TestAdView {
        FrameLayout thumbnail = mock(FrameLayout.class);
        TextView advertiser = mock(TextView.class);
        TextView description = mock(TextView.class);
        TextView title = mock(TextView.class);
        public boolean adReady_wasCalled;

        public MyTestAdView(Context context) {
            super(context);
        }

        public MyTestAdView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyTestAdView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void adReady() {
            adReady_wasCalled = true;
        }

        @Override
        public TextView getTitle() {
            return title;
        }

        @Override
        public TextView getDescription() {
            return description;
        }

        @Override
        public TextView getAdvertiser() {
            return advertiser;
        }

        @Override
        public FrameLayout getThumbnail() {
            return thumbnail;
        }
    }

    @Implements(View.class)
    public static class MyViewShadow extends ShadowView {
        public View.OnAttachStateChangeListener onAttachStateListener;

        @Implementation
        public void addOnAttachStateChangeListener(View.OnAttachStateChangeListener listener) {
            onAttachStateListener = listener;
        }

        @Implementation
        public void removeOnAttachStateChangeListener(View.OnAttachStateChangeListener listener) {
            if (listener == onAttachStateListener) onAttachStateListener = null;
        }
    }

    @Implements(ImageView.class)
    public static class MyImageViewShadow extends MyViewShadow {

    }
}