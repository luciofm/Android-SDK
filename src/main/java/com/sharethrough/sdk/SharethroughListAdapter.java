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

    private final ListAdapter mOriginalAdapter;
    private final Context mContext;
    private final Sharethrough mSharethrough;
    
    // Ad variables
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
     * @param originalAdapter Your adapter.
     * @param sharethrough An instance of your configured Sharethrough object.
     * @param adLayoutResourceId The custom layout for Sharethrough's native ad unit.
     * @param titleViewId The view which will display the ad's title.
     * @param descriptionViewId The view which will display the ad's description.
     * @param advertiserViewId The view which will display the ad's advertiser.
     * @param thumbnailViewId The view which will display the ad's thumbnail image.
     * @param brandLogoId The imageView which will display the ad's brand logo.
     */
    public SharethroughListAdapter(Context context, ListAdapter originalAdapter, Sharethrough sharethrough, int adLayoutResourceId, int titleViewId, int descriptionViewId, int advertiserViewId, int thumbnailViewId, int optoutId, int brandLogoId) {
        mContext = context;
        mOriginalAdapter = originalAdapter;
        mSharethrough = sharethrough;
        this.adLayoutResourceId = adLayoutResourceId;
        this.titleViewId = titleViewId;
        this.descriptionViewId = descriptionViewId;
        this.advertiserViewId = advertiserViewId;
        this.thumbnailViewId = thumbnailViewId;
        this.optoutId = optoutId;
        this.brandLogoId = brandLogoId;

        mOriginalAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                notifyDataSetInvalidated();
            }
        });

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
        return isAd(position) || mOriginalAdapter.isEnabled(adjustedPosition(position));
    }

    @Override
    public int getCount() {
        int count = mOriginalAdapter.getCount();
        return  mSharethrough.getNumberOfPlacedAds() + count;
    }

    @Override
    public Object getItem(int position) {
        if (isAd(position)) {
            return null;
        } else {
            return mOriginalAdapter.getItem(adjustedPosition(position));
        }
    }

    @Override
    public long getItemId(int position) {
        if (isAd(position)) {
            return -1;
        }

        return mOriginalAdapter.getItemId(adjustedPosition(position));
    }

    @Override
    public int getItemViewType(int position) {
        if (isAd(position)) {
            return mOriginalAdapter.getViewTypeCount();
        } else {
            return mOriginalAdapter.getItemViewType(adjustedPosition(position));
        }
    }

    @Override
    public int getViewTypeCount() {
        return 1 + mOriginalAdapter.getViewTypeCount();
    }

    @Override
    public boolean hasStableIds() {
        return mOriginalAdapter.hasStableIds();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mOriginalAdapter.areAllItemsEnabled();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        boolean hasAdToShowForPosition = mSharethrough.isAdAtPosition(position) || mSharethrough.getNumberOfAdsReadyToShow() != 0;

        //if position is an ad and there's ads to show
        if (isAd(position) && hasAdToShowForPosition) {
            return getAd(position, convertView instanceof IAdView? (IAdView) convertView: null);
        }

        //if position is an ad but there are no ads available
        if(isAd(position)){
            if(mSharethrough.strSdkConfig.getCreativeIndices().contains(position)) {
                mSharethrough.strSdkConfig.getCreativeIndices().remove(position);
                //force redraw of articles below this position
                notifyDataSetChanged();
            }
        }

        mSharethrough.fetchAdsIfReadyForMore();
        return mOriginalAdapter.getView(adjustedPosition(position),
                                        convertView instanceof IAdView? null:  convertView,
                                        parent);
    }

    /**
     * Get ad view from Sharethrough
     * @param slotNumber position
     * @param convertView convertView
     * @return ad view
     */
    private View getAd(int slotNumber, IAdView convertView) {
        return mSharethrough.getAdView(mContext, slotNumber, adLayoutResourceId, titleViewId, descriptionViewId,
                advertiserViewId, thumbnailViewId, optoutId, brandLogoId, convertView).getAdView();
    }

    /**
     * Converts Sharethrough adapter position TO publisher's adapter position
     * @param position index
     * @return adjusted position
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private int adjustedPosition(int position) {
        if (position <= mSharethrough.getArticlesBeforeFirstAd()) {
            return position;
        } else {
            int numberOfAdsBeforePosition = mSharethrough.getNumberOfAdsBeforePosition(position);
            return position - numberOfAdsBeforePosition;
        }
    }

    /**
     * Checks if given position should be an ad
     * @param position index of list
     * @return true if position should be an ad, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private boolean isAd(int position) {
        int articlesBeforeFirstAd = mSharethrough.getArticlesBeforeFirstAd();
        int articlesBetweenAds = mSharethrough.getArticlesBetweenAds();

        if (position < articlesBeforeFirstAd) {
            return false;
        }
        else if (position == articlesBeforeFirstAd) {
            return true;
        }
        else if ( ((position - (articlesBeforeFirstAd)) >= articlesBetweenAds) && ((position - (articlesBeforeFirstAd)) % (articlesBetweenAds+1)) == 0) {
            return true;
        }
        else {
            return false;
        }
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
