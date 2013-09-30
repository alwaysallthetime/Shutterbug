package com.applidium.shutterbug;

import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.applidium.shutterbug.utils.ShutterbugManager;
import com.applidium.shutterbug.utils.ShutterbugManager.ShutterbugManagerListener;

public class FetchableImageView extends ImageView implements ShutterbugManagerListener {
    public interface FetchableImageViewListener {
        void onImageFetched(Bitmap bitmap, String url);

        void onImageFailure(String url);
    }

    private FetchableImageViewListener mListener;
    private int mMaxWidth;
    private int mMaxHeight;

    public FetchableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FetchableImageViewListener getListener() {
        return mListener;
    }

    public void setListener(FetchableImageViewListener listener) {
        mListener = listener;
    }

    public void setImage(String url) {
        setImage(url, new ColorDrawable(getContext().getResources().getColor(R.color.transparent)));
    }

    public void setImage(String url, int placeholderDrawableId) {
        setImage(url, getContext().getResources().getDrawable(placeholderDrawableId));
    }

    public void setImage(String url, Drawable placeholderDrawable) {
        final ShutterbugManager manager = ShutterbugManager.getSharedImageManager(getContext());
        manager.cancel(this);
        setImageDrawable(placeholderDrawable);
        if (url != null) {
            manager.download(url, this);
        }
    }

    public void cancelCurrentImageLoad() {
        ShutterbugManager.getSharedImageManager(getContext()).cancel(this);
    }

    public void setMaxSize(int width, int height) {
        mMaxWidth = width;
        mMaxHeight = height;
    }

    @Override
    public void onImageSuccess(ShutterbugManager imageManager, Bitmap bitmap, String url) {
        if(mMaxWidth != 0 && mMaxHeight != 0) {
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();

            int newWidth = 0, newHeight = 0;
            if(width > height) {
                newWidth = mMaxWidth;
                newHeight = (int)((mMaxWidth / width) * height);
            } else {
                newHeight = mMaxHeight;
                newWidth = (int)((mMaxHeight / height) * width);
            }
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
//            if(scaledBitmap != bitmap) {
//                bitmap.recycle();
//            }
            bitmap = scaledBitmap;
        }
        setImageBitmap(bitmap);
        requestLayout();
        if (mListener != null) {
            mListener.onImageFetched(bitmap, url);
        }
    }

    @Override
    public void onImageFailure(ShutterbugManager imageManager, String url) {
        if (mListener != null) {
            mListener.onImageFailure(url);
        }
    }

}
