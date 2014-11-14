package com.sharethrough.sdk;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import com.sharethrough.android.sdk.R;

public class SharethroughListAdapter extends BaseAdapter {

    private final Adapter mAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;

    private final int adLayout;

    private static final int AD_INDEX = 3;

    public SharethroughListAdapter(Context context, Adapter adapter, Sharethrough sharethrough, int adLayout) {
        mContext = context;
        mAdapter = adapter;
        mSharethrough = sharethrough;

        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
            }
        });

        this.adLayout = adLayout;
    }

    @Override
    public int getCount() {
        int count = mAdapter.getCount();
        return 1 + count;
    }

    @Override
    public Object getItem(int position) {
        if (isAd(position)) {
            return null;
        } else {
            return mAdapter.getItemViewType(adjustedPosition(position));
        }
    }

    @Override
    public long getItemId(int position) {
        if (isAd(position)) {
            return -1;
        }

        return mAdapter.getItemId(adjustedPosition(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (isAd(position)) {
            return getAd();
        } else {
            return mAdapter.getView(adjustedPosition(position), convertView, parent);
        }
    }

    private View getAd() {

        BasicAdView adView = new BasicAdView(mContext);

        adView.showAd(mSharethrough, adLayout, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);

        return adView;
    }

    @Override
    public int getItemViewType(int position) {
        if (isAd(position)) {
            return mAdapter.getViewTypeCount();
        } else {
            return mAdapter.getItemViewType(adjustedPosition(position));
        }
    }

    @Override
    public int getViewTypeCount() {
        return 1 + mAdapter.getViewTypeCount();
    }

    private int adjustedPosition(int position) {
        if (position < AD_INDEX) {
            return position;
        } else {
            return position - 1;
        }
    }

    private boolean isAd(int position) {
        return position == AD_INDEX;
    }
}
