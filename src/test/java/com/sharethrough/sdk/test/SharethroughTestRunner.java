package com.sharethrough.sdk.test;

import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.SdkConfig;
import org.robolectric.annotation.Config;

public class SharethroughTestRunner extends RobolectricTestRunner {
    public SharethroughTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected SdkConfig pickSdkVersion(AndroidManifest appManifest, Config config) {
        return ((config != null) && (config.emulateSdk() > 0)) ? new SdkConfig(config.emulateSdk()) : new SdkConfig(18);
    }
}
