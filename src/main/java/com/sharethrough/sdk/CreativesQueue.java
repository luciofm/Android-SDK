package com.sharethrough.sdk;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreativesQueue {
    private final List<Creative> list = Collections.synchronizedList(new ArrayList<Creative>());

    public void add(Creative creative) {
        list.add(creative);
        Log.i("DEBUG", "Added to creatives queue: " + list.size());
    }

    public boolean readyForMore() {
        return list.size() <= 1;
    }

    public Creative getNext() {
        Log.i("DEBUG", "creativesQueue.getNext (size = " + list.size() + ")");
        return list.isEmpty() ? null : list.remove(0);
    }
}
