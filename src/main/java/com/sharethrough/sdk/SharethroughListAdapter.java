package com.sharethrough.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * A list adapter which wraps your list adapter and inserts Sharethrough ads.
 */
public class SharethroughListAdapter extends BaseAdapter {

    private final ListAdapter mAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;
    private final int adLayoutResourceId;
    private final int titleViewId;
    private final int descriptionViewId;
    private final int advertiserViewId;
    private final int thumbnailViewId;
    private final int optoutId;
    private final int brandLogoId;

    /**
     * This constructor returns an instance of a SharethroughListAdapter, which wraps your own list adapter.
     *
     * @param context The Android context.
     * @param adapter Your adapter.
     * @param sharethrough An instance of your configured Sharethrough object.
     * @param adLayoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param descriptionViewId The view which will display the ad's description.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     * @param brandLogoId The imageView which will display the ad's brand logo.
     */
    public SharethroughListAdapter(Context context, ListAdapter adapter, Sharethrough sharethrough, int adLayoutResourceId, int titleViewId, int descriptionViewId, int advertiserViewId, int thumbnailViewId, int optoutId, int brandLogoId) {
        mContext = context;
        mAdapter = adapter;
        mSharethrough = sharethrough;
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        this.optoutId = optoutId;
        this.brandLogoId = brandLogoId;

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
        mSharethrough.setOrCallPlacementCallback(new Callback<Placement>() {
            @Override
            public void call(final Placement result) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
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
            return mAdapter.getItem(adjustedPosition(position));
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
            // we must check to make sure convertView is correct type
            if (convertView != null && !(convertView instanceof IAdView)) {
                convertView = null;
            }
            return getAd(position, (IAdView) convertView);
        } else {
            if (convertView != null && convertView instanceof IAdView) {
                convertView = null;
            }
            return mAdapter.getView(adjustedPosition(position), convertView, parent);
        }
    }

    private View getAd(int slotNumber, IAdView convertView) {
        return mSharethrough.getAdView(mContext, slotNumber, adLayoutResourceId, titleViewId, descriptionViewId,
                advertiserViewId, thumbnailViewId, optoutId, brandLogoId, convertView).getAdView();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private int adjustedPosition(int position) {
        if (position < mSharethrough.placement.getArticlesBeforeFirstAd()) {
            return position;
        } else {
            int numberOfAdsToPossiblyShow = 1 + (position - mSharethrough.placement.getArticlesBeforeFirstAd()) / (mSharethrough.placement.getArticlesBetweenAds() + 1);
            int numberOfAdsAvailable = creativesCount();
            int numberOfAdsPlaced = mSharethrough.creativesBySlot.size();
            int numberOfAdsShown = Math.min(numberOfAdsToPossiblyShow, numberOfAdsAvailable);
            int adjustedPosition = position - numberOfAdsShown;

            Log.d("ADJUSTED", "position: " + position);
            Log.d("ADJUSTED", "adjusted position: " + adjustedPosition);
            Log.d("ADJUSTED", "numberOfAdsShown : " + numberOfAdsShown );

            return adjustedPosition;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private boolean isAd(int position) {
//        int articlesBeforeFirstAd = mSharethrough.placement.getArticlesBeforeFirstAd();
//        return position == articlesBeforeFirstAd ||
//                position >= articlesBeforeFirstAd &&
//                        0 == (position - articlesBeforeFirstAd) % (mSharethrough.placement.getArticlesBetweenAds() + 1);

        int articlesBeforeFirstAd = mSharethrough.placement.getArticlesBeforeFirstAd();

        if (creativesCount() == 0){
            return false;
        }

        if (position == articlesBeforeFirstAd) {
            return mSharethrough.creativesBySlot.get(position) != null || mSharethrough.availableCreatives.size() != 0;
        }

//        int numberOfAdsToPossiblyShow = 1 + (position - mSharethrough.placement.getArticlesBeforeFirstAd()) / (mSharethrough.placement.getArticlesBetweenAds() + 1);
//        int numberOfAdsAvailable = creativesCount();
//        int numberOfAdsShown = Math.min(numberOfAdsToPossiblyShow, numberOfAdsAvailable);

        boolean couldPossiblyBeAnAd = 0 == (position - articlesBeforeFirstAd) % (mSharethrough.placement.getArticlesBetweenAds() + 1);
        boolean adAlreadyInPosition = mSharethrough.creativesBySlot.get(position) != null;

        return position >= articlesBeforeFirstAd && couldPossiblyBeAnAd && (adAlreadyInPosition || mSharethrough.availableCreatives.size() != 0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private int creativesCount() {
        return mSharethrough.availableCreatives.size() + mSharethrough.creativesBySlot.size();
    }

    private int numberOfAds(int count) {
        if (count < mSharethrough.placement.getArticlesBeforeFirstAd()) {
            return 0;
        }
        return 1 + (count - mSharethrough.placement.getArticlesBeforeFirstAd()) / mSharethrough.placement.getArticlesBetweenAds();
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
