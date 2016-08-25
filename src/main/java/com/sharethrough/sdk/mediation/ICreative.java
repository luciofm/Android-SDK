package com.sharethrough.sdk.mediation;

/**
 * Created by engineer on 8/3/16.
 */
public interface ICreative {
    void setNetworkType(String networkType);
    String getNetworkType();
    void setClassName(String className);
    String getClassName();

    //todo: classes extending ICreative need serialize and deserialize method
}
