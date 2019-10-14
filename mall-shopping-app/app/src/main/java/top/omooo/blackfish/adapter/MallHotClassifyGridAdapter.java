package top.omooo.blackfish.adapter;

import android.content.Context;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.MallHotClassifyGridInfo;
import top.omooo.blackfish.listener.OnViewItemClickListener;
import top.omooo.blackfish.utils.SpannableStringUtil;

/**
 * Created by SSC on 2018/4/8.
 */

public class MallHotClassifyGridAdapter extends BaseAdapter {

    private Context mContext;
    private List<MallHotClassifyGridInfo> mMallHotClassifyGridInfos;
    private SpannableStringUtil mStringUtil = new SpannableStringUtil();

    private static final String TAG = "MallGridAdapter";

    public MallHotClassifyGridAdapter(Context context, List<MallHotClassifyGridInfo> mallHotClassifyGridInfos) {
        mContext = context;
        mMallHotClassifyGridInfos = mallHotClassifyGridInfos;
    }

    @Override
    public int getCount() {
        return mMallHotClassifyGridInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mMallHotClassifyGridInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder myViewHolder;
        if (convertView == null) {
            myViewHolder = new MyViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.mall_pager_hot_classify_grid_item_layout, parent, false);
            myViewHolder.heraderImage = convertView.findViewById(R.id.iv_hot_classify_goods_image);
            myViewHolder.mTextDesc = convertView.findViewById(R.id.tv_hot_classify_goods_desc);
            myViewHolder.mTextPeriods = convertView.findViewById(R.id.tv_hot_classify_goods_periods);
            myViewHolder.mTextPrice = convertView.findViewById(R.id.tv_hot_classify_goods_price);
            myViewHolder.mLinearLayout = convertView.findViewById(R.id.ll_goods_item);
            convertView.setTag(myViewHolder);
        } else {
            myViewHolder = (MyViewHolder) convertView.getTag();
        }
        myViewHolder.heraderImage.setImageURI(mMallHotClassifyGridInfos.get(position).getHeaderImageUrl());
        myViewHolder.mTextDesc.setText(mMallHotClassifyGridInfos.get(position).getGoodsDesc());
        myViewHolder.mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnViewItemClickListener.onItemClick("HotGoodsItem");
            }
        });
        String text = mMallHotClassifyGridInfos.get(position).getGoodsPeriods();
        int spaceIndex = getFirstSpaceIndex(text.toCharArray());
        Log.i(TAG, "getView: " + spaceIndex);
        SpannableString periods = mStringUtil.setMallGoodsPrice(text, 0, spaceIndex);
        myViewHolder.mTextPeriods.setText(periods);
        myViewHolder.mTextPrice.setText(mMallHotClassifyGridInfos.get(position).getGoodsPrice());

        return convertView;
    }

    private int getFirstSpaceIndex(char[] text) {

        for (int i = 0; i < text.length; i++) {
            if (text[i] == ' ') {
                return i;
            }
        }
        return 0;
    }

    private class MyViewHolder {
        private LinearLayout mLinearLayout;
        private SimpleDraweeView heraderImage;
        private TextView mTextDesc;
        private TextView mTextPeriods;
        private TextView mTextPrice;
    }

    private OnViewItemClickListener mOnViewItemClickListener;

    public void setOnViewItemClickListener(OnViewItemClickListener listener) {
        this.mOnViewItemClickListener = listener;
    }
}
