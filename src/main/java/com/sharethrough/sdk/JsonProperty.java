package com.sharethrough.sdk;

// TODO: remove if we start using Jackson for JSON parsing again
public @interface JsonProperty {
    String value() default "";
}
