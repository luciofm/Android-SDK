package com.sharethrough.sdk;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

    /**
     *
     * @param context The Android context.
     * @param adapter Your adapter.
     * @param sharethrough An instance of your configured Sharethrough object.
     * @param adLayoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     */
    public SharethroughListAdapter(Context context, ListAdapter adapter, Sharethrough sharethrough, int adLayoutResourceId, int titleViewId, int advertiserViewId, int thumbnailViewId) {
        this(context, adapter, sharethrough, adLayoutResourceId, titleViewId, -1, advertiserViewId, thumbnailViewId);
    }

    /**
     *
     * @param context The Android context.
     * @param adapter Your adapter.
     * @param sharethrough An instance of your configured Sharethrough object.
     * @param adLayoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param descriptionViewId The view which will display the ad's description.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     */
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
            public void call(final Placement result) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        placement = result;
                        notifyDataSetChanged();
                    }
                });
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
            return getAd(position, (IAdView) convertView);
        } else {
            return mAdapter.getView(adjustedPosition(position), convertView, parent);
        }
    }

    private View getAd(int slotNumber, IAdView convertView) {
        Log.d("LISTVIEW", "convert view is " + convertView + " slotNumber " + slotNumber);
        return mSharethrough.getAdView(mContext, slotNumber, adLayoutResourceId, titleViewId, descriptionViewId,
                advertiserViewId, thumbnailViewId, convertView).getAdView();
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

    /**
     *
     * @param onItemLongClickListener The original listener callback.
     * @return A new long click listener that forwards requests to the original listener.
     */
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

    /**
     *
     * @param onItemClickListener The original listener callback.
     * @return A new click listener that forwards requests to the original listener.
     */
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

    /**
     *
     * @param onItemSelectedListener The original listener callback.
     * @return A new selected listener that forwards requests to the original listener.
     */
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
