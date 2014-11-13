package com.sharethrough.sdk;

public interface Function<IN, OUT> {
    OUT apply(IN in);
}
