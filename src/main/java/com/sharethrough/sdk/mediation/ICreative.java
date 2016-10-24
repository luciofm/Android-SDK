package com.sharethrough.sdk.mediation;

/**
 * Created by engineer on 8/3/16.
 */
public abstract class ICreative {
    protected final String networkType;
    protected final String className;
    protected final String mrid;
    protected int placementIndex;

    public ICreative(final String networkType, final String className, final String mrid) {
        this.networkType = networkType;
        this.className = className;
        this.mrid = mrid;
    }
    public abstract void setPlacementIndex(int placementIndex);
    public abstract int getPlacementIndex();
    public abstract String getNetworkType();
    public abstract String getClassName();

    //todo: classes extending ICreative need serialize and deserialize method
}
