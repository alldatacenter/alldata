package com.platform.app.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.bean.HotGoods;
import com.platform.app.utils.CartShopProvider;
import com.platform.app.utils.GlideUtils;
import com.platform.app.utils.ToastUtils;

import java.util.List;

/**
 * Created by wulinhao
 * Time  2019/8/8
 * Describe: 热卖商品的适配器
 */

public class HotGoodsAdapter extends RecyclerView.Adapter<HotGoodsAdapter.ViewHolder> implements
        View.OnClickListener {

    private List<HotGoods.ListBean> mDatas;
    private LayoutInflater          mInflater;
    CartShopProvider provider;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener = null;

    public HotGoodsAdapter(List<HotGoods.ListBean> datas, Context context) {
        this.mDatas = datas;
        this.mContext = context;
        provider = new CartShopProvider(mContext);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mInflater = LayoutInflater.from(parent.getContext());
        View view = mInflater.inflate(R.layout.template_hot_wares, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HotGoodsAdapter.ViewHolder holder, int position) {
        final HotGoods.ListBean data = getData(position);
        GlideUtils.load(MallShoppingApp.sContext, data.getImgUrl(), holder.ivView);
        holder.textTitle.setText(data.getName());
        holder.textPrice.setText("￥" + data.getPrice());
        holder.itemView.setTag(position);

        if (holder.mButton != null) {
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    ShoppingCart cart = convertData(data);
                    provider.put(data);
                    ToastUtils.showSafeToast(mContext,"已添加到购物车");
                }
            });
        }
    }


    private HotGoods.ListBean getData(int position) {
        return mDatas.get(position);
    }

    public List<HotGoods.ListBean> getDatas() {
        return mDatas;
    }

    public void clearData() {
        mDatas.clear();
        notifyItemRangeRemoved(0, mDatas.size());
    }

    public void addData(List<HotGoods.ListBean> datas) {
        addData(0, datas);
    }

    public void addData(int position, List<HotGoods.ListBean> datas) {
        if (datas != null && datas.size() > 0) {
            mDatas.addAll(datas);
            notifyItemRangeChanged(position, mDatas.size());
        }

    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    @Override
    public void onClick(View v) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(v, (int) v.getTag());
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivView;
        TextView  textTitle;
        TextView  textPrice;
        Button    mButton;

        public ViewHolder(View itemView) {
            super(itemView);

            ivView = (ImageView) itemView.findViewById(R.id.iv_view);
            textTitle = (TextView) itemView.findViewById(R.id.text_title);
            textPrice = (TextView) itemView.findViewById(R.id.text_price);
            mButton = (Button) itemView.findViewById(R.id.btn_add);
        }
    }


    /**
     * item的点击事件
     */
    public static interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    /**
     * 暴露给外面,以便于调用
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }


}
