package com.sharethrough.test.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Robolectric.shadowOf;

public class Misc {
    public static <V extends View> V findViewOfType(Class<V> viewType, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);

            V viewFound;

            if (viewType.isInstance(v)) {
                return (V) v;
            } else if (v instanceof ViewGroup && (viewFound = findViewOfType(viewType, (ViewGroup) v)) != null) {
                return viewFound;
            }
        }
        return null;
    }

    public static void assertImageViewFromBytes(ImageView imageView, byte[] imageBytes) {
        Bitmap expected = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        assertImageViewFromBitmap(imageView, expected);
    }

    public static void assertImageViewFromBitmap(ImageView imageView, Bitmap expected) {
        Bitmap actual = ((BitmapDrawable) shadowOf(imageView).getDrawable()).getBitmap();
        assertThat(actual).isEqualTo(expected);
    }
}