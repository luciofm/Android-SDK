package com.sharethrough.sdk.test;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;

public class SharethroughTestRunner extends RobolectricTestRunner {
    public SharethroughTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected int pickSdkVersion(Config config, AndroidManifest appManifest) {
        return ((config != null) && (config.sdk().length > 0 && config.sdk()[0] > 0)) ? config.sdk()[0] : 18;
    }
}
