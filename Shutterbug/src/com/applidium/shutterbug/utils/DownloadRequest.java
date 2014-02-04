package com.applidium.shutterbug.utils;

import com.applidium.shutterbug.utils.ShutterbugManager.ShutterbugManagerListener;

public class DownloadRequest {
    private String                    mUrl;
    private ShutterbugManagerListener mListener;
    private int                       mMaxWidth;
    private int                       mMaxHeight;

    public DownloadRequest(String url, int maxWidth, int maxHeight, ShutterbugManagerListener listener) {
        mUrl = url;
        mListener = listener;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getCacheKeyPrefix() {
        return mUrl;
    }

    public ShutterbugManagerListener getListener() {
        return mListener;
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }
}
