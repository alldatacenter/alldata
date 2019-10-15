package com.platform.app.activity;

import android.support.design.widget.TabLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cjj.MaterialRefreshLayout;
import com.platform.app.R;
import com.platform.app.adapter.HotGoodsAdapter;
import com.platform.app.bean.HotGoods;
import com.platform.app.contants.Contants;
import com.platform.app.contants.HttpContants;
import com.platform.app.utils.LogUtil;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.EnjoyshopToolBar;
import com.google.gson.Gson;
import com.platform.app.http.okhttp.OkHttpUtils;
import com.platform.app.http.okhttp.callback.StringCallback;

import java.util.List;

import butterknife.BindView;
import okhttp3.Call;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/17
 *     desc   :商品列表
 *     version: 1.0
 * </pre>
 */

public class GoodsListActivity extends BaseActivity implements View.OnClickListener, TabLayout
        .OnTabSelectedListener {

    public static final int ACTION_LIST = 3;                //列表形式
    public static final int ACTION_GIRD = 4;                //多列形式
    private             int actionType  = ACTION_LIST;     //列表形式默认的值

    public static final int TAG_DEFAULT = 0;     //tabLayout 默认
    public static final int TAG_SALE    = 1;     //tabLayout 价格
    public static final int TAG_PRICE   = 2;     //tabLayout 销量

    @BindView(R.id.tab_layout)
    TabLayout             mTablayout;
    @BindView(R.id.txt_summary)
    TextView              mTxtSummary;
    @BindView(R.id.recycler_view)
    RecyclerView          mRecyclerview;
    @BindView(R.id.refresh_layout)
    MaterialRefreshLayout mRefreshLayout;
    @BindView(R.id.toolbar)
    EnjoyshopToolBar          mToolbar;
    @BindView(R.id.ll_summary)
    LinearLayout          mLlSummary;

    private long campaignId = 0;       //从上一个界面传递过来的参数,也是本界面请求接口的参数,以判断是点击哪里传过来的,进而请求接口
    private Gson mGson      = new Gson();
    private int  currPage   = 1;     //当前是第几页
    private int  totalPage  = 1;    //一共有多少页
    private int  pageSize   = 10;     //每页数目

    private List<HotGoods.ListBean> datas;
    private HotGoodsAdapter         mAdatper;

    @Override
    protected void init() {

        initToolBar();
        campaignId = getIntent().getLongExtra(Contants.COMPAINGAIN_ID, 0);
        initTab();
        getData();
    }


    private void initToolBar() {

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoodsListActivity.this.finish();
            }
        });

        mToolbar.setRightButtonIcon(R.drawable.icon_grid_32);
        mToolbar.getRightButton().setTag(ACTION_LIST);
        mToolbar.setRightButtonOnClickListener(this);
    }


    private void initTab() {

        mTablayout.setOnTabSelectedListener(this);     //这一句必须放在添加tab的前面,要不然第一次进入时,没有默认的
        //    http://www.jianshu.com/p/493d40a9d38e

        TabLayout.Tab tab = mTablayout.newTab();
        tab.setText("默认");
        tab.setTag(TAG_DEFAULT);
        mTablayout.addTab(tab);

        tab = mTablayout.newTab();
        tab.setText("价格");
        tab.setTag(TAG_SALE);
        mTablayout.addTab(tab);

        tab = mTablayout.newTab();
        tab.setText("销量");
        tab.setTag(TAG_PRICE);
        mTablayout.addTab(tab);

    }

    private void getData() {

    }


    @Override
    protected int getContentResourseId() {
        return R.layout.activity_goods_list;
    }


    @Override
    public void onClick(View v) {
        //点击后,取反
        if (actionType == ACTION_LIST) {
            actionType = ACTION_GIRD;
            mToolbar.setRightButtonIcon(R.drawable.icon_list_32);
            mToolbar.getRightButton().setTag(ACTION_GIRD);
        } else {
            actionType = ACTION_LIST;
            mToolbar.setRightButtonIcon(R.drawable.icon_grid_32);
            mToolbar.getRightButton().setTag(ACTION_LIST);
        }

        showData();     //这里必须在获取一次数据,不调用的话,只会在 获取后台数据后展示.点击后不会再走showData()
    }

    @Override
    public void onTabSelected(final TabLayout.Tab tab) {

        int orderBy = (int) tab.getTag();

        String url = HttpContants.WARES_CAMPAIN_LIST
                + "?campaignId=" + campaignId
                + "&orderBy=" + orderBy
                + "&curPage=" + 1
                + "&pageSize=" + 10;

        OkHttpUtils.get().url(url).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtil.e(TAG, "onResponse: " + "失败", true);
            }

            @Override
            public void onResponse(String response, int id) {
                LogUtil.e(TAG, "onResponse:成功 " + response, true);
                HotGoods goodsBean = mGson.fromJson(response, HotGoods.class);
                totalPage = goodsBean.getTotalPage();
                currPage = goodsBean.getCurrentPage();
                datas = goodsBean.getList();

                showData();
            }
        });
    }


    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }


    /**
     * 展示数据
     */
    private void showData() {

        if (datas != null && datas.size() > 0) {
            mTxtSummary.setText("共有" + datas.size() + "件商品");
        } else {
            mLlSummary.setVisibility(View.GONE);
            ToastUtils.showUiToast(GoodsListActivity.this,"暂无商品信息");
            return;
        }

        mAdatper = new HotGoodsAdapter(datas, this);
        mRecyclerview.setAdapter(mAdatper);
        if (actionType == ACTION_LIST) {
            mRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        } else {
            mRecyclerview.setLayoutManager(new GridLayoutManager(this, 2));
        }

        mRecyclerview.setItemAnimator(new DefaultItemAnimator());
        mRecyclerview.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration
                .HORIZONTAL));

    }


}
