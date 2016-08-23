package com.sharethrough.sdk;


import android.util.LruCache;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SharethroughSerializerTest extends TestBase {
    @Test
    public void Serialize_WhenQueueIsNullReturnEmptyString() throws Exception {
        LruCache<Integer, Creative> slot = new LruCache<>(10);
        assertThat(SharethroughSerializer.serialize(null, slot, 2, 5)).isEqualTo("");
    }

    @Test
    public void Serialize_WhenSlotIsNullReturnEmptyString() throws Exception {
        CreativesQueue cq = new CreativesQueue();
        assertThat(SharethroughSerializer.serialize(cq, null, 2, 5)).isEqualTo("");
    }

    @Test
    public void Serialize_ResultStringShouldContainQueueAndSlotAndArticles() throws Exception {
        LruCache<Integer, Creative> slot = new LruCache<>(10);
        CreativesQueue cq = new CreativesQueue();
        String serializedSharethrough = SharethroughSerializer.serialize(cq, slot, 2, 5);
        assertThat(serializedSharethrough).contains(SharethroughSerializer.SLOT);
        assertThat(serializedSharethrough).contains(SharethroughSerializer.QUEUE);
        assertThat(serializedSharethrough).contains(SharethroughSerializer.ARTICLES_BEFORE);
        assertThat(serializedSharethrough).contains(SharethroughSerializer.ARTICLES_BETWEEN);
    }

    @Test
    public void GetCreativesQueue_EmptyStringShouldReturnEmptyNewCreativesQueue() throws Exception {
        CreativesQueue cq = SharethroughSerializer.getCreativesQueue("");
        assertThat(cq).isNotNull();
        assertThat(cq.size()).isEqualTo(0);
    }

    @Test
    public void Deserialize_EmptyStringShouldReturnEmptyHashMap() throws Exception {
        assertThat(SharethroughSerializer.deserialize("")).isNotNull();
        assertThat(SharethroughSerializer.deserialize("").size()).isEqualTo(0);
    }

    @Test
    public void Deserialize_NullShouldReturnEmptyHashMap() throws Exception {
        assertThat(SharethroughSerializer.deserialize(null)).isNotNull();
        assertThat(SharethroughSerializer.deserialize(null).size()).isEqualTo(0);
    }

    @Test
    public void GetCreativesQueue_SerializedStringShouldReturnCreativesQueue() throws Exception {
        CreativesQueue cq = new CreativesQueue();
        Creative creative1 = new Creative(new Response.Creative(), "mrid");
        creative1.setNetworkType("STX");
        cq.add(creative1);
        Creative creative2 = new Creative(new Response.Creative(), "mrid");
        creative2.setNetworkType("STX");
        cq.add(creative2);
        Creative creative3 = new Creative(new Response.Creative(), "mrid");
        creative3.setNetworkType("STX");
        cq.add(creative3);
        assertThat(cq.size()).isEqualTo(3);

        String serializedSharethroughObj = SharethroughSerializer.serialize(cq, new LruCache<Integer, Creative>(10), 2, 5);
        CreativesQueue retreivedCq = SharethroughSerializer.getCreativesQueue(serializedSharethroughObj);
        assertThat(retreivedCq.size()).isEqualTo(3);;
        assertThat(retreivedCq.getNext() instanceof Creative).isEqualTo(true);
        assertThat(retreivedCq.getNext().getNetworkType()).isEqualTo("STX");
        assertThat(retreivedCq.getNext().getNetworkType()).isEqualTo("STX");
    }

    @Test
    public void GetCreativesQueue_SerializedStringShouldReturnCreativesQueueWithOnlySharethrough() throws Exception {
        CreativesQueue cq = new CreativesQueue();
        Creative creative1 = new Creative(new Response.Creative(), "mrid");
        creative1.setNetworkType("STX");
        cq.add(creative1);
        Creative creative2 = new Creative(new Response.Creative(), "mrid");
        creative2.setNetworkType("NOT STX");
        cq.add(creative2);
        Creative creative3 = new Creative(new Response.Creative(), "mrid");
        creative3.setNetworkType("STX");
        cq.add(creative3);
        assertThat(cq.size()).isEqualTo(3);

        String serializedSharethroughObj = SharethroughSerializer.serialize(cq, new LruCache<Integer, Creative>(10), 2, 5);
        CreativesQueue retreivedCq = SharethroughSerializer.getCreativesQueue(serializedSharethroughObj);

        //only STX creatives are serialized because the Serializer is only aware of "STX" type creatives
        assertThat(retreivedCq.size()).isEqualTo(2);
    }

    @Test
    public void GetSlot_EmptyStringShouldReturnEmptyNewSlot() throws Exception {
        LruCache<Integer, Creative> slot = SharethroughSerializer.getSlot("");
        assertThat(slot).isNotNull();
        assertThat(slot.size()).isEqualTo(0);
        assertThat(slot.maxSize()).isEqualTo(10);
    }

    @Test
    public void GetSlot_SerializedStringShouldReturnSlot() throws Exception {
        LruCache<Integer,Creative> slot = new LruCache<>(10);

        slot.put(0, new Creative(new Response.Creative(), "mrid"));
        slot.put(1, new Creative(new Response.Creative(), "mrid"));
        slot.put(2, new Creative(new Response.Creative(), "mrid"));
        assertThat(slot.size()).isEqualTo(3);

        String serializedSharethroughObj = SharethroughSerializer.serialize(new CreativesQueue(), slot, 2, 5);
        LruCache<Integer, Creative> retreivedSlot = SharethroughSerializer.getSlot(serializedSharethroughObj);
        assertThat(retreivedSlot.size()).isEqualTo(3);
    }

    @Test
    public void GetArticlesBefore_returnArticlesBefore() throws Exception {
        LruCache<Integer, Creative> slot = new LruCache<>(10);
        CreativesQueue cq = new CreativesQueue();
        String serializedSharethrough = SharethroughSerializer.serialize(cq, slot, 2, 5);
        assertThat(SharethroughSerializer.getArticlesBefore(serializedSharethrough)).isEqualTo(2);
    }

    @Test
    public void GetArticlesBetween_returnArticlesBetween() throws Exception {
        LruCache<Integer, Creative> slot = new LruCache<>(10);
        CreativesQueue cq = new CreativesQueue();
        String serializedSharethrough = SharethroughSerializer.serialize(cq, slot, 2, 5);
        assertThat(SharethroughSerializer.getArticlesBetween(serializedSharethrough)).isEqualTo(5);
    }
}
