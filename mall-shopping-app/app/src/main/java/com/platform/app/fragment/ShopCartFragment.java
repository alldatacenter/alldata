package com.platform.app.fragment;

import android.content.Intent;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.platform.app.R;
import com.platform.app.activity.CreateOrderActivity;
import com.platform.app.adapter.ShopCartAdapter;
import com.platform.app.bean.MessageEvent;
import com.platform.app.bean.ShoppingCart;
import com.platform.app.utils.CartShopProvider;
import com.platform.app.widget.EnjoyshopToolBar;
import com.platform.app.widget.WrapContentLinearLayoutManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/09
 *     desc   : 购物车fragment
 *     version: 1.0
 * </pre>
 */

public class ShopCartFragment extends BaseFragment implements ShopCartAdapter.CheckItemListener {

    public static final  int    ACTION_EDIT     = 1;
    public static final  int    ACTION_CAMPLATE = 2;
    private static final String TAG             = "CartFragment";

    @BindView(R.id.recycler_view)
    RecyclerView     mRecyclerView;
    @BindView(R.id.checkbox_all)
    CheckBox         mCheckBox;
    @BindView(R.id.txt_total)
    TextView         mTextTotal;
    @BindView(R.id.btn_order)
    Button           mBtnOrder;
    @BindView(R.id.btn_del)
    Button           mBtnDel;
    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolbar;
    @BindView(R.id.rv_bottom)
    RelativeLayout   mRvBottom;
    @BindView(R.id.ll_empty)
    LinearLayout     mLlEmpty;

    private ShopCartAdapter    mAdapter;
    private CartShopProvider   mCartShopProvider;
    private boolean            isSelectAll;
    //列表数据
    private List<ShoppingCart> dataArray;
    //选中后的数据
    private List<ShoppingCart> checkedList;

    @Override
    protected int getContentResourseId() {
        return R.layout.fragment_shopcart;
    }

    @Override
    protected void init() {
        mCartShopProvider = new CartShopProvider(getContext());
        checkedList = new ArrayList<>();
        changeToolbar();
        showData();
        showTotalPrice();
    }


    /**
     * 改变标题栏
     */
    private void changeToolbar() {
        mToolbar.hideSearchView();
        mToolbar.showTitleView();
        mToolbar.setTitle(R.string.cart);
    }


    /**
     * 获取数据
     */


    private void showData() {

        dataArray = mCartShopProvider.getAll();

        if (dataArray == null) {
            initEmptyView();           //如果数据为空,显示空的试图
            return;
        }

        /**
         * 购物车数据不为空
         */
        mAdapter = new ShopCartAdapter(getContext(), dataArray, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new WrapContentLinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

    }




    private void initEmptyView() {
        mRvBottom.setVisibility(View.GONE);
        mLlEmpty.setVisibility(View.VISIBLE);
    }


    @OnClick({R.id.btn_del, R.id.btn_order, R.id.tv_goshop, R.id.checkbox_all})
    public void viewClick(View view) {
        switch (view.getId()) {
            case R.id.btn_del:
                //                mAdapter.delCart();
                break;
            case R.id.btn_order:
                Intent intent = new Intent(getContext(), CreateOrderActivity.class);
                startActivity(intent, true);
                break;
            case R.id.tv_goshop:      //如果没有商品时
                mLlEmpty.setVisibility(View.GONE);
                //跳转到homeFragment中

                //不能这样使用.replace也不行.会出现界面的重叠

                //                getActivity().getSupportFragmentManager()
                //                        .beginTransaction()
                //                        .add(R.id.realtabcontent, new HomeFragment())
                //                        .commit();

                EventBus.getDefault().post(new MessageEvent(0));

                break;
            case R.id.checkbox_all:
                isSelectAll = !isSelectAll;
                checkedList.clear();
                if (isSelectAll) {//全选处理
                    mCheckBox.setChecked(true);
                    checkedList.addAll(dataArray);
                }else {
                    mCheckBox.setChecked(false);
                }
                for (ShoppingCart checkBean : dataArray) {
                    checkBean.setIsChecked(isSelectAll);
                }
                mAdapter.notifyDataSetChanged();
                showTotalPrice();
                break;
        }
    }


    @Override
    public void itemChecked(ShoppingCart checkBean, boolean isChecked) {
        //处理Item点击选中回调事件
        if (isChecked) {
            //选中处理
            if (!checkedList.contains(checkBean)) {
                checkedList.add(checkBean);
            }
        } else {
            //未选中处理
            if (checkedList.contains(checkBean)) {
                checkedList.remove(checkBean);
            }
        }
        //判断列表数据是否全部选中
        if (checkedList.size() == dataArray.size()) {
            mCheckBox.setChecked(true);
        } else {
            mCheckBox.setChecked(false);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            refData();
        }
    }

    /**
     * 刷新数据
     * <p>
     * fragment是隐藏与显示.生命周期很多没走.
     * 先将数据全部清除,再重新添加(有可能和以前一样,有可能有新数据)
     * 清除的目的,就是为了防止添加了新数据而界面上没展示
     */
    private void refData() {

        List<ShoppingCart> carts = mCartShopProvider.getAll();
        if (carts != null && carts.size() > 0) {
            mLlEmpty.setVisibility(View.GONE);
            mRvBottom.setVisibility(View.VISIBLE);
            showData();
            showTotalPrice();
        } else {
            initEmptyView();
        }

    }

    public void showTotalPrice() {
        float total = getTotalPrice();
        mTextTotal.setText(Html.fromHtml("合计 ￥<span style='color:#eb4f38'>" + total + "</span>"),
                TextView.BufferType.SPANNABLE);
    }

    /**
     * 计算总和
     */

    public boolean isNull() {
        return (dataArray != null && dataArray.size() > 0);
    }

    private float getTotalPrice() {

        float sum = 0;
        if (!isNull())
            return sum;

        for (ShoppingCart cart : dataArray) {
            if (cart.isChecked()) {   //是否勾上去了
                sum += cart.getCount() * cart.getPrice();
            }
        }

        return sum;
    }

}



