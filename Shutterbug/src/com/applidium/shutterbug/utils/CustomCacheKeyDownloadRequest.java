package com.applidium.shutterbug.utils;

public class CustomCacheKeyDownloadRequest extends DownloadRequest {

    private CustomCacheKeyObject object;

    public CustomCacheKeyDownloadRequest(CustomCacheKeyObject object, int maxWidth, int maxHeight, ShutterbugManager.ShutterbugManagerListener listener) {
        super(object.getUrl(), maxWidth, maxHeight, listener);
        this.object = object;
    }

    public String getCacheKeyPrefix() {
        return object.getCustomCacheKeyPrefix();
    }
}
