package com.sharethrough.sdk;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class SynchronizedWeakOrderedSet<T> {

    private final List<WeakReference<T>> list = new LinkedList<>();

    /**
     * @return null if there aren't any valid references remaining
     */
    public synchronized T popNext() {
        T result = null;
        while (list.size() > 0) {
            result = list.remove(0).get();
            if (result != null) break;
        }
        return result;
    }

    public synchronized void put(T object) {
        for (int i = list.size() - 1; i >= 0; i--) {
            WeakReference<T> reference = list.get(i);
            if (reference.get() == null || reference.get() == object) {
                list.remove(i);
            }
        }

        list.add(new WeakReference<>(object));
    }

    public synchronized int size(){
        return list.size();
    }
}
