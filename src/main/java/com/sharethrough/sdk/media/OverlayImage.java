package com.sharethrough.sdk.media;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class OverlayImage extends ImageView {

    public OverlayImage(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        FrameLayout thumbnailImage = (FrameLayout) getParent();

        for (int i = 0; i < thumbnailImage.getChildCount(); i++) {
            View child = thumbnailImage.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null && tag.equals(Media.THUMBNAIL)) {
                int overlayDimensionMax = Math.max((Math.min(child.getMeasuredHeight(), child.getMeasuredWidth()) / 4), getSuggestedMinimumHeight());
                setMeasuredDimension(overlayDimensionMax, overlayDimensionMax);
                return;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
