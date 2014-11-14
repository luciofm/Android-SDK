package com.sharethrough.sdk;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class CreativesQueueTest extends TestBase {

    @Test
    public void getNext_whenListIsEmpty_returnsNull() throws Exception {
        CreativesQueue subject = new CreativesQueue();
        assertThat(subject.getNext()).isNull();
    }
}