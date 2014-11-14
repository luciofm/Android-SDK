package com.sharethrough.sdk;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import com.sharethrough.android.sdk.R;

import java.util.ArrayList;
import java.util.List;

public class SharethroughListAdapter extends BaseAdapter {

    private final Adapter mAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;

    private final int adLayout;

    private static final int AD_INDEX = 3;

    private List<Creative> mCreatives = new ArrayList<>();

    public SharethroughListAdapter(Context context, Adapter adapter, Sharethrough sharethrough, int adLayout) {
        mContext = context;
        mAdapter = adapter;
        mSharethrough = sharethrough;
        this.adLayout = adLayout;
    }

    @Override
    public int getCount() {
        int count = mAdapter.getCount();
        return 1 + count;
    }

    @Override
    public Object getItem(int position) {
        if (position < AD_INDEX) {
            return mAdapter.getItem(position);
        } else if (position == AD_INDEX) {
            return mSharethrough;
        } else {
            return mAdapter.getItem(position - 1);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < AD_INDEX) {
            return mAdapter.getItemId(position);
        } else if (position == AD_INDEX) {
            return -1;
        }

        return mAdapter.getItemId(position);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (position == AD_INDEX) {
            return getAd();
        } else {
            if (position < AD_INDEX) {
                if (convertView != null) {
                }
                return mAdapter.getView(position, convertView, parent);
            } else {
                if (convertView != null) {
                }
                return mAdapter.getView(position - 1, convertView, parent);
            }
        }
    }

    private View getAd() {

        BasicAdView adView = new BasicAdView(mContext);

        adView.showAd(mSharethrough, adLayout, R.id.title, R.id.description, R.id.advertiser, R.id.thumbnail);

        return adView;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == AD_INDEX) {
            return 1; // TODO: figure out correct return types
        } else {
            return 0;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 1 + mAdapter.getViewTypeCount();
    }

}
