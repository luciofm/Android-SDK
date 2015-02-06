package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;
import com.sharethrough.sdk.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowContext;
import org.robolectric.shadows.ShadowMenuInflater;
import org.robolectric.tester.android.view.TestMenuItem;
import org.robolectric.util.ActivityController;

import java.util.Arrays;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.robolectric.Robolectric.shadowOf;

@Config(shadows = ShareableDialogTest.MenuInflaterShadow.class)
public class ShareableDialogTest extends TestBase {

    private ShareableDialog subject;
    private Creative creative;
    private BeaconService beaconService;
    private ActivityController<Activity> activityController;
    private int feedPosition;

    @Before
    public void setUp() throws Exception {
        creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("Title");
        when(creative.getShareUrl()).thenReturn("http://share.me/with/friends");
        feedPosition = 5;

        beaconService = mock(BeaconService.class);
        subject = new ShareableDialog(Robolectric.application, android.R.style.Theme_Black, beaconService, feedPosition) {
            @Override
            protected Creative getCreative() {
                return creative;
            }
        };
        subject.show();
        activityController = Robolectric.buildActivity(Activity.class).create().start().resume().visible();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void sharing() throws Exception {
        assertThat(subject.getWindow().hasFeature(Window.FEATURE_ACTION_BAR)).isTrue();

        ArgumentCaptor<Intent> sharingIntentArgumentCapture = ArgumentCaptor.forClass(Intent.class);
        verify(MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER).setShareIntent(sharingIntentArgumentCapture.capture());
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, creative.getTitle() + " " + creative.getShareUrl());
        assertThat(sharingIntentArgumentCapture.getValue()).isEqualTo(sharingIntent);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenGmailSelected_sendsEmailBeacon() throws Exception {
        setUpPackages();
        shareVia("com.google.android.gm/com.google.android.gm.ComposeActivityGmail");
        verify(beaconService).adShared(any(Context.class), eq(creative), eq("email"), eq(feedPosition));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenEmailSelected_sendsEmailBeacon() throws Exception {
        setUpPackages();
        shareVia("com.foo.email/com.foo.email.Whatever");
        verify(beaconService).adShared(any(Context.class), eq(creative), eq("email"), eq(feedPosition));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenFacebookSelected_sendsFacebookBeacon() throws Exception {
        shareVia("com.facebook.katana/com.facebook.katana.Whatever");
        verify(beaconService).adShared(any(Context.class), eq(creative), eq("facebook"), eq(feedPosition));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenTwitterSelected_sendsTwitterBeacon() throws Exception {
        shareVia("com.twitter.android/com.twitter.android.Whatever");
        verify(beaconService).adShared(any(Context.class), eq(creative), eq("twitter"), eq(feedPosition));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenOtherSelected_sendsPackageNameInBeacon() throws Exception {
        shareVia("com.something.else/com.something.else.Whatever");
        verify(beaconService).adShared(any(Context.class), eq(creative), eq("com.something.else"), eq(feedPosition));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void whenCreativeHasCustomEngagement_displaysButtonInActionBar() { //TODO implement this test
        when(creative.getCustomEngagementUrl()).thenReturn("//custom.url");
        when(creative.getCustomEngagementLabel()).thenReturn("custom_label");

        subject = new ShareableDialog(Robolectric.application, android.R.style.Theme_Black, beaconService, feedPosition) {
            @Override
            protected Creative getCreative() {
                return creative;
            }
        };
        subject.show();

        //get reference either to Menu, or the menuItem
        //Check visibility
        //Check title
    }

    @Test
    public void whenCustomEngagmentSelected_IntentIsFiredWithCustomUrlData() {
        when(creative.getCustomEngagementUrl()).thenReturn("//custom.url");
        when(creative.getCustomEngagementLabel()).thenReturn("custom_label");

        MenuItem item = new TestMenuItem(R.id.menu_item_custom);
        subject.onMenuItemSelected(0, item);

        ShadowContext shadowContext = shadowOf(subject.getContext());
        Intent startedIntent = shadowContext.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent.getData().toString()).isEqualTo("//custom.url");
    }

    @Implements(MenuInflater.class)
    public static class MenuInflaterShadow extends ShadowMenuInflater {
        public static ShareActionProvider LATEST_SHARE_ACTION_PROVIDER;

        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        @Implementation
        @Override
        public void inflate(int resource, Menu menu) {
            super.inflate(resource, menu);

            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.menu_item_share) {
                    LATEST_SHARE_ACTION_PROVIDER = mock(ShareActionProvider.class);
                    item.setActionProvider(LATEST_SHARE_ACTION_PROVIDER);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void shareVia(String componentFlattenedString) {
        ArgumentCaptor<ShareActionProvider.OnShareTargetSelectedListener> shareTargetSelectedListenerArgumentCaptor = ArgumentCaptor.forClass(ShareActionProvider.OnShareTargetSelectedListener.class);
        verify(MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER).setOnShareTargetSelectedListener(shareTargetSelectedListenerArgumentCaptor.capture());
        shareTargetSelectedListenerArgumentCaptor.getValue().onShareTargetSelected(MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER, new Intent(Intent.ACTION_SEND).setComponent(ComponentName.unflattenFromString(componentFlattenedString)));
    }

    private void setUpPackages() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager) Robolectric.application.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_SEND).setType("message/rfc822");

        ResolveInfo email = new ResolveInfo();
        email.activityInfo = new ActivityInfo();
        email.activityInfo.packageName = "com.foo.email";
        ResolveInfo gmail = new ResolveInfo();
        gmail.activityInfo = new ActivityInfo();
        gmail.activityInfo.packageName = "com.google.android.gm";

        packageManager.addResolveInfoForIntent(intent, Arrays.asList(email, gmail));
    }
}