package com.platform.app.activity;

import android.content.Intent;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.adapter.GoodsOrderAdapter;
import com.platform.app.bean.Charge;
import com.platform.app.bean.ShoppingCart;
import com.platform.app.contants.Contants;
import com.platform.app.contants.HttpContants;
import com.platform.app.msg.CreateOrderRespMsg;
import com.platform.app.msg.LoginRespMsg;
import com.platform.app.utils.CartShopProvider;
import com.platform.app.utils.LogUtil;
import com.platform.app.widget.FullyLinearLayoutManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.platform.app.http.okhttp.OkHttpUtils;
import com.platform.app.http.okhttp.callback.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Response;


/**
 * Created by wulinhao
 * Time  2019/9/9
 * Describe: 订单确认
 */

public class CreateOrderActivity extends BaseActivity implements View.OnClickListener {

    //微信支付渠道
    private static final String CHANNEL_WECHAT = "wx";
    //支付支付渠道
    private static final String CHANNEL_ALIPAY = "alipay";
    //百度支付渠道
    private static final String CHANNEL_BFB    = "bfb";

    @BindView(R.id.txt_order)
    TextView       txtOrder;
    @BindView(R.id.recycler_view)
    RecyclerView   mRecyclerView;
    @BindView(R.id.rl_alipay)
    RelativeLayout mLayoutAlipay;
    @BindView(R.id.rl_wechat)
    RelativeLayout mLayoutWechat;
    @BindView(R.id.rl_bd)
    RelativeLayout mLayoutBd;
    @BindView(R.id.rb_alipay)
    View           mRbAlipay;
    @BindView(R.id.rb_webchat)
    View           mRbWechat;
    @BindView(R.id.rb_bd)
    View           mRbBd;
    @BindView(R.id.btn_createOrder)
    Button         mBtnCreateOrder;
    @BindView(R.id.txt_total)
    TextView       mTxtTotal;


    private CartShopProvider  cartProvider;
    private GoodsOrderAdapter mAdapter;
    private String            orderNum;
    private String payChannel = CHANNEL_ALIPAY;           //默认为支付宝支付
    private float amount;


    private HashMap<String, RelativeLayout> channels = new HashMap<>();

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_create_order;
    }

    @Override
    protected void init() {
        showData();
        initView();
    }


    private void initView() {

        channels.put(CHANNEL_ALIPAY, mLayoutAlipay);
        channels.put(CHANNEL_WECHAT, mLayoutWechat);
        channels.put(CHANNEL_BFB, mLayoutBd);

        mLayoutAlipay.setOnClickListener(this);
        mLayoutWechat.setOnClickListener(this);
        mLayoutBd.setOnClickListener(this);

        amount = mAdapter.getTotalPrice();
        mTxtTotal.setText("应付款： ￥" + amount);
    }


    public void showData() {

        cartProvider = new CartShopProvider(this);
        mAdapter = new GoodsOrderAdapter(cartProvider.getAll());

        FullyLinearLayoutManager layoutManager = new FullyLinearLayoutManager(this);
        //recyclerView外面嵌套ScrollView.数据显示不全
        layoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setAdapter(mAdapter);

    }


    @Override
    public void onClick(View v) {
        selectPayChannle(v.getTag().toString());
    }

    @OnClick(R.id.rl_addr)
    public void chooseAddress(View view) {
        Intent intent = new Intent(CreateOrderActivity.this, AddressListActivity.class);
        startActivityForResult(intent, Contants.REQUEST_CHOOSE_ADDRESS);
    }


    /**
     * 当前的支付渠道 以及三个支付渠道互斥 的功能
     */
    public void selectPayChannle(String paychannel) {

        for (Map.Entry<String, RelativeLayout> entry : channels.entrySet()) {
            payChannel = paychannel;
            RelativeLayout rb = entry.getValue();
            if (entry.getKey().equals(payChannel)) {
                int childCount = rb.getChildCount();
                LogUtil.e("测试子控件", childCount + "", true);

                View viewCheckBox = rb.getChildAt(2);      //这个是类似checkBox的控件
                viewCheckBox.setBackgroundResource(R.drawable.icon_check_true);
            } else {
                View viewCheckBox = rb.getChildAt(2);      //这个是类似checkBox的控件
                viewCheckBox.setBackgroundResource(R.drawable.icon_check_false);
            }

        }
    }


    @OnClick(R.id.btn_createOrder)
    public void createNewOrder(View view) {
        postNewOrder();     //提交订单
    }


    private void postNewOrder() {

        final List<ShoppingCart> carts = mAdapter.getData();

        List<WareItem> items = new ArrayList<>(carts.size());
        for (ShoppingCart c : carts) {
            // c.getPrice()  是double类型    而接口总价为int 类型,需要转化

            WareItem item = new WareItem(Long.parseLong(String.valueOf(c.getId())), (int) Math
                    .floor(c.getPrice()));
            items.add(item);
        }

        String item_json = new Gson().toJson(items);

        Map<String, String> params = new HashMap<>();
        params.put("user_id", MallShoppingApp.getInstance().getUser().getId() + "");
        params.put("item_json", item_json);
        params.put("pay_channel", payChannel);
        params.put("amount", (int) amount + "");
        params.put("addr_id", 1 + "");

        mBtnCreateOrder.setEnabled(false);

        OkHttpUtils.post().url(HttpContants.ORDER_CREATE)
                .params(params).build()
                .execute(new Callback<CreateOrderRespMsg>() {
                    @Override
                    public CreateOrderRespMsg parseNetworkResponse(Response response, int id)
                            throws Exception {

                        LogUtil.e("支付", "AAAAAAAAAAA", true);
                        String string = response.body().string();
                        CreateOrderRespMsg msg = new Gson().fromJson(string, new
                                TypeToken<LoginRespMsg>() {
                                }.getType());
                        return msg;

                    }

                    @Override
                    public void onError(okhttp3.Call call, Exception e, int id) {
                        mBtnCreateOrder.setEnabled(true);
                        LogUtil.e("支付", e.toString(), true);
                    }



                    @Override
                    public void onResponse(CreateOrderRespMsg response, int id) {

                        mBtnCreateOrder.setEnabled(true);

                        orderNum = response.getData().getOrderNum();
                        Charge charge = response.getData().getCharge();
                    }
                });

    }

    /**
     * 因为接口文档要求,商品列表为json格式,所以这里需要定义一个内部类
     */
    class WareItem {

        private Long ware_id;
        private int  amount;

        public WareItem(Long ware_id, int amount) {
            this.ware_id = ware_id;
            this.amount = amount;
        }

        public Long getWare_id() {
            return ware_id;
        }

        public void setWare_id(Long ware_id) {
            this.ware_id = ware_id;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

}
