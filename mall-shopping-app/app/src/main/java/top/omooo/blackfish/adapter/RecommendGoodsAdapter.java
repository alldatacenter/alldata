package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.RecommendGoodsInfo;
import top.omooo.blackfish.utils.SpannableStringUtil;

/**
 * Created by SSC on 2018/4/11.
 */

public class RecommendGoodsAdapter extends RecyclerView.Adapter<RecommendGoodsAdapter.MyViewHolder> {


    private Context mContext;
    private List<RecommendGoodsInfo> mGoodsInfos;
    private SpannableStringUtil mStringUtil;

    public RecommendGoodsAdapter(Context context, List<RecommendGoodsInfo> goodsInfos) {
        mGoodsInfos = new ArrayList<>();
        mContext = context;
        mGoodsInfos = goodsInfos;
        mStringUtil = new SpannableStringUtil();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_recommend_goods_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        RecommendGoodsInfo goodsInfo = mGoodsInfos.get(position);
        holder.mDraweeView.setImageURI(goodsInfo.getImageUrl());
        holder.mTextDesc.setText(goodsInfo.getDesc());
        String price = "¥" + goodsInfo.getSinglePrice() + " x" + goodsInfo.getPeriods() + "期" + "    ¥" + goodsInfo.getTotalPrice();
        SpannableString spannableString = mStringUtil.setMallGoodsPrice(price, 0, getFirstSpaceIndex(price.toCharArray()));
        holder.mTextPrice.setText(spannableString);
        String evaluation = "好评率:" + goodsInfo.getEvaluation();
        holder.mTextEvaluation.setText(evaluation);
    }

    @Override
    public int getItemCount() {
        return mGoodsInfos.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private SimpleDraweeView mDraweeView;
        private TextView mTextDesc;
        private TextView mTextPrice;
        private TextView mTextEvaluation;
        private MyViewHolder(View itemView) {
            super(itemView);
            mDraweeView = itemView.findViewById(R.id.iv_mall_recommend_goods_image);
            mTextDesc = itemView.findViewById(R.id.tv_mall_recommend_goods_desc);
            mTextPrice = itemView.findViewById(R.id.tv_mall_recommend_goods_price);
            mTextEvaluation = itemView.findViewById(R.id.tv_mall_recommend_goods_evaluation);
        }
    }

    private int getFirstSpaceIndex(char[] text) {

        for (int i = 0; i < text.length; i++) {
            if (text[i] == ' ') {
                return i;
            }
        }
        return 0;
    }
}
