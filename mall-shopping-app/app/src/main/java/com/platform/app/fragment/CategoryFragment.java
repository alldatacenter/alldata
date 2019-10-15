package com.platform.app.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.cjj.MaterialRefreshLayout;
import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.activity.GoodsDetailsActivity;
import com.platform.app.adapter.CategoryAdapter;
import com.platform.app.adapter.SecondGoodsAdapter;
import com.platform.app.bean.Category;
import com.platform.app.bean.HotGoods;
import com.platform.app.bean.Weather;
import com.platform.app.contants.HttpContants;
import com.platform.app.service.LocationService;
import com.platform.app.utils.LogUtil;
import com.platform.app.utils.ToastUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sunfusheng.marqueeview.MarqueeView;
import com.platform.app.http.okhttp.OkHttpUtils;
import com.platform.app.http.okhttp.callback.StringCallback;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import okhttp3.Call;

import static com.platform.app.MallShoppingApp.getApplication;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/08
 *     desc   : 分类fragment
 *     version: 1.0
 * </pre>
 */
public class CategoryFragment extends BaseFragment {

    private static final int STATE_NORMAL = 0;
    private static final int STATE_REFREH = 1;
    private static final int STATE_MORE   = 2;
    private              int state        = STATE_NORMAL;       //正常情况

    @BindView(R.id.recyclerview_category)
    RecyclerView          mRecyclerView;
    @BindView(R.id.recyclerview_wares)
    RecyclerView          mRecyclerviewWares;
    @BindView(R.id.refresh_layout)
    MaterialRefreshLayout mRefreshLaout;
    @BindView(R.id.vf_hotmessage)
    MarqueeView           mVfHotMessage;
    @BindView(R.id.tv_city)
    TextView              mCityName;
    @BindView(R.id.tv_day_weather)
    TextView              mDayWeather;
    @BindView(R.id.tv_night_weather)
    TextView              mNightWeather;

    private Gson           mGson         = new Gson();
    private List<Category> categoryFirst = new ArrayList<>();      //一级菜单
    private CategoryAdapter         mCategoryAdapter;                      //一级菜单
    private SecondGoodsAdapter      mSecondGoodsAdapter;              //二级菜单
    private List<HotGoods.ListBean> datas;
    private List<String>            mVFMessagesList;                 //上下轮播的信息

    private String          provinceName;                                  //省份
    private String          cityName;                                      //城市名
    private String          dayWeather;
    private String          nightWeather;
    private LocationService locationService;

    private int currPage  = 1;     //当前是第几页
    private int totalPage = 1;    //一共有多少页
    private int pageSize  = 10;     //每页数目


    @Override
    protected int getContentResourseId() {
        return R.layout.fragment_category;
    }


    @Override
    protected void init() {

        mVFMessagesList = new ArrayList<>();

        requestCategoryData();      // 热门商品数据
        requestMessageData();        //轮播信息数据
        getLocation();            //获取当前城市的位置

    }

    @Override
    public void onResume() {
        super.onResume();
        mVfHotMessage.startFlipping();
    }

    private void getLocation() {

        locationService = ((MallShoppingApp) getApplication()).locationService;
        locationService.registerListener(mListener);
        locationService.setLocationOption(locationService.getOption());
        locationService.start();// 定位SDK
    }


