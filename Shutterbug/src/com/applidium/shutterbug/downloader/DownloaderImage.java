package com.applidium.shutterbug.downloader;

import android.graphics.Bitmap;

public class DownloaderImage {
    private Bitmap mBitmap;
    private byte[] mMovie;

    public DownloaderImage(Bitmap bitmap) {
        mBitmap = bitmap;
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
}
