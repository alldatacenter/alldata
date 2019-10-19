package com.platform.app.activity;

import android.view.View;

import com.platform.app.R;
import com.platform.app.widget.EnjoyshopToolBar;

import butterknife.BindView;

/**
 * Created by wulinhao
 * Time  2019/9/21
 * Describe: 我的收藏
 */

public class MyFavoriteActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolBar;

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_myfavorite;
    }


    @Override
    protected void init() {
        initToolBar();
        //TODO 获取后台的数据
        //getDataFromNet();
    }

    /**
     * 关于标题栏的操作
     */
    private void initToolBar() {

        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyFavoriteActivity.this.finish();
            }
        });
    }

}
