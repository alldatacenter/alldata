package top.omooo.blackfish.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.SimilarRecoInfo;
import top.omooo.blackfish.utils.SpannableStringUtil;

/**
 * Created by SSC on 2018/4/13.
 */

public class SimilarRecoAdapter extends BaseAdapter {

    private Context mContext;
    private List<SimilarRecoInfo> mSimilarRecoInfos;
    private SpannableStringUtil mStringUtil = new SpannableStringUtil();

    private static final String TAG = "SimilarRecoAdapter";

    public SimilarRecoAdapter(Context context, List<SimilarRecoInfo> similarRecoInfos) {
        mContext = context;
        mSimilarRecoInfos = new ArrayList<>();
        mSimilarRecoInfos = similarRecoInfos;
    }

    @Override
    public int getCount() {
        return mSimilarRecoInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mSimilarRecoInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (null == convertView) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.goods_details_similar_reco, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        SimilarRecoInfo similarRecoInfo = mSimilarRecoInfos.get(position);
        viewHolder.mIvGoods.setImageURI(similarRecoInfo.getImageUrl());
        viewHolder.mTvDesc.setText(similarRecoInfo.getDesc());
        String singlePrice = "¥" + similarRecoInfo.getSinglePrice() + " x" + similarRecoInfo.getPeriods() + "期";
        viewHolder.mTvSinglePrice.setText(singlePrice);
        String price = "¥" + similarRecoInfo.getTotalPrice();
        viewHolder.mTvPrice.setText(price);
        Log.i(TAG, "getView: " + singlePrice + "  " + price);
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.iv_goods)
        SimpleDraweeView mIvGoods;
        @BindView(R.id.tv_desc)
        TextView mTvDesc;
        @BindView(R.id.tv_single_price)
        TextView mTvSinglePrice;
        @BindView(R.id.tv_price)
        TextView mTvPrice;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