    private void requestCategoryData() {

        OkHttpUtils.get().url(HttpContants.CATEGORY_LIST).build()
                .execute(new StringCallback() {

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        LogUtil.e("分类一级", e + "", true);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        LogUtil.e("分类一级", response + "", true);

                        Type collectionType = new TypeToken<Collection<Category>>() {
                        }.getType();
                        Collection<Category> enums = mGson.fromJson(response, collectionType);
                        Iterator<Category> iterator = enums.iterator();
                        while (iterator.hasNext()) {
                            Category bean = iterator.next();
                            categoryFirst.add(bean);
                        }

                        showCategoryData();
                        defaultClick();

                    }
                });

    }


    private void requestMessageData() {

        mVFMessagesList.add("开学季,凭录取通知书购手机6折起");
        mVFMessagesList.add("都世丽人内衣今晚20点最低10元开抢");
        mVFMessagesList.add("购联想手机达3000元以上即送赠电脑包");
        mVFMessagesList.add("秋老虎到来,商城App为您准备了这些必备生活用品");
        mVFMessagesList.add("穿了幸福时光男装,帅呆呆,妹子马上来");

        if (!mVFMessagesList.isEmpty()) {
            mVfHotMessage.setVisibility(View.VISIBLE);
            mVfHotMessage.startWithList(mVFMessagesList);
        } else {
            mVfHotMessage.setVisibility(View.GONE);
        }

    }


    /**
     * 展示一级菜单数据
     */
    private boolean isclick = false;

    private void showCategoryData() {

        mCategoryAdapter = new CategoryAdapter(categoryFirst);

        mCategoryAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Category category = (Category) adapter.getData().get(position);
                int id = category.getId();
                String name = category.getName();
                isclick = true;
                defaultClick();
                requestWares(id);
            }
        });


        mRecyclerView.setAdapter(mCategoryAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));

    }


    private void defaultClick() {

        //默认选中第0个
        if (!isclick) {
            Category category = categoryFirst.get(0);
            int id = category.getId();
            requestWares(id);
        }
    }


    /**
     * 二级菜单数据
     *
     * @param firstCategorId 一级菜单的firstCategorId
     */
    private void requestWares(int firstCategorId) {

        String url = HttpContants.WARES_LIST + "?categoryId=" + firstCategorId + "&curPage=" +
                currPage + "&pageSize=" + pageSize;

        OkHttpUtils.get().url(url).build().execute(new StringCallback() {

            @Override
            public void onError(Call call, Exception e, int id) {
                LogUtil.e("二级菜单", e + "", true);
            }

            @Override
            public void onResponse(String response, int id) {
                LogUtil.e("二级菜单", response + "", true);

                HotGoods hotGoods = mGson.fromJson(response, HotGoods.class);
                totalPage = hotGoods.getTotalPage();
                currPage = hotGoods.getCurrentPage();
                datas = hotGoods.getList();

                showData();

            }
        });
    }

    /**
     * 展示二级菜单的数据
     */
    private void showData() {
        switch (state) {
            case STATE_NORMAL:

                mSecondGoodsAdapter = new SecondGoodsAdapter(datas);
                mSecondGoodsAdapter.setOnItemClickListener(new BaseQuickAdapter
                        .OnItemClickListener() {

                    @Override
                    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                        HotGoods.ListBean listBean = (HotGoods.ListBean) adapter.getData().get
                                (position);

                        Intent intent = new Intent(getContext(), GoodsDetailsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("itemClickGoods", (Serializable) listBean);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                });


                mRecyclerviewWares.setAdapter(mSecondGoodsAdapter);
                mRecyclerviewWares.setLayoutManager(new GridLayoutManager(getContext(), 2));
                mRecyclerviewWares.setItemAnimator(new DefaultItemAnimator());
                mRecyclerviewWares.addItemDecoration(new DividerItemDecoration(getContext(),
                        DividerItemDecoration.HORIZONTAL));
                break;

            //            case STATE_REFREH:
            //                mAdatper.clearData();
            //                mAdatper.addData(datas);
            //                mRecyclerView.scrollToPosition(0);
            //                mRefreshLaout.finishRefresh();
            //                break;
            //
            //            case STATE_MORE:
            //                mAdatper.addData(mAdatper.getDatas().size(), datas);
            //                mRecyclerView.scrollToPosition(mAdatper.getDatas().size());
            //                mRefreshLaout.finishRefreshLoadMore();
            //                break;
        }
    }


    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法，可以直接拷贝如下代码到自己工程中修改
     *
     */
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {

            if (null != location && location.getLocType() != BDLocation.TypeServerError) {

                cityName = location.getCity();
                provinceName = location.getProvince();
                if (cityName != null) {
                    mCityName.setText(cityName.substring(0, cityName.length() - 1));
                } else {
                    mCityName.setText("上海");
                }
                getCityWeather();
            } else {
                getCityWeather();
            }
        }

    };


    /**
     * 查询天气数据
     */
    private void getCityWeather() {

        String city;          //有可能查询不到,或者网络异常,所以默认查询城市为 湖北武汉
        String province;

        if (cityName != null && provinceName != null) {
            city = cityName.substring(0, cityName.length() - 1);
            province = provinceName.substring(0, provinceName.length() - 1);
        } else {
            city = "武汉";
            province = "湖北";
        }

        String url = HttpContants.requestWeather + "?key=201f8a7a91c30&city=" + city +
                "&province=" + province;

        OkHttpUtils.get().url(url).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {

            }

            @Override
            public void onResponse(String response, int id) {
                try {
                    Weather weather = mGson.fromJson(response, Weather.class);
                    List<Weather.ResultBean> result = weather.getResult();
                    //只有一个城市,所以只有一个数据
                    List<Weather.ResultBean.FutureBean> future = result.get(0).getFuture();
                    dayWeather = future.get(0).getDayTime();
                    nightWeather = future.get(0).getNight();
                    showWeather();
                } catch (Exception e) {
                    ToastUtils.showSafeToast(getContext(), e.getMessage());
                }
            }
        });
    }

    /**
     * 展示天气数据
     */
    private void showWeather() {
        mDayWeather.setText("白天: " + dayWeather);
        mNightWeather.setText("晚间: " + nightWeather);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
    }

    @Override
    public void onPause() {
        super.onPause();
        mVfHotMessage.stopFlipping();
    }
}



