package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.Creative;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowMenuInflater;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18, shadows = ShareableDialogTest.MenuInflaterShadow.class)
public class ShareableDialogTest {
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void sharing() throws Exception {
        final Creative creative = mock(Creative.class);
        when(creative.getTitle()).thenReturn("Title");
        when(creative.getShareUrl()).thenReturn("http://share.me/with/friends");

        final ShareableDialog subject = new ShareableDialog(Robolectric.application, android.R.style.Theme_Black) {
            @Override
            protected Creative getCreative() {
                return creative;
            }
        };
        subject.show();

        assertThat(subject.getWindow().hasFeature(Window.FEATURE_ACTION_BAR)).isTrue();

        ArgumentCaptor<Intent> sharingIntentArgumentCapture = ArgumentCaptor.forClass(Intent.class);
        verify(MenuInflaterShadow.LATEST_SHARE_ACTION_PROVIDER).setShareIntent(sharingIntentArgumentCapture.capture());
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, creative.getTitle() + " " + creative.getShareUrl());
        assertThat(sharingIntentArgumentCapture.getValue()).isEqualTo(sharingIntent);
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
}