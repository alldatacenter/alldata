package top.omooo.blackfish.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.DensityUtil;

/**
 * Created by SSC on 2018/3/30.
 */

public class WheelRecyclerView extends RecyclerView {

    private LinearLayoutManager mLinearLayoutManager;
    private List<String> mStringList;
    private int mItemHeight;
    private int mOffset = 1;
    private int mSelectTextColor = Color.parseColor("#222222");
    private int mUnselectTextColor=Color.parseColor("#999999");
    private float mSelectTextSize = DensityUtil.sp2px(14);
    private float mUnselectTextSize = DensityUtil.sp2px(14);
    private float mDividerWidth = ViewGroup.LayoutParams.MATCH_PARENT;
    private float mDividerHeight = DensityUtil.dp2px(1);
    private float mDividerColor = Color.parseColor("#E4F1FF");
    private Paint mPaint;
    private int mSelected;

    private WheelAdapter mAdapter;

    public WheelRecyclerView(Context context) {
        super(context);

        mStringList = new ArrayList<>();
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.colorLoginMessage));
        mPaint.setStrokeWidth(0.5f);

        init();
    }

    private void init() {
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(mLinearLayoutManager);
        addItemDecoration(new DividerItemDecoration());
        mAdapter = new WheelAdapter();
        setAdapter(mAdapter);

    }

    public WheelRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WheelRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width;
        int height;
        int heightSpecSize = MeasureSpec.getSize(heightSpec);
        int heightSpecMode = MeasureSpec.getMode(heightSpec);
        switch (heightSpecMode) {
            case MeasureSpec.EXACTLY:
                height = heightSpecSize;
                mItemHeight = height / (mOffset * 2 + 1);
                break;
            default:
                height = (mOffset * 2 + 1) * mItemHeight;
                break;
        }
        width = getDefaultSize(DensityUtil.dp2px(160), widthSpec);
        mDividerWidth = width;
        setMeasuredDimension(width, height);
    }

    public void setData(List<String> data) {
        if (null == data) {
            return;
        }
//        mStringList.clear();
        mStringList.addAll(data);
        mAdapter.notifyDataSetChanged();
        setSelect(0);
        scrollTo(0, 0);
    }

    public void setSelect(int position) {
        mSelected = position;
        mLinearLayoutManager.scrollToPosition(mSelected);
    }

    public int getSelected() {
        return mSelected;
    }

    private class WheelAdapter extends Adapter<WheelAdapter.WheelHolder> {


        @Override
        public WheelHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            WheelHolder holder = new WheelHolder(LayoutInflater.from(getContext()).inflate(R.layout.view_picker_item_layout, parent, false));
            holder.mTextView.getLayoutParams().height = mItemHeight;
            return holder;
        }

        @Override
        public void onBindViewHolder(WheelHolder holder, int position) {
            //头尾填充offset个空白View使数据能处于中间选中状态
            if (position < mOffset || position > mStringList.size() + mOffset - 1) {
                holder.mTextView.setText("");
            } else {
                holder.mTextView.setText(mStringList.get(position - mOffset));
            }
        }

        @Override
        public int getItemCount() {
            return mStringList.size() + mOffset * 2;
        }

        class WheelHolder extends ViewHolder {
            TextView mTextView;
            public WheelHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.tv_picker_item);
            }
        }
    }

    private class DividerItemDecoration extends ItemDecoration {
        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            //绘制分割线
            float startX = getMeasuredWidth() / 2 - mDividerWidth / 2;
            float topY = mItemHeight * mOffset;
            float endX = getMeasuredWidth() / 2 + mDividerWidth / 2;
            float bottomY = mItemHeight * (mOffset + 1);

            c.drawLine(startX, topY, endX, topY, mPaint);
            c.drawLine(startX, bottomY, endX, bottomY, mPaint);
        }
    }
}
