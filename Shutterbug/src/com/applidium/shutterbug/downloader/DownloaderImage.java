package com.applidium.shutterbug.downloader;

import android.graphics.Bitmap;

public class DownloaderImage {
    private Bitmap mBitmap;
    private byte[] mMovie;
    private String mMimeType;

    public DownloaderImage(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public DownloaderImage(Bitmap bitmap, String mimeType) {
        mBitmap = bitmap;
        mMimeType = mimeType;
    }

    public DownloaderImage(byte[] movieBytes) {
        mMovie = movieBytes;
    }

    public boolean isBitmap() {
        return mBitmap != null;
    }

    public byte[] getMovieBytes() {
        return mMovie;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public String getMimeType() {
        return mMimeType;
    }
}
