package com.sharethrough.sdk;

import com.sharethrough.sdk.mediation.ICreative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreativesQueue {
    private final List<ICreative> list = Collections.synchronizedList(new ArrayList<ICreative>());

    public void add(ICreative creative) {
        list.add(creative);
    }

    public boolean readyForMore() {
        return list.size() <= 1;
    }

    public ICreative getNext() {
        return list.isEmpty() ? null : list.remove(0);
    }

    public int size(){
        return list.size();
    }
}
