package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.sharethrough.sdk.media.*;
import com.sharethrough.sdk.test.SharethroughTestRunner;
import com.sharethrough.test.util.TestAdView;
import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowView;

import java.util.Timer;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
@RunWith(SharethroughTestRunner.class)
public class RendererTest extends TestBase {

    private RendererStub subject;
    private Creative creative;
    private MyTestAdView adView;
    private Timer timer;
    private BeaconService beaconService;
    private Sharethrough sharethrough;
    private int feedPosition;
    @Mock private Placement placement;
    @Mock private Media media;

    public class RendererStub extends Renderer {
        protected Media createMedia(IAdView adView, Creative creative, BeaconService beaconService, int feedPosition) {
            return media;
        }
    }

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("title");
        when(creative.getDescription()).thenReturn("description");
        when(creative.getAdvertiser()).thenReturn("advertiser");
        when(creative.getTitle()).thenReturn("title");
        when(creative.getBrandLogoUrl()).thenReturn("logoBrandUrl");
        when(creative.getThumbnailUrl()).thenReturn("fake_image.jpg");
        when(media.getCreative()).thenReturn(creative);
        feedPosition = 0;

        beaconService = mock(BeaconService.class);
        adView = makeAdView();
        timer = mock(Timer.class);
        sharethrough = mock(Sharethrough.class);
        sharethrough.placement = placement;

