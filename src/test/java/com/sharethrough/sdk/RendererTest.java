package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import org.fest.assertions.api.ANDROID;
import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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
public class RendererTest extends TestBase {

    private Renderer subject;
    private Creative creative;
    private Bitmap bitmap;
    private Media media;
    private MyTestAdView adView;
    private Timer timer;
    private BeaconService beaconService;
    private Sharethrough sharethrough;
    private int feedPosition;
    @Mock private Placement placement;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("title");
        when(creative.getDescription()).thenReturn("description");
        when(creative.getAdvertiser()).thenReturn("advertiser");
        when(creative.getTitle()).thenReturn("title");
        bitmap = mock(Bitmap.class);
        when(creative.makeThumbnailImage()).thenReturn(bitmap);
        when(creative.makeThumbnailImage(anyInt(), anyInt())).thenReturn(bitmap);
        media = mock(Media.class);
        when(media.getCreative()).thenReturn(creative);
        when(creative.getMedia()).thenReturn(media);
        feedPosition = 0;

        beaconService = mock(BeaconService.class);

        adView = makeAdView();
        timer = mock(Timer.class);
        sharethrough = mock(Sharethrough.class);
        sharethrough.placement = placement;

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
        verify(beaconService).adReceived(any(Context.class), eq(creative), eq(feedPosition), eq(placement));
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void whenAdIsClicked_firesMediaBeacon_andMediaClickListener() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        adView.performClick();

        verify(media).wasClicked(adView, beaconService, feedPosition);
        verify(media).fireAdClickBeaconOnFirstClick(creative, adView, beaconService, feedPosition, placement);
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

    @Test
    public void whenCreativeHasBrandLogo_andAdViewHasImageViewForIt_BrandLogoIsDisplayed() {
        ImageView brandLogo = adView.getBrandLogo();
        when(creative.makeBrandLogo()).thenReturn(bitmap);
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        assertThat(brandLogo.getVisibility()).isEqualTo(View.VISIBLE);

        BitmapDrawable bitmapDrawable = (BitmapDrawable) brandLogo.getDrawable();
        assertThat(bitmapDrawable.getBitmap()).isEqualTo(bitmap);
    }

    @Test
    public void whenCreativeDoesNotHaveBrandLogo_andAdViewHasImageViewForIt_BrandLogoIsNotDisplayed() {
        when(creative.makeBrandLogo()).thenReturn(null);
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View brandLogo = adView.getBrandLogo();
        assertThat(brandLogo.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void whenCreativeHasBrandLogo_andAdViewDoesNotHasImageViewForIt_NothingBadHappens() {
        when(creative.makeBrandLogo()).thenReturn(bitmap);
        adView.brandLogo = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();
    }

    @Test
    public void whenBrandLogoIsNull_NothingBadHappens() throws Exception {
        adView.brandLogo = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void onceAdIsReady_showsProportionalOptoutButton_thatLinksToPrivacyInformation() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View optout = adView.getOptout();
        assertThat(optout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(optout.getMinimumHeight()).isEqualTo(20);
        assertThat(optout.getMinimumWidth()).isEqualTo(20);
        optout.performClick();
        ANDROID.assertThat(shadowOf(Robolectric.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse(Sharethrough.PRIVACY_POLICY_ENDPOINT)));
    }

    public static MyTestAdView makeAdView() {
        return new MyTestAdView(Robolectric.application);
    }

    public static class MyTestAdView extends TestAdView {
        FrameLayout thumbnail = mock(FrameLayout.class);
        TextView advertiser = mock(TextView.class);
        TextView description = mock(TextView.class);
        TextView title = mock(TextView.class);
        ImageView brandLogo = new ImageView(Robolectric.application);
        ImageView optout = new ImageView(Robolectric.application);
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

        @Override
        public ImageView getBrandLogo() {
            return brandLogo;
        }

        @Override
        public ImageView getOptout() {
            return optout;
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