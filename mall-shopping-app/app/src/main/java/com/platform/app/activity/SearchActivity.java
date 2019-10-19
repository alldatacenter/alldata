package com.platform.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.platform.app.R;
import com.platform.app.adapter.HistorySearchAdapter;
import com.platform.app.adapter.HotSearchAdapter;
import com.platform.app.utils.PreferencesUtils;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.ClearEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by wulinhao
 * Time  2019/9/22
 * Describe: 搜索界面
 *
 * 一开始使用的是greendao数据库.对于之前没有没有搜索的数据,添加是没有问题的
 * 对于之前存在的数据,不会出现重复添加,重复的数据点击后可以放在最前面,但后面的顺序有些错乱
 * 所以想到使用集合.集合和字符串之间的相互转化,然后用sp存起来.方法是有效的
 * 设计这个界面的目的是为了练习数据库.但还是有问题.
 * 这段时间太忙了,等抽空再次使用greendao尝试一些
 */

public class SearchActivity extends BaseActivity {

    @BindView(R.id.edittxt_phone)
    ClearEditText mEditText;
    @BindView(R.id.hot_search_ry)
    RecyclerView  mHotSearchView;
    @BindView(R.id.history_search_ry)
    RecyclerView  mHistorySearchView;

    private List<String> hotSearchData;
    private List<String> historySearchData;

    private HotSearchAdapter     mHotSearchAdapter;
    private HistorySearchAdapter mHistorySearchAdapter;


    @Override
    protected int getContentResourseId() {
        return R.layout.activity_search;
    }


    @Override
    protected void init() {
        hotSearchData = new ArrayList<>();
        historySearchData = new ArrayList<>();
        setHotSearchData();
        getHotSearchData();
    }


    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        getHistorydata();
        setHistorySearchData();
    }

    /**
     * 热门搜索
     */
    private void getHotSearchData() {

        //TODO 真正开发这里的数据从后台获取

        hotSearchData.add("华为手机");
        hotSearchData.add("玫瑰花");
        hotSearchData.add("移动硬盘");
        hotSearchData.add("android高级进阶");
        hotSearchData.add("蚕丝被");
        mHotSearchAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化热门搜索相关的适配器及其信息
     */
    private void setHotSearchData() {
        mHotSearchAdapter = new HotSearchAdapter(hotSearchData);
        mHotSearchView.setAdapter(mHotSearchAdapter);
        mHotSearchView.setLayoutManager(new GridLayoutManager(this, 3));

        mHotSearchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                String content = (String) adapter.getData().get(position);
                doData(content);
            }
        });
    }

    /**
     * 初始化历史搜索相关的适配器及其信息
     */
    private void setHistorySearchData() {

        mHistorySearchAdapter = new HistorySearchAdapter(historySearchData);
        mHistorySearchView.setAdapter(mHistorySearchAdapter);
        mHistorySearchView.setLayoutManager(new GridLayoutManager(this, 3));

        mHistorySearchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                String content = (String) adapter.getData().get(position);
                doData(content);
            }
        });
    }


    @OnClick(R.id.gosearch)
    public void onViewClicked() {
        String content = mEditText.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            ToastUtils.showSafeToast(SearchActivity.this,"还没输入您想搜索的宝贝呢");
            return;
        }
        doData(content);
    }


    /**
     * 历史搜索
     * 必须进行判断是不是本来就停留在这个界面进行操作,还是初始化进入
     * 要不然,会重复添加数据
     */
    private void getHistorydata() {

        String histortStr = PreferencesUtils.getString(SearchActivity.this, "histortStr");

        if (histortStr != null) {
            historySearchData = new Gson().fromJson(histortStr, new TypeToken<List<String>>() {
            }.getType());
        }

    }


    /**
     * 操作数据库数据
     */
    private int position = -1;

    private void doData(String content) {

        //有历史数据
        if (historySearchData != null && historySearchData.size() > 0) {
            for (int i = 0; i < historySearchData.size(); i++) {
                if (content.equals(historySearchData.get(i))) {
                    //有重复的
                    position = i;
                }
            }

            if (position != -1) {
                historySearchData.remove(position);
                historySearchData.add(0, content);
            } else {
                historySearchData.add(0, content);
            }

        } else {
            //没有历史数据
            historySearchData.add(content);
        }

        mHistorySearchAdapter.notifyDataSetChanged();
        String histortStr = new Gson().toJson(historySearchData);
        PreferencesUtils.putString(SearchActivity.this, "histortStr", histortStr);

        Bundle bundle = new Bundle();
        bundle.putString("search", content);
        Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);     //跳转到搜索结果界面

    }
}


