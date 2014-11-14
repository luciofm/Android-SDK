package com.sharethrough.sdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreativesQueue {
    private final List<Creative> list = Collections.synchronizedList(new ArrayList<Creative>());

    public void add(Creative creative) {
        list.add(creative);
    }

    public Creative getNext() {
        return list.isEmpty() ? null : list.remove(0);
    }
}
