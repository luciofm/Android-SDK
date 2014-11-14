package com.sharethrough.sdk;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.sharethrough.android.sdk.R;

public class SharethroughListAdapter extends BaseAdapter {

    private final ListAdapter mAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;

    private final int adLayout;

    private static final int AD_INDEX = 3;

    public SharethroughListAdapter(Context context, ListAdapter adapter, Sharethrough sharethrough, int adLayout) {
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
    public boolean isEnabled(int position) {
        return isAd(position) || mAdapter.isEnabled(adjustedPosition(position));
    }

    @Override
    public int getCount() {
        return numberOfAds() + mAdapter.getCount();
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

    @Override
    public boolean hasStableIds() {
        return mAdapter.hasStableIds();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mAdapter.areAllItemsEnabled();
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

    private int numberOfAds() {
        return 1;
    }

    public AdapterView.OnItemLongClickListener createOnItemLongClickListener(final AdapterView.OnItemLongClickListener onItemLongClickListener) {
        return new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (isAd(position)) {
                    return view.performLongClick();
                }
                return onItemLongClickListener.onItemLongClick(parent, view, adjustedPosition(position), id);
            }
        };
    }

    public AdapterView.OnItemClickListener createOnItemClickListener(final AdapterView.OnItemClickListener onItemClickListener) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isAd(position)) {
                    view.performClick();
                } else {
                    onItemClickListener.onItemClick(parent, view, adjustedPosition(position), id);
                }
            }
        };
    }

    public AdapterView.OnItemSelectedListener createOnItemSelectListener(final AdapterView.OnItemSelectedListener onItemSelectedListener) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isAd(position)) {
                    onItemSelectedListener.onItemSelected(parent, view, adjustedPosition(position), id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                onItemSelectedListener.onNothingSelected(parent);
            }
        };
    }
}
