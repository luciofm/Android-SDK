package com.sharethrough.sdk;

import android.app.Application;
import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class STRExecutorService {
    private static ExecutorService service;
    private Context context;

    public static ExecutorService getInstance(){
        if( service == null ) {
            service = Executors.newFixedThreadPool(4);// TODO: pick a reasonable number
        }
        return service;
    }

    public static void setExecutorService(ExecutorService executorService) {
        service = executorService;
    }
}
