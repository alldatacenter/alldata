package com.platform.app.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.bean.ShoppingCart;
import com.platform.app.fragment.ShopCartFragment;
import com.platform.app.utils.GlideUtils;
import com.platform.app.widget.NumberAddSubView;

import java.util.List;

/**
 * Created by wulinhao
 * Time  2019/9/9
 * Describe: 购物车的适配器
 */

public class ShopCartAdapter extends RecyclerView.Adapter<ShopCartAdapter.ViewHolder> {

    private Context            mContext;
    private List<ShoppingCart> mDatas;
    private CheckItemListener  mCheckListener;

    public ShopCartAdapter(Context mContext, List<ShoppingCart> mDatas, CheckItemListener
            mCheckListener) {
        this.mContext = mContext;
        this.mDatas = mDatas;
        this.mCheckListener = mCheckListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.template_cart, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ShoppingCart cart = mDatas.get(position);


        holder.mTvTitle.setText(cart.getName());
        holder.mTvPrice.setText("￥" + cart.getPrice());
        holder.mCheckBox.setChecked(cart.isChecked());
        holder.mNumberAddSubView.setValue(cart.getCount());
        GlideUtils.load(MallShoppingApp.sContext, cart.getImgUrl(), holder.mIvLogo);


        //点击实现选择功能，当然可以把点击事件放在item_cb对应的CheckBox上，只是焦点范围较小
        holder.mLlContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cart.setIsChecked(!cart.isChecked());
                holder.mCheckBox.setChecked(cart.isChecked());
                if (null != mCheckListener) {
                    mCheckListener.itemChecked(cart, holder.mCheckBox.isChecked());
                }
                notifyDataSetChanged();
                ((ShopCartFragment)mCheckListener).showTotalPrice();
            }
        });


        holder.mNumberAddSubView.setOnButtonClickListener(new NumberAddSubView.OnButtonClickListener() {
            @Override
            public void onButtonAddClick(View view, int value) {
                cart.setCount(value);
//                mCartShopProvider.updata(cart);
                ((ShopCartFragment)mCheckListener).showTotalPrice();
            }

            @Override
            public void onButtonSubClick(View view, int value) {
                cart.setCount(value);
//                mCartShopProvider.updata(cart);
                ((ShopCartFragment)mCheckListener).showTotalPrice();
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout     mLlContent;
        private TextView         mTvTitle;
        private TextView         mTvPrice;
        private CheckBox         mCheckBox;
        private ImageView        mIvLogo;
        private NumberAddSubView mNumberAddSubView;

        public ViewHolder(View itemView) {
            super(itemView);
            mLlContent = itemView.findViewById(R.id.ll_item);
            mCheckBox = itemView.findViewById(R.id.checkbox);
            mIvLogo = itemView.findViewById(R.id.iv_view);
            mTvTitle = itemView.findViewById(R.id.text_title);
            mTvPrice = itemView.findViewById(R.id.text_price);
            mNumberAddSubView = itemView.findViewById(R.id.num_control);
        }
    }

    public interface CheckItemListener {

        void itemChecked(ShoppingCart checkBean, boolean isChecked);
    }
}
