package com.applidium.shutterbug.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.applidium.shutterbug.cache.ImageCache;
import com.applidium.shutterbug.cache.ImageCache.ImageCacheListener;
import com.applidium.shutterbug.downloader.DownloaderImage;
import com.applidium.shutterbug.downloader.DownloaderInputStream;
import com.applidium.shutterbug.downloader.ShutterbugDownloader;
import com.applidium.shutterbug.downloader.ShutterbugDownloader.ShutterbugDownloaderListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShutterbugManager implements ImageCacheListener, ShutterbugDownloaderListener {


    public interface ShutterbugManagerListener {
        void onImageSuccess(ShutterbugManager imageManager, DownloaderImage downloaderImage, String url);
        void onImageFailure(ShutterbugManager imageManager, String url);
    }

    private static ShutterbugManager          sImageManager;

    private Context                           mContext;
    private ImageCache                        mImageCache;
    private List<String>                      mFailedUrls             = new ArrayList<String>();
    private List<ShutterbugManagerListener>   mCacheListeners         = new ArrayList<ShutterbugManagerListener>();
    private List<String>                      mCacheUrls              = new ArrayList<String>();
    private Map<String, ShutterbugDownloader> mDownloadersMap         = new HashMap<String, ShutterbugDownloader>();
    private List<DownloadRequest>             mDownloadRequests       = new ArrayList<DownloadRequest>();
    private List<ShutterbugManagerListener>   mDownloadImageListeners = new ArrayList<ShutterbugManagerListener>();
    private List<ShutterbugDownloader>        mDownloaders            = new ArrayList<ShutterbugDownloader>();

    final static private int                  LISTENER_NOT_FOUND      = -1;

    public ShutterbugManager(Context context, int diskCacheSize) {
        mContext = context;
        mImageCache = ImageCache.getSharedImageCache(context, diskCacheSize);
    }

    public ShutterbugManager(Context context) {
        mContext = context;
        mImageCache = ImageCache.getSharedImageCache(context);
    }

    public static ShutterbugManager getSharedImageManager(Context context) {
        if (sImageManager == null) {
            sImageManager = new ShutterbugManager(context);
        }
        return sImageManager;
    }

    /**
     * Call this before ShutterbugManager (or FetchableImageView) is used.
     *
     * @param context
     * @param diskCacheSize
     */
    public static void setDiskCacheSize(Context context, int diskCacheSize) {
        sImageManager = new ShutterbugManager(context, diskCacheSize);
    }

    public ImageCache getImageCache() {
        return mImageCache;
    }

    public void download(String url, ShutterbugManagerListener listener) {
        download(url, 0, 0, listener);
    }

    public void download(final CustomCacheKeyObject object, int maxWidth, int maxHeight, ShutterbugManagerListener listener) {
        if (object == null || listener == null || mFailedUrls.contains(object.getUrl())) {
            return;
        }

        mCacheListeners.add(listener);
        mCacheUrls.add(object.getUrl());
        CustomCacheKeyDownloadRequest downloadRequest = new CustomCacheKeyDownloadRequest(object, maxWidth, maxHeight, listener);
        mImageCache.queryCache(getCacheKey(downloadRequest, maxWidth, maxHeight), this, downloadRequest);
    }

    public void download(String url, int maxWidth, int maxHeight, ShutterbugManagerListener listener) {
        if (url == null || listener == null || mFailedUrls.contains(url)) {
            return;
        }

        mCacheListeners.add(listener);
        mCacheUrls.add(url);
        DownloadRequest downloadRequest = new DownloadRequest(url, maxWidth, maxHeight, listener);
        mImageCache.queryCache(getCacheKey(downloadRequest, maxWidth, maxHeight), this, downloadRequest);
    }

    public static String getCacheKey(DownloadRequest downloadRequest, int maxWidth, int maxHeight) {
        try {
            String key = downloadRequest.getCacheKeyPrefix() + "w=" + maxWidth + "&h=" + maxHeight;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(key.getBytes("UTF-8"), 0, key.length());
            return String.format("%x", new BigInteger(md.digest()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getListenerIndex(ShutterbugManagerListener listener, String url) {
        for (int index = 0; index < mCacheListeners.size(); index++) {
            if (mCacheListeners.get(index) == listener && mCacheUrls.get(index).equals(url)) {
                return index;
            }
        }
        return LISTENER_NOT_FOUND;
    }

    @Override
    public void onImageFound(ImageCache imageCache, DownloaderImage downloaderImage, String key, DownloadRequest downloadRequest) {
        final String url = downloadRequest.getUrl();
        final ShutterbugManagerListener listener = downloadRequest.getListener();

        int idx = getListenerIndex(listener, url);
        if (idx == LISTENER_NOT_FOUND) {
            // Request has since been canceled
            return;
        }

        listener.onImageSuccess(this, downloaderImage, url);
        mCacheListeners.remove(idx);
        mCacheUrls.remove(idx);
    }

    @Override
    public void onImageNotFound(ImageCache imageCache, String key, DownloadRequest downloadRequest) {
        final String url = downloadRequest.getUrl();
        final ShutterbugManagerListener listener = downloadRequest.getListener();

        int idx = getListenerIndex(listener, url);
        if (idx == LISTENER_NOT_FOUND) {
            // Request has since been canceled
            return;
        }
        mCacheListeners.remove(idx);
        mCacheUrls.remove(idx);

        // Share the same downloader for identical URLs so we don't download the
        // same URL several times
        ShutterbugDownloader downloader = mDownloadersMap.get(url);
        if (downloader == null) {
            downloader = new ShutterbugDownloader(url, this, downloadRequest);
            downloader.start();
            mDownloadersMap.put(url, downloader);
        }
        mDownloadRequests.add(downloadRequest);
        mDownloadImageListeners.add(listener);
        mDownloaders.add(downloader);
    }

    @Override
    public void onImageDownloadSuccess(final ShutterbugDownloader downloader, final DownloaderInputStream inputStream,
            final DownloadRequest downloadRequest) {
        new InputStreamHandlingTask(downloader, downloadRequest).execute(inputStream);
    }

    @Override
    public void onImageDownloadFailure(ShutterbugDownloader downloader, DownloadRequest downloadRequest) {
        for (int idx = mDownloaders.size() - 1; idx >= 0; idx--) {
            final int uidx = idx;
            ShutterbugDownloader aDownloader = mDownloaders.get(uidx);
            if (aDownloader == downloader) {
                ShutterbugManagerListener listener = mDownloadImageListeners.get(uidx);
                listener.onImageFailure(this, downloadRequest.getUrl());
                mDownloaders.remove(uidx);
                mDownloadImageListeners.remove(uidx);
            }
        }
        mDownloadersMap.remove(downloadRequest.getUrl());
    }

    private class InputStreamHandlingTask extends AsyncTask<DownloaderInputStream, Void, DownloaderImage> {
        ShutterbugDownloader mDownloader;
        DownloadRequest      mDownloadRequest;

        InputStreamHandlingTask(ShutterbugDownloader downloader, DownloadRequest downloadRequest) {
            mDownloader = downloader;
            mDownloadRequest = downloadRequest;
        }

        @Override
        protected DownloaderImage doInBackground(DownloaderInputStream... params) {
            DownloaderInputStream downloaderInputStream = params[0];
            DownloaderImage downloaderImage = null;
            final ImageCache sharedImageCache = mImageCache;
            final int maxWidth = mDownloadRequest.getMaxWidth();
            final int maxHeight = mDownloadRequest.getMaxHeight();
            final String cacheKey = getCacheKey(mDownloadRequest, maxWidth, maxHeight);

            if(ImageCache.MIMETYPE_GIF.equals(downloaderInputStream.getMimetype())) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int read;
                byte[] input = new byte[4096];
                try {
                    while ( -1 != ( read = downloaderInputStream.getInputStream().read(input) ) ) {
                        buffer.write( input, 0, read );
                    }
                    downloaderImage = new DownloaderImage(buffer.toByteArray());
                } catch(IOException e) {
                    Log.d("ShutterbugManager", e.getMessage(), e);
                }
            } else {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(params[0].getInputStream());
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                }
                if (bitmap != null) {
                    if(maxWidth != 0 && maxHeight != 0) {
                        float width = bitmap.getWidth();
                        float height = bitmap.getHeight();

                        if(width > maxWidth || height > maxHeight) {
                            int newWidth = 0, newHeight = 0;
                            if(width > height) {
                                newWidth = (int) Math.min(width, maxWidth);;
                                newHeight = (int)((maxWidth / width) * height);
                            } else {
                                newHeight = (int) Math.min(height, maxHeight);
                                newWidth = (int)((maxHeight / height) * width);
                            }
                            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        }
                    }
                    downloaderImage = new DownloaderImage(bitmap, downloaderInputStream.getMimetype());
                }
            }

            if(downloaderImage != null) {
                sharedImageCache.storeToDisk(downloaderImage, cacheKey);
                sharedImageCache.storeToMemory(downloaderImage, cacheKey);
            }
            return downloaderImage;
        }

        @Override
        protected void onPostExecute(DownloaderImage downloaderImage) {
            // Notify all the downloadListener with this downloader
            for (int idx = mDownloaders.size() - 1; idx >= 0; idx--) {
                final int uidx = idx;
                ShutterbugDownloader aDownloader = mDownloaders.get(uidx);
                if (aDownloader == mDownloader) {
                    ShutterbugManagerListener listener = mDownloadImageListeners.get(uidx);
                    if (downloaderImage != null) {
                        listener.onImageSuccess(ShutterbugManager.this, downloaderImage, mDownloadRequest.getUrl());
                    } else {
                        listener.onImageFailure(ShutterbugManager.this, mDownloadRequest.getUrl());
                    }
                    mDownloaders.remove(uidx);
                    mDownloadImageListeners.remove(uidx);
                }
            }
            if (downloaderImage != null) {
            } else { // TODO add retry option
                mFailedUrls.add(mDownloadRequest.getUrl());
            }
            mDownloadersMap.remove(mDownloadRequest.getUrl());
        }

    }

    public void cancel(ShutterbugManagerListener listener) {
        int idx;
        while ((idx = mCacheListeners.indexOf(listener)) != -1) {
            mCacheListeners.remove(idx);
            mCacheUrls.remove(idx);
        }

        while ((idx = mDownloadImageListeners.indexOf(listener)) != -1) {
            ShutterbugDownloader downloader = mDownloaders.get(idx);

            mDownloadRequests.remove(idx);
            mDownloadImageListeners.remove(idx);
            mDownloaders.remove(idx);

            if (!mDownloaders.contains(downloader)) {
                // No more listeners are waiting for this download, cancel it
                downloader.cancel();
                mDownloadersMap.remove(downloader.getUrl());
            }
        }

    }
}
