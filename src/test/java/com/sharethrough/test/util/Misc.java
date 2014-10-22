package com.sharethrough.test.util;

import android.view.View;
import android.view.ViewGroup;

public class Misc {
    public static <V extends View> V findViewOfType(Class<V> viewType, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View v = viewGroup.getChildAt(i);

            V viewFound;

            if (viewType.isInstance(v)) {
                return (V) v;
            } else if (v instanceof ViewGroup && (viewFound = findViewOfType(viewType, (ViewGroup)v)) != null) {
                return viewFound;
            }
        }
        return null;
    }
}