package com.sharethrough.sdk;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

public class SharethroughListAdapter extends BaseAdapter {

    private final ListAdapter mAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;
    private Placement placement;

    private final int adLayoutResourceId;
    private final int titleViewId;
    private final int descriptionViewId;
    private final int advertiserViewId;
    private final int thumbnailViewId;

    public SharethroughListAdapter(Context context, ListAdapter adapter, Sharethrough sharethrough, int adLayoutResourceId, int titleViewId, int advertiserViewId, int thumbnailViewId) {
        this(context, adapter, sharethrough, adLayoutResourceId, titleViewId, -1, advertiserViewId, thumbnailViewId);
    }

    public SharethroughListAdapter(Context context, ListAdapter adapter, Sharethrough sharethrough, int adLayoutResourceId, int titleViewId, int descriptionViewId, int advertiserViewId, int thumbnailViewId) {
        mContext = context;
        mAdapter = adapter;
        mSharethrough = sharethrough;
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;

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

        this.adLayoutResourceId = adLayoutResourceId;

        placement = new Placement(Integer.MAX_VALUE, Integer.MAX_VALUE);
        sharethrough.getPlacement(new Callback<Placement>() {
            @Override
            public void call(Placement result) {
                placement = result;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean isEnabled(int position) {
        return isAd(position) || mAdapter.isEnabled(adjustedPosition(position));
    }

    @Override
    public int getCount() {
        int count = mAdapter.getCount();
        return numberOfAds(count) + count;
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

        adView.showAd(mSharethrough, adLayoutResourceId, titleViewId, descriptionViewId, advertiserViewId, thumbnailViewId);

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
        if (position < placement.getArticlesBeforeFirstAd()) {
            return position;
        } else {
            int numberOfAdsShown = 1 + (position - placement.getArticlesBeforeFirstAd()) / (placement.getArticlesBetweenAds() + 1);
            return position - numberOfAdsShown;
        }
    }

    private boolean isAd(int position) {
        int articlesBeforeFirstAd = placement.getArticlesBeforeFirstAd();
        return position == articlesBeforeFirstAd ||
                position >= articlesBeforeFirstAd &&
                        0 == (position - articlesBeforeFirstAd) % (placement.getArticlesBetweenAds() + 1);
    }

    private int numberOfAds(int count) {
        if (count < placement.getArticlesBeforeFirstAd()) {
            return 0;
        }
        return 1 + (count - placement.getArticlesBeforeFirstAd()) / placement.getArticlesBetweenAds();
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
