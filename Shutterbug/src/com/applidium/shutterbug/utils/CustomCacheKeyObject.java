package com.applidium.shutterbug.utils;

public class CustomCacheKeyObject {
    protected String url;
    protected String customCacheKeyPrefix;

    public CustomCacheKeyObject(String url, String customCacheKeyPrefix) {
        this.url = url;
        this.customCacheKeyPrefix = customCacheKeyPrefix;
    }

    public String getUrl() {
        return url;
    }

    public String getCustomCacheKeyPrefix() {
        return customCacheKeyPrefix;
    }
}
