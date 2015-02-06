package com.sharethrough.sdk.dialogs;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ShareActionProvider;
import com.sharethrough.android.sdk.R;
import com.sharethrough.sdk.BeaconService;
import com.sharethrough.sdk.Creative;

import java.util.List;

public abstract class ShareableDialog extends Dialog {
    protected final BeaconService beaconService;
    protected final int feedPosition;

    public ShareableDialog(Context context, int theme, BeaconService beaconService, int feedPosition) {
        super(context, theme);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        this.beaconService = beaconService;
        this.feedPosition = feedPosition;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        new MenuInflater(getContext()).inflate(R.menu.share_menu, menu);

        Creative creative = getCreative();

        final String label = creative.getCustomEngagementLabel();
        final String url = creative.getCustomEngagementUrl();

        if (label != null && !label.isEmpty() && url != null && !url.isEmpty()) {
            MenuItem custom = menu.findItem(R.id.menu_item_custom);
            custom.setTitle(label);
            custom.setVisible(true);
        }


        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        ShareActionProvider shareActionProvider = (ShareActionProvider) item.getActionProvider();
        shareActionProvider.setShareIntent(new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getCreative().getTitle() + " " + getCreative().getShareUrl()));
        shareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                String packageName = "" + intent.getComponent().getPackageName();

                PackageManager pkgManager = getContext().getPackageManager();
                List<ResolveInfo> resolveInfos = pkgManager.queryIntentActivities(new Intent(Intent.ACTION_SEND).setType("message/rfc822"), 0);
                for (ResolveInfo resolveInfo : resolveInfos) {
                    if (packageName.equals(resolveInfo.activityInfo.packageName)) {
                        beaconService.adShared(getContext(), getCreative(), "email", feedPosition);
                        return false;
                    }
                }

                if (packageName.startsWith("com.twitter")) {
                    beaconService.adShared(getContext(), getCreative(), "twitter", feedPosition);
                } else if (packageName.startsWith("com.facebook")) {
                    beaconService.adShared(getContext(), getCreative(), "facebook", feedPosition);
                } else {
                    beaconService.adShared(getContext(), getCreative(), packageName, feedPosition);
                }

                return false;
            }
        });

        // Return true to display menu
        return super.onCreateOptionsMenu(menu);
    }

    protected abstract Creative getCreative();

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            cancel();
            return true;
        } else if (item.getItemId() == R.id.menu_item_custom) {
            final String url = getCreative().getCustomEngagementUrl();
            final Intent intent = new Intent();
            intent.setData(Uri.parse(url));
            getContext().startActivity(intent);

            return true;
        } else {
            return super.onMenuItemSelected(featureId, item);
        }
    }
}
