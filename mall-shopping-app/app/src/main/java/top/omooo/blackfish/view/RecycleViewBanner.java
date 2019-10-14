package top.omooo.blackfish.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/4/6.
 */

public class RecycleViewBanner extends FrameLayout {

    private static final int DEFAULT_SELECTED_COLOR = 0xffffffff;
    private static final int DEFAULT_UNSELECTED_COLOR = 0x50ffffff;

    private int mInterval;  //时间间隔
    private boolean isShowIndicator;
    private Drawable mSelectedDrawable;
    private Drawable mUnselectedDrawable;
    private int mSize;
    private int mSpace;

    private RecyclerView mRecyclerView;
    private LinearLayout mLinearLayout;

    private RecyclerAdapter adapter;
    private OnSwitchRvBannerListener mOnSwitchRvBannerListener;
    private OnRvBannerClickListener mBannerClickListener;
    private List<Object> mData = new ArrayList<>();
    private int startX,startY, currentIndex;
    private boolean isPlaying;
    private Handler mHandler = new Handler();
    private boolean isTouched;
    private boolean isAutoPlaying = true;

    private Runnable playTask=new Runnable() {
        @Override
        public void run() {
            mRecyclerView.smoothScrollToPosition(++currentIndex);
            if (isShowIndicator) {
                switchIndicator();
            }
            mHandler.postDelayed(this, mInterval);
        }
    };

    public RecycleViewBanner(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RecycleViewBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public RecycleViewBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.RecycleViewBanner);
        mInterval = typedArray.getInt(R.styleable.RecycleViewBanner_rvb_interval, 3000);
        isShowIndicator = typedArray.getBoolean(R.styleable.RecycleViewBanner_rvb_showIndicator, true);
        isAutoPlaying = typedArray.getBoolean(R.styleable.RecycleViewBanner_rvb_autoPlaying, true);
        Drawable selectedSrc = typedArray.getDrawable(R.styleable.RecycleViewBanner_rvb_indicatorSelectedSrc);
        Drawable unSelectedSrc = typedArray.getDrawable(R.styleable.RecycleViewBanner_rvb_indicatorUnselectedSrc);
        if (null == selectedSrc) {
            mSelectedDrawable = generateDefaultDrawable(DEFAULT_SELECTED_COLOR);
        } else {
            if (selectedSrc instanceof ColorDrawable) {
                mSelectedDrawable = generateDefaultDrawable(((ColorDrawable) selectedSrc).getColor());
            } else {
                mSelectedDrawable = selectedSrc;
            }
        }
        if (null == unSelectedSrc) {
            mUnselectedDrawable = generateDefaultDrawable(DEFAULT_UNSELECTED_COLOR);
        } else {
            if (unSelectedSrc instanceof ColorDrawable) {
                mUnselectedDrawable = generateDefaultDrawable(((ColorDrawable) unSelectedSrc).getColor());
            } else {
                mUnselectedDrawable = unSelectedSrc;
            }
        }
        mSize = typedArray.getDimensionPixelSize(R.styleable.RecycleViewBanner_rvb_indicatorSize, 0);
        mSpace = typedArray.getDimensionPixelSize(R.styleable.RecycleViewBanner_rvb_indicatorSpace, dp2px(4));
        int margin = typedArray.getDimensionPixelSize(R.styleable.RecycleViewBanner_rvb_indicatorMargin, dp2px(10));
        int g = typedArray.getInt(R.styleable.RecycleViewBanner_rvb_indicatorGravity, 1);
        int gravity;
        if (g == 0) {
            gravity = GravityCompat.START;
        } else if (g == 2) {
            gravity = GravityCompat.END;
        } else{
            gravity = Gravity.CENTER;
        }
        typedArray.recycle();

        mRecyclerView = new RecyclerView(context);
        mLinearLayout = new LinearLayout(context);

