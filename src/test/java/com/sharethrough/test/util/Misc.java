package com.sharethrough.test.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import org.mockito.ArgumentCaptor;
import org.robolectric.Shadows;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;


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
        Bitmap actual = ((BitmapDrawable) Shadows.shadowOf(imageView).getDrawable()).getBitmap();
        assertThat(actual).isEqualTo(expected);
    }

    public static void runLast(ExecutorService executorService) {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService).execute(runnableArgumentCaptor.capture());
        runnableArgumentCaptor.getValue().run();
    }

    public static void runAll(ExecutorService executorService1) {
        ArgumentCaptor<Runnable> runnableArgumentCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executorService1, atLeastOnce()).execute(runnableArgumentCaptor.capture());
        List<Runnable> allExecutions = runnableArgumentCaptor.getAllValues();
        for (Runnable runnable : allExecutions) {
            runnable.run();
        }
    }
}