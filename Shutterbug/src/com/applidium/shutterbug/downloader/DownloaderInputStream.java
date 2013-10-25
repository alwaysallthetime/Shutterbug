package com.applidium.shutterbug.downloader;

import java.io.InputStream;

public class DownloaderInputStream {
    private InputStream mInputStream;
    private String mMimetype;

    public DownloaderInputStream(InputStream inputStream, String mimeType) {
        mInputStream = inputStream;
        mMimetype = mimeType;
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public String getMimetype() {
        return mMimetype;
    }
}