        new PagerSnapHelper().attachToRecyclerView(mRecyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        adapter = new RecyclerAdapter();
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int first = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    int last = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    if (first == last && currentIndex != last) {
                        currentIndex = last;
                        if (isShowIndicator && isTouched) {
                            isTouched = false;
                            switchIndicator();
                        }
                    }
                }
            }
        });
        mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        mLinearLayout.setGravity(Gravity.CENTER);

        LayoutParams vpLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        LayoutParams linearLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.gravity = Gravity.BOTTOM | gravity;
        linearLayoutParams.setMargins(margin, margin, margin, margin);
        addView(mRecyclerView, vpLayoutParams);
        addView(mLinearLayout, linearLayoutParams);
    }

    /**
     *  默认指示器是一系列直径为6dp的小圆点
     */
    private GradientDrawable generateDefaultDrawable(int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setSize(dp2px(6), dp2px(6));
        gradientDrawable.setCornerRadius(dp2px(6));
        gradientDrawable.setColor(color);
        return gradientDrawable;
    }

    public void isShowIndicator(boolean isShow) {
        this.isShowIndicator = isShow;
    }

    public void setIndicatorInterval(int m) {
        this.mInterval = m;
    }

    private synchronized void setPlaying(boolean playing) {
        if (isAutoPlaying) {
            if (!isPlaying && playing && adapter != null && adapter.getItemCount() > 2) {
                mHandler.postDelayed(playTask, mInterval);
                isPlaying = true;
            } else if (isPlaying && !playing) {
                mHandler.removeCallbacksAndMessages(null);
                isPlaying = false;
            }
        }
    }

    public void setRvBannerData(List data) {
        setPlaying(false);
        mData.clear();
        if (null != data) {
            mData.addAll(data);
        }
        if (mData.size() > 1) {
            currentIndex = mData.size();
            adapter.notifyDataSetChanged();
            mRecyclerView.scrollToPosition(currentIndex);
            if (isShowIndicator) {
                createIndicators();
            }
            setPlaying(true);
        } else {
            currentIndex = 0;
            adapter.notifyDataSetChanged();
        }
    }

    private void createIndicators() {
        mLinearLayout.removeAllViews();
        for (int i = 0; i < mData.size(); i++) {
            ImageView imageView = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = mSpace / 2;
            lp.rightMargin = mSpace / 2;
            if (mSize >= dp2px(4)) {
                lp.width = lp.height = mSize;
            } else {
                imageView.setMinimumHeight(dp2px(2));
                imageView.setMinimumHeight(dp2px(2));
            }
            imageView.setImageDrawable(i == 0 ? mSelectedDrawable : mUnselectedDrawable);
            mLinearLayout.addView(imageView, lp);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //手指触摸的时候自动停止播放，根据手势变换
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) ev.getX();
                startY = (int) ev.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getX();
                int moveY = (int) ev.getY();
                int disX = moveX - startX;
                int disY = moveY - startY;
                boolean hasMoved = 2 * Math.abs(disX) > Math.abs(disX);
                getParent().requestDisallowInterceptTouchEvent(hasMoved);
                if (hasMoved) {
                    setPlaying(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isPlaying) {
                    isTouched = true;
                    setPlaying(true);
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPlaying(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setPlaying(false);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == GONE || visibility == INVISIBLE) {
            setPlaying(false);
        } else if (visibility == VISIBLE) {
            setPlaying(true);
        }
        super.onWindowVisibilityChanged(visibility);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    class RecyclerAdapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            SimpleDraweeView imageView = new SimpleDraweeView(parent.getContext());
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(params);
            imageView.setId(R.id.rvb_banner_image_view_id);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mBannerClickListener) {
                        mBannerClickListener.onClick(currentIndex % mData.size());
                    }
                }
            });
            return new RecyclerView.ViewHolder(imageView) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            SimpleDraweeView draweeView = holder.itemView.findViewById(R.id.rvb_banner_image_view_id);
            if (null != mOnSwitchRvBannerListener) {
                mOnSwitchRvBannerListener.switchBanner(position % mData.size(), draweeView);
            }
        }

        @Override
        public int getItemCount() {
            return mData == null ? 0 : mData.size() < 2 ? mData.size() : Integer.MAX_VALUE;
        }
    }

    private class PagerSnapHelper extends LinearSnapHelper {
        /**
         * 通过当前滑动速度，计算最终定位的position
         */
        @Override
        public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
            // TODO: 2018/4/6 还未理解 targetPos currentView 的含义
            int targetPos = super.findTargetSnapPosition(layoutManager, velocityX, velocityY);
            View currentView = findSnapView(layoutManager);
            if (targetPos != RecyclerView.NO_POSITION && null != currentView) {
                int currentPos = layoutManager.getPosition(currentView);
                int first = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                int last = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                currentPos = targetPos < currentPos ? last : (targetPos > currentPos ? first : currentPos);
                targetPos = targetPos < currentPos ? currentPos - 1 : (targetPos > currentPos ? currentPos + 1 : currentPos);
            }
            return targetPos;
        }
    }

    private void switchIndicator() {
        if (null != mLinearLayout && mLinearLayout.getChildCount() > 0) {
            for (int i = 0; i < mLinearLayout.getChildCount(); i++) {
                ((ImageView) mLinearLayout.getChildAt(i)).setImageDrawable(i == currentIndex % mData.size() ? mSelectedDrawable : mUnselectedDrawable);
            }
        }
    }

    public interface OnSwitchRvBannerListener {
        void switchBanner(int position, SimpleDraweeView simpleDraweeView);
    }

    public void setOnSwitchRvBannerListener(OnSwitchRvBannerListener listener) {
        this.mOnSwitchRvBannerListener = listener;
    }

    public interface OnRvBannerClickListener {
        void onClick(int position);
    }

    public void setOnBannerClickListener(OnRvBannerClickListener listener) {
        this.mBannerClickListener = listener;
    }
}
