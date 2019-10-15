package com.platform.app.activity;

import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.platform.app.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by:wulinhao
 * Time:  2019/8/22
 * Describe: 引导页.
 * 第一次安装或者卸载后重装时,才出现这个界面
 */

public class GuideActivity extends BaseActivity {

    @BindView(R.id.viewpager)
    ViewPager mViewPager;
    @BindView(R.id.btn_start)
    Button    mBtnStart;

    //获取图片资源
    int[] imgRes = new int[]{R.drawable.guide_1, R.drawable.guide_2, R.drawable.guide_3, R
            .drawable.guide_4};
    private List<View> mViewList = new ArrayList<>();


    @Override
    protected int getContentResourseId() {

        //必须写在这里,不能写在 init 中.先全屏,再加载试图
//        requestWindowFeature(Window.FEATURE_NO_TITLE);       // 无标题栏
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
//                .LayoutParams.FLAG_FULLSCREEN);    //全屏

        return R.layout.activity_guide;
    }


    /**
     * 必须重写base中的setStatusBar方法.要不然用继承父类的沉浸式状态栏
     */
    @Override
    protected void setStatusBar() {
        //里面什么东西都没有
    }


    @Override
    protected void init() {
        initData();

        MyPagerAdapter adapter = new MyPagerAdapter();
        mViewPager.setAdapter(adapter);

        mViewPager.setOnPageChangeListener(new PagePositionLister());
    }

    /**
     * 初始化数据
     */
    private void initData() {

        for (int i = 0; i < imgRes.length; i++) {
            View inflate = getLayoutInflater().inflate(R.layout.guide_item, null);
            ImageView ivGuide = (ImageView) inflate.findViewById(R.id.iv_guide);
            ivGuide.setBackgroundResource(imgRes[i]);
            mViewList.add(inflate);
        }
    }

    @OnClick(R.id.btn_start)
    public void jumpActivity(View view) {
        startActivity(new Intent(GuideActivity.this, MainActivity.class));
        finish();
    }


    /**
     * viewPager适配器
     */
    private class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mViewList == null ? 0 : mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = mViewList.get(position);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));
        }
    }

    private class PagePositionLister implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //如果滑动到最后一张,显示按钮
            if (position == mViewList.size() - 1) {
                mBtnStart.setVisibility(View.VISIBLE);
            } else {
                mBtnStart.setVisibility(View.GONE);
            }
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
