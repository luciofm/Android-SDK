package com.sharethrough.sdk;

import android.util.LruCache;
import android.util.Pair;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sharethrough.sdk.mediation.ICreative;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SharethroughSerializer {
    public static final String SLOT = "slot";
    public static final String QUEUE = "queue";
    public static final String ARTICLES_BEFORE = "articles_before";
    public static final String ARTICLES_BETWEEN = "articles_between";
    private static Gson gson = new Gson();

    public static HashMap<String, Class> classMap= new HashMap<>();
    static {
        classMap.put("STX", Creative.class);
    }

    public static String serialize( CreativesQueue queue, LruCache<Integer, ICreative> slot, int articlesBefore, int articlesBetween ){
        if( queue == null || slot == null ){
            return "";
        }

        HashMap<String, String> hashmap = new HashMap<>();
        Type slotType = new TypeToken<LruCache<Integer,ICreative>>() {}.getType();

        String slotOutput = gson.toJson(retainSharethroughCreatives(slot), slotType);
        hashmap.put(QUEUE, serializeQueueByType(queue));
        hashmap.put(SLOT, slotOutput);
        hashmap.put(ARTICLES_BEFORE, String.valueOf(articlesBefore));
        hashmap.put(ARTICLES_BETWEEN, String.valueOf(articlesBetween));
        String serializedSharethroughObj = gson.toJson(hashmap);
        return serializedSharethroughObj;
    }

    public static LruCache<Integer, ICreative> retainSharethroughCreatives(LruCache<Integer, ICreative> slot) {
        LruCache<Integer,ICreative> result = new LruCache<>(10);
        Map<Integer, ICreative> snapshot = slot.snapshot();
        for(Integer position :snapshot.keySet()){
            if(snapshot.get(position) instanceof Creative ){
                result.put(position, snapshot.get(position));
            }
        }
        return result;
    }

    /**
     * Serialize creative queue based on creative type, all new creative types need to have a "serialize"
     * and "deserialize" method
     * @param queue
     * @return
     */
    public static String serializeQueueByType(CreativesQueue queue) {
        ArrayList<Pair<String, String>> result = new ArrayList<>();
        while (queue.size() > 0) {
            ICreative creative = queue.getNext();
            if (classMap.containsKey(creative.getNetworkType())) {
                Class creativeClass = classMap.get(creative.getNetworkType());
                try {
                    Method method = creativeClass.getDeclaredMethod("serialize", ICreative.class);
                    result.add(new Pair(creative.getNetworkType(), (String) method.invoke(null, creative)));
                } catch (NoSuchMethodException e) {
                    Logger.e("There is no serialize method for this class - %s", e, creativeClass);
                } catch (InvocationTargetException e) {
                    Logger.e("Serialize method for this class - %s - cannot be invoked", e, creativeClass);
                } catch (IllegalAccessException e) {
                    Logger.e("Serialize method for this class - %s - cannot be invoked", e, creativeClass);
                }

            }
        }

        Type serializedQueueType = new TypeToken<ArrayList<Pair<String,String>>>() {}.getType();
        String queueOutput = gson.toJson(result, serializedQueueType);
        return queueOutput;
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

        Type serializedQueueType = new TypeToken<ArrayList<Pair<String,String>>>() {}.getType();
        ArrayList<Pair<String,String>> cq = gson.fromJson(serializedQueue, serializedQueueType);
        if( cq == null ){
            cq = new ArrayList<>();
        }

        CreativesQueue result = new CreativesQueue();
        for (Pair<String, String> entry : cq) {
            if (classMap.containsKey(entry.first)) {
                Class creativeClass = classMap.get(entry.first);
                try {
                    Method method = creativeClass.getDeclaredMethod("deserialize", String.class);
                    result.add((ICreative) method.invoke(null, entry.second));
                } catch (NoSuchMethodException e) {
                    Logger.e("There is no deserialize method for this class - %s", e, creativeClass);
                } catch (InvocationTargetException e) {
                    Logger.e("Deserialize method for this class - %s - cannot be invoked", e, creativeClass);
                } catch (IllegalAccessException e) {
                    Logger.e("Deserialize method for this class - %s - cannot be invoked", e, creativeClass);                }
            }
        }

        return result;
    }

    /**
     * Gets creative slot from serialized Sharethrough string
     * @param serializedSharethrough
     * @return creative slot containing rendered creatives, or empty slot otherwise
     */
    public static LruCache<Integer, ICreative> getSlot(String serializedSharethrough) {
        String serializedSlot = (String) deserialize(serializedSharethrough).get(SLOT);
        Type lruCacheType = new TypeToken<LruCache<Integer, Creative>>() {}.getType();
        LruCache<Integer, ICreative> slot = gson.fromJson(serializedSlot, lruCacheType);
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
