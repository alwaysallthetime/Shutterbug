package com.applidium.shutterbug.downloader;

import android.os.AsyncTask;
import android.os.Build;

import com.applidium.shutterbug.utils.DownloadRequest;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ShutterbugDownloader {
    public interface ShutterbugDownloaderListener {
        void onImageDownloadSuccess(ShutterbugDownloader downloader, DownloaderInputStream inputStream, DownloadRequest downloadRequest);

        void onImageDownloadFailure(ShutterbugDownloader downloader, DownloadRequest downloadRequest);
    }

    private String                             mUrl;
    private ShutterbugDownloaderListener       mListener;
    private byte[]                             mImageData;
    private DownloadRequest                    mDownloadRequest;
    private final static int                   TIMEOUT = 30000;
    private AsyncTask<Void, Void, DownloaderInputStream> mCurrentTask;

    public ShutterbugDownloader(String url, ShutterbugDownloaderListener listener, DownloadRequest downloadRequest) {
        mUrl = url;
        mListener = listener;
        mDownloadRequest = downloadRequest;
    }

    public String getUrl() {
        return mUrl;
    }

    public ShutterbugDownloaderListener getListener() {
        return mListener;
    }

    public byte[] getImageData() {
        return mImageData;
    }

    public DownloadRequest getDownloadRequest() {
        return mDownloadRequest;
    }

    public void start() {
        mCurrentTask = new AsyncTask<Void, Void, DownloaderInputStream>() {

            @Override
            protected DownloaderInputStream doInBackground(Void... params) {
                HttpGet request = new HttpGet(mUrl);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");

                try {
                    URL imageUrl = new URL(mUrl);
                    HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                    connection.setConnectTimeout(TIMEOUT);
                    connection.setReadTimeout(TIMEOUT);
                    connection.setInstanceFollowRedirects(true);
                    InputStream inputStream = connection.getInputStream();

                    String contentType = null;
                    Map<String,List<String>> headerFields = connection.getHeaderFields();
                    List<String> contentTypes = headerFields.get("Content-Type");
                    if(contentTypes != null && contentTypes.size() == 1) {
                        contentType = contentTypes.get(0);
                    }
                    return new DownloaderInputStream(inputStream, contentType);

                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(DownloaderInputStream inputStream) {
                if (isCancelled()) {
                    inputStream = null;
                }

                if (inputStream != null) {
                    mListener.onImageDownloadSuccess(ShutterbugDownloader.this, inputStream, mDownloadRequest);
                } else {
                    mListener.onImageDownloadFailure(ShutterbugDownloader.this, mDownloadRequest);
                }
            }

        };
        // AsyncTask was changed in Honeycomb to execute in serial by default, at which time
        // executeOnExecutor was added to specify parallel execution.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mCurrentTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            mCurrentTask.execute();
        }
    }

    public void cancel() {
        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
    }
}
