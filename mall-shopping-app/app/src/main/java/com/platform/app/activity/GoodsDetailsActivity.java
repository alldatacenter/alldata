package com.platform.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.bean.HotGoods;
import com.platform.app.bean.User;
import com.platform.app.contants.HttpContants;
import com.platform.app.helper.SharePresenter;
import com.platform.app.utils.CartShopProvider;
import com.platform.app.utils.LogUtil;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.EnjoyshopToolBar;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.platform.app.http.okhttp.OkHttpUtils;
import com.platform.app.http.okhttp.callback.StringCallback;

import butterknife.BindView;
import okhttp3.Call;

/**
 * Created by wulinhao
 * Time  2019/9/9
 * Describe: 商品详情
 */

public class GoodsDetailsActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolBar;
    @BindView(R.id.webView)
    WebView      mWebView;

    private HotGoods.ListBean goodsBean;
    private WebAppInterface   mAppInterfce;
    private CartShopProvider  cartProvider;

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_goods_detail;
    }

    @Override
    protected void init() {

        //接收数据
        Bundle bundle = getIntent().getExtras();
        goodsBean = (HotGoods.ListBean) bundle.getSerializable("itemClickGoods");
        if (goodsBean == null) {
            finish();
        }

        cartProvider = new CartShopProvider(this);

        LogUtil.e("跳转后数据", goodsBean.getName() + goodsBean.getPrice(), true);

        initToolBar();

        initData();

    }


    private void initData() {

        final WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setAppCacheEnabled(true);
        mWebView.loadUrl(HttpContants.WARES_DETAIL);

        mAppInterfce = new WebAppInterface(this);
        mWebView.addJavascriptInterface(mAppInterfce, "appInterface");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String s) {
                webView.loadUrl(s);
                return true;
            }

            @Override
            public void onPageFinished(WebView webView, String s) {
                super.onPageFinished(webView, s);
                //整个页面加载完后,才能调用这个方法
                mAppInterfce.showDetail();
            }
        });

    }

    /**
     * 初始化标题栏
     */
    private void initToolBar() {

        mToolBar.setNavigationOnClickListener(this);
        mToolBar.setRightButtonText("分享");
        mToolBar.setRightButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharePresenter.getInstance().showShareDialogOnBottom
                        (0, GoodsDetailsActivity.this, "计算机书籍",
                                "第二行代码", "0");
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar:
                this.finish();
                break;
        }
    }


    class WebAppInterface {

        private Context context;

        public WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void showDetail() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:showDetail(" + goodsBean.getId() + ")");
                }
            });
        }

        @JavascriptInterface
        public void buy(long id) {
            cartProvider.put(goodsBean);
            ToastUtils.showSafeToast(GoodsDetailsActivity.this, "已添加到购物车");
        }


        /**
         * 这里和视频 源代码有出入.
         * 已经修改了.之前是 收藏  加入购物车.
         * 现在变成了 加入购物车  立即购物
         */
        @JavascriptInterface
        public void addToCart(long id) {
            addToFavorite();
        }
    }


    private void addToFavorite() {

        User user = MallShoppingApp.getInstance().getUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class), true);
        }

        Long userId = MallShoppingApp.getInstance().getUser().getId();

        String url = HttpContants.FAVORITE_CREATE + "?user_id=" + userId + "&ware_id=" + goodsBean
                .getId();


        OkHttpUtils.post().url(HttpContants.FAVORITE_CREATE).build().execute(new StringCallback() {

            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtil.e("收藏", "收藏失败" + e, true);
            }

            @Override
            public void onResponse(String response, int id) {
                ToastUtils.showSafeToast(GoodsDetailsActivity.this,"已添加到收藏夹");
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
    }
}
