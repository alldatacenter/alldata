package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;

/**
 * Created by SSC on 2018/4/14.
 */

public class ShowImageAdapter extends PagerAdapter {

    private Context mContext;
    private String[] mStrings;
    private static final String TAG = "ShowImageAdapter";

    public ShowImageAdapter(Context context, String[] strings) {
        mContext = context;
        mStrings = strings;
    }

    @Override
    public int getCount() {
        return mStrings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        SimpleDraweeView draweeView = new SimpleDraweeView(mContext);
        draweeView.setImageURI(mStrings[position]);
        draweeView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        container.addView(draweeView);
        draweeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnImageClickListener.onImageClick();
            }
        });
        return draweeView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    private OnImageClickListener mOnImageClickListener;

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.mOnImageClickListener = listener;
    }
    public interface OnImageClickListener {
        void onImageClick();
    }
}
