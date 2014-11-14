package com.sharethrough.sdk;

import com.sharethrough.sdk.test.SharethroughTestRunner;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(SharethroughTestRunner.class)
public abstract class TestBase {
    public TestBase() {
        MockitoAnnotations.initMocks(this);
    }
}
