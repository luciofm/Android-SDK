package com.sharethrough.sdk;

interface Function<IN, OUT> {
    OUT apply(IN in);
}
