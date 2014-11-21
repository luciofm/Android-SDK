package com.sharethrough.sdk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultSharethroughExecutorServiceProivder {
    public static ExecutorService create() {
        return Executors.newFixedThreadPool(4); // TODO: pick a reasonable number
    }
}