        subject = new RendererStub();
    }

    @Test
    public void onUIthread_callsCallback_andShowsTitleDescriptionAdvertiserAndThumbnailWithOverlay() throws Exception {
        ShadowLooper.pauseMainLooper();
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        verifyNoMoreInteractions(adView.getTitle());
        assertThat(adView.adReady_wasCalled).isFalse();

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();

        assertThat(adView.adReady_wasCalled).isTrue();

        verify(adView.getTitle()).setText("title");
        verify(adView.getDescription()).setText("description");
        verify(adView.getAdvertiser()).setText("advertiser");
        ArgumentCaptor<View> thumbnailViewCaptor = ArgumentCaptor.forClass(View.class);
        verify(adView.getThumbnail(), atLeastOnce()).addView(thumbnailViewCaptor.capture(), any(FrameLayout.LayoutParams.class));

        ImageView thumbnailImageView = (ImageView) thumbnailViewCaptor.getAllValues().get(0);

        verifyNoMoreInteractions(media);
        shadowLooper.runOneTask();
        verify(media).wasRendered(adView, thumbnailImageView);
    }

    @Test
    public void firesImpressionBeaconOnlyOnce() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
        verify(beaconService).adReceived(any(Context.class), eq(creative), eq(feedPosition));
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
        verifyNoMoreInteractions(beaconService);
    }

    @Test
    public void whenAdIsClicked_firesMediaBeacon_andMediaClickListener() throws Exception {
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        adView.performClick();

        verify(media).wasClicked(adView, beaconService, feedPosition);
        verify(media).fireAdClickBeaconOnFirstClick(creative, adView, beaconService, feedPosition);
    }

    @Test
    public void whenDescriptionIsNull_NothingBadHappens() throws Exception {
        adView.description = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);
    }

    @Test
    public void whenHandlerPosts_ifOriginalAdViewHasBeenRecycled_doesNothing() throws Exception {
        ShadowLooper.pauseMainLooper();

        Creative creative1 = mock(Creative.class);
        subject.putCreativeIntoAdView(adView, creative1, beaconService, sharethrough, timer);
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        verifyNoMoreInteractions(creative1);
        verifyNoMoreInteractions(creative);

        ShadowLooper.unPauseMainLooper();

        verifyNoMoreInteractions(creative1);
        verify(creative).getTitle();
    }

    @Test
    public void whenCreativeHasBrandLogo_andAdViewHasImageViewForIt_BrandLogoIsDisplayed() {
        ImageView brandLogo = adView.getBrandLogo();
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        assertThat(brandLogo.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void whenCreativeDoesNotHaveBrandLogo_andAdViewHasImageViewForIt_BrandLogoIsNotDisplayed() {
        when(creative.getBrandLogoUrl()).thenReturn("");
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View brandLogo = adView.getBrandLogo();
        assertThat(brandLogo.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void whenCreativeHasBrandLogo_andAdViewDoesNotHasImageViewForIt_NothingBadHappens() {
        adView.brandLogo = null;
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
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

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View optout = adView.getOptout();
        assertThat(optout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(optout.getMinimumHeight()).isEqualTo(20);
        assertThat(optout.getMinimumWidth()).isEqualTo(20);
        optout.performClick();
        String expectedString = "http://platform-cdn.sharethrough.com/privacy-policy.html";
        ANDROID.assertThat(Shadows.shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse(expectedString)));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void whenOptoutInfoIsProvided_addsOptOutParamsToUrl() throws Exception {
        when(creative.getOptOutText()).thenReturn("New Opt Out Text");
        when(creative.getOptOutUrl()).thenReturn("http://example.com");
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View optout = adView.getOptout();
        assertThat(optout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(optout.getMinimumHeight()).isEqualTo(20);
        assertThat(optout.getMinimumWidth()).isEqualTo(20);
        optout.performClick();
        String expectedString = "http://platform-cdn.sharethrough.com/privacy-policy.html?opt_out_url=http%3A%2F%2Fexample.com&opt_out_text=New%20Opt%20Out%20Text";
        ANDROID.assertThat(Shadows.shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse(expectedString)));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void whenOptoutMissingText_BlankOptoutParam() throws Exception {
        when(creative.getOptOutText()).thenReturn("");
        when(creative.getOptOutUrl()).thenReturn("http://example.com");
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View optout = adView.getOptout();
        assertThat(optout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(optout.getMinimumHeight()).isEqualTo(20);
        assertThat(optout.getMinimumWidth()).isEqualTo(20);
        optout.performClick();
        String expectedString = "http://platform-cdn.sharethrough.com/privacy-policy.html";
        ANDROID.assertThat(Shadows.shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse(expectedString)));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void whenOptoutMissingUrl_BlankOptoutParam() throws Exception {
        when(creative.getOptOutText()).thenReturn("OptOutText");
        when(creative.getOptOutUrl()).thenReturn("");
        subject.putCreativeIntoAdView(adView, creative, beaconService, sharethrough, timer);

        ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
        shadowLooper.runOneTask();
        shadowLooper.runOneTask();

        View optout = adView.getOptout();
        assertThat(optout.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(optout.getMinimumHeight()).isEqualTo(20);
        assertThat(optout.getMinimumWidth()).isEqualTo(20);
        optout.performClick();
        String expectedString = "http://platform-cdn.sharethrough.com/privacy-policy.html";
        ANDROID.assertThat(Shadows.shadowOf(RuntimeEnvironment.application).getNextStartedActivity()).isEqualTo(new Intent(Intent.ACTION_VIEW, Uri.parse(expectedString)));
    }



    public static MyTestAdView makeAdView() {
        return new MyTestAdView(RuntimeEnvironment.application);
    }

    public static class MyTestAdView extends TestAdView {
        FrameLayout thumbnail = mock(FrameLayout.class);
        TextView advertiser = mock(TextView.class);
        TextView description = mock(TextView.class);
        TextView title = mock(TextView.class);
        ImageView brandLogo = new ImageView(RuntimeEnvironment.application);
        ImageView optout = new ImageView(RuntimeEnvironment.application);
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

        @Override
        public ScreenVisibilityListener getScreenVisibilityListener() {
            return null;
        }

        @Override
        public void setScreenVisibilityListener(ScreenVisibilityListener screenListener) {

        }

        @Override
        public void onScreen() {

        }

        @Override
        public void offScreen() {

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

    @Test
    public void getType_Youtube() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.YOUTUBE);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof Youtube).isTrue();
    }

    @Test
    public void getType_Vine() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.VINE);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof Vine).isTrue();
    }

    @Test
    public void getType_HostedVideo() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.HOSTEDVIDEO);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof HostedVideo).isTrue();
    }

    @Test
    public void getType_Instagram() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.INSTAGRAM);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof Instagram).isTrue();
    }

    @Test
    public void getType_Pinterest() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.PINTEREST);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof Pinterest).isTrue();
    }

    @Test
    public void getType_Clickout() throws Exception {
        IAdView mockedAdView = mock(IAdView.class);
        Renderer subject = new Renderer();
        when(creative.getType()).thenReturn(Creative.CreativeType.CLICKOUT);
        assertThat(subject.createMedia(mockedAdView, creative, beaconService, feedPosition) instanceof Clickout).isTrue();
    }
}