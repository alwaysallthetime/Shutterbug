package com.applidium.shutterbug.cache;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.applidium.shutterbug.cache.DiskLruCache.Editor;
import com.applidium.shutterbug.cache.DiskLruCache.Snapshot;
import com.applidium.shutterbug.downloader.DownloaderImage;
import com.applidium.shutterbug.utils.DownloadRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageCache {
    private static final String TAG = "ImageCache";

    public interface ImageCacheListener {
        void onImageFound(ImageCache imageCache, DownloaderImage downloaderImage, String key, DownloadRequest downloadRequest);
        void onImageNotFound(ImageCache imageCache, String key, DownloadRequest downloadRequest);
    }

    // 1 entry per key
    private final static int         DISK_CACHE_VALUE_COUNT = 2;
    // 100 MB of disk cache
    private final static int         DISK_CACHE_MAX_SIZE    = 100 * 1024 * 1024;

    private static ImageCache        sImageCache;
    private Context                  mContext;
    private LruCache<String, DownloaderImage> mMemoryCache;
    private DiskLruCache             mDiskCache;

    ImageCache(Context context) {
        mContext = context;
        // Get memory class of this device, exceeding this amount will throw an
        // OutOfMemory exception.
        final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = 1024 * 1024 * memClass / 8;

        mMemoryCache = new LruCache<String, DownloaderImage>(cacheSize) {
            @Override
            protected int sizeOf(String key, DownloaderImage downloaderImage) {
                if(downloaderImage.isBitmap()) {
                // The cache size will be measured in bytes rather than number
                // of items.
                    Bitmap bitmap = downloaderImage.getBitmap();
                    return bitmap.getRowBytes() * bitmap.getHeight();
                } else {
                    return downloaderImage.getMovieBytes().length;
                }
            }
        };

        openDiskCache();
    }

    public static ImageCache getSharedImageCache(Context context) {
        if (sImageCache == null) {
            sImageCache = new ImageCache(context);
        }
        return sImageCache;
    }

    public void queryCache(String cacheKey, ImageCacheListener listener, DownloadRequest downloadRequest) {
        if (cacheKey == null) {
            listener.onImageNotFound(this, cacheKey, downloadRequest);
            return;
        }

        // First check the in-memory cache...
        Log.d(TAG, "checking for " + cacheKey);
        DownloaderImage cachedDownloaderImage = mMemoryCache.get(cacheKey);

        if (cachedDownloaderImage != null) {
            // ...notify listener immediately, no need to go async
            listener.onImageFound(this, cachedDownloaderImage, cacheKey, downloadRequest);
            return;
        }

        if (mDiskCache != null) {
            new BitmapDecoderTask(cacheKey, listener, downloadRequest).execute();
            return;
        }
        listener.onImageNotFound(this, cacheKey, downloadRequest);
    }

    public Snapshot storeToDisk(InputStream inputStream, String cacheKey) {
        try {
            Editor editor = mDiskCache.edit(cacheKey);
            final OutputStream outputStream = editor.newOutputStream(0);
            final int bufferSize = 1024;
            try {
                byte[] bytes = new byte[bufferSize];
                for (;;) {
                    int count = inputStream.read(bytes, 0, bufferSize);
                    if (count == -1)
                        break;
                    outputStream.write(bytes, 0, count);
                }
                outputStream.close();
                editor.commit();
                return mDiskCache.get(cacheKey);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage(), e);
            }
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return null;
    }

    public Snapshot storeToDisk(DownloaderImage downloaderImage, String cacheKey) {
        try {
            boolean isBitmap = downloaderImage.isBitmap();
            Editor editor = mDiskCache.edit(cacheKey);
            editor.set(0, isBitmap ? "1" : "0");
            final OutputStream outputStream = editor.newOutputStream(1);
            try {
                if(isBitmap) {
                    downloaderImage.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                } else {
                    outputStream.write(downloaderImage.getMovieBytes());
                }
                outputStream.close();
                editor.commit();
                return mDiskCache.get(cacheKey);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public void storeToMemory(DownloaderImage downloaderImage, String cacheKey) {
        mMemoryCache.put(cacheKey, downloaderImage);
    }

    public void clear() {
        try {
            mDiskCache.delete();
            openDiskCache();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        mMemoryCache.evictAll();
    }

    private class BitmapDecoderTask extends AsyncTask<Void, Void, DownloaderImage> {
        private String             mCacheKey;
        private ImageCacheListener mListener;
        private DownloadRequest    mDownloadRequest;

        public BitmapDecoderTask(String cacheKey, ImageCacheListener listener, DownloadRequest downloadRequest) {
            mCacheKey = cacheKey;
            mListener = listener;
            mDownloadRequest = downloadRequest;
        }

        @Override
        protected DownloaderImage doInBackground(Void... params) {
            try {
                Snapshot snapshot = mDiskCache.get(mCacheKey);
                if (snapshot != null) {
                    try {
                        String string = snapshot.getString(0);
                        if("1".equals(string)) {
                            return new DownloaderImage(BitmapFactory.decodeStream(snapshot.getInputStream(1)));
                        } else {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int read;
                            byte[] input = new byte[4096];
                            while ( -1 != ( read = snapshot.getInputStream(1).read(input) ) ) {
                                buffer.write( input, 0, read );
                            }
                            return new DownloaderImage(buffer.toByteArray());
                        }
                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, e.getMessage(), e);
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(DownloaderImage result) {
            if (result != null) {
                storeToMemory(result, mCacheKey);
                mListener.onImageFound(ImageCache.this, result, mCacheKey, mDownloadRequest);
            } else {
                mListener.onImageNotFound(ImageCache.this, mCacheKey, mDownloadRequest);
            }
        }

    }
    
    private void openDiskCache() {
        File directory;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            directory = new File(android.os.Environment.getExternalStorageDirectory(), "Applidium Image Cache");
        } else {
            directory = mContext.getCacheDir();
        }
        int versionCode;
        try {
            versionCode = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            versionCode = 0;
            Log.e(TAG, e.getMessage(), e);
        }
        try {
            mDiskCache = DiskLruCache.open(directory, versionCode, DISK_CACHE_VALUE_COUNT, DISK_CACHE_MAX_SIZE);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
