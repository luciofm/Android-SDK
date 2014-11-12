package com.sharethrough.sdk;

public class NoOp implements Runnable {
    public static final NoOp INSTANCE = new NoOp();

    @Override
    public void run() {
    }
}
