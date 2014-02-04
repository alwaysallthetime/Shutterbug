package com.applidium.shutterbug;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.applidium.shutterbug.downloader.DownloaderImage;
import com.applidium.shutterbug.utils.CustomCacheKeyObject;
import com.applidium.shutterbug.utils.ShutterbugManager;
import com.applidium.shutterbug.utils.ShutterbugManager.ShutterbugManagerListener;

public class FetchableImageView extends ImageView implements ShutterbugManagerListener {
    public interface FetchableImageViewListener {
        void onImageFetched(Bitmap bitmap, String url);

        void onImageFailure(String url);
    }

    private FetchableImageViewListener mListener;
    private int                        mMaxWidth;
    private int                        mMaxHeight;

    public FetchableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FetchableImageViewListener getListener() {
        return mListener;
    }

    public void setListener(FetchableImageViewListener listener) {
        mListener = listener;
    }

    public void setMaxImageSize(int maxWidth, int maxHeight) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    public void setImage(String url) {
        setImage(url, new ColorDrawable(getContext().getResources().getColor(R.color.transparent)));
    }

    public void setImage(String url, int placeholderDrawableId) {
        setImage(url, getContext().getResources().getDrawable(placeholderDrawableId));
    }

    public void setImage(CustomCacheKeyObject object, Drawable placeholderDrawable) {
        final ShutterbugManager manager = ShutterbugManager.getSharedImageManager(getContext());
        manager.cancel(this);
        setImageDrawable(placeholderDrawable);
        if (object != null) {
            manager.download(object, mMaxWidth, mMaxHeight, this);
        }
    }

    public void setImage(String url, Drawable placeholderDrawable) {
        final ShutterbugManager manager = ShutterbugManager.getSharedImageManager(getContext());
        manager.cancel(this);
        setImageDrawable(placeholderDrawable);
        if (url != null) {
            manager.download(url, mMaxWidth, mMaxHeight, this);
        }
    }

    public void cancelCurrentImageLoad() {
        ShutterbugManager.getSharedImageManager(getContext()).cancel(this);
    }

    @Override
    public void onImageSuccess(ShutterbugManager imageManager, DownloaderImage downloaderImage, String url) {
        setImageBitmap(downloaderImage.getBitmap());
        requestLayout();
        if (mListener != null) {
            mListener.onImageFetched(downloaderImage.getBitmap(), url);
        }
    }

    @Override
    public void onImageFailure(ShutterbugManager imageManager, String url) {
        if (mListener != null) {
            mListener.onImageFailure(url);
        }
    }

}
