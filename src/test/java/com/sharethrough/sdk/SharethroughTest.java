package com.sharethrough.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.ANDROID.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
public class SharethroughTest {
    @Test
    public void testName() throws Exception {
        assertThat(false).isFalse();
    }
}