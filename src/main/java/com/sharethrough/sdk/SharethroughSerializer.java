package com.sharethrough.sdk;

import android.util.LruCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;



public class SharethroughSerializer {
    public static final String SLOT = "slot";
    public static final String QUEUE = "queue";
    public static final String ARTICLES_BEFORE = "articles_before";
    public static final String ARTICLES_BETWEEN = "articles_between";
    private static Gson gson = new Gson();

    public static String serialize( CreativesQueue queue, LruCache<Integer, Creative> slot, int articlesBefore, int articlesBetween ){
        if( queue == null || slot == null ){
            return "";
        }

        HashMap<String, String> hashmap = new HashMap<>();
        Type slotType = new TypeToken<LruCache<Integer,Creative>>() {}.getType();
        String slotOutput = gson.toJson(slot, slotType);
        String queueOutput = gson.toJson(queue);
        hashmap.put(QUEUE, queueOutput);
        hashmap.put(SLOT, slotOutput);
        hashmap.put(ARTICLES_BEFORE, String.valueOf(articlesBefore));
        hashmap.put(ARTICLES_BETWEEN, String.valueOf(articlesBetween));
        String serializedSharethroughObj = gson.toJson(hashmap);
        return serializedSharethroughObj;
    }

    /**
     * Internal method that deserializes string to HashMap with necessary Sharethrough state
     * @param serializedSharethrough string
     * @return HashMap with necessary STR state, or empty HashMap otherwise
     */
    protected static HashMap deserialize(String serializedSharethrough) {
        Type serializedSharethroughType = new TypeToken<HashMap<String, String>>() {}.getType();
        HashMap<String,String> deserializedSharethroughObj = gson.fromJson(serializedSharethrough, serializedSharethroughType);
        if( deserializedSharethroughObj == null ){
            deserializedSharethroughObj = new HashMap<>();
        }

        return deserializedSharethroughObj;
    }

    /**
     * Gets creative queue from serialized Sharethrough string
     * @param serializedSharethrough
     * @return creative queue containing cached creative, or empty queue otherwise
     */
    public static CreativesQueue getCreativesQueue(String serializedSharethrough) {
        String serializedQueue = (String) deserialize(serializedSharethrough).get(QUEUE);
        CreativesQueue cq = gson.fromJson(serializedQueue, CreativesQueue.class);
        if( cq == null ){
            cq = new CreativesQueue();
        }
        return cq;
    }

    /**
     * Gets creative slot from serialized Sharethrough string
     * @param serializedSharethrough
     * @return creative slot containing rendered creatives, or empty slot otherwise
     */
    public static LruCache<Integer, Creative> getSlot(String serializedSharethrough) {
        String serializedSlot = (String) deserialize(serializedSharethrough).get(SLOT);
        Type lruCacheType = new TypeToken<LruCache<Integer, Creative>>() {}.getType();
        LruCache<Integer,Creative> slot = gson.fromJson(serializedSlot, lruCacheType);
        if( slot == null ){
            slot = new LruCache<>(10);
        }
        return slot;
    }

    public static int getArticlesBefore(String serializedSharethrough) {
        String articlesBefore = (String) deserialize(serializedSharethrough).get(ARTICLES_BEFORE);
        return Integer.parseInt(articlesBefore);
    }

    public static int getArticlesBetween(String serializedSharethrough) {
        String articlesBetween = (String) deserialize(serializedSharethrough).get(ARTICLES_BETWEEN);
        return Integer.parseInt(articlesBetween);
    }
}
