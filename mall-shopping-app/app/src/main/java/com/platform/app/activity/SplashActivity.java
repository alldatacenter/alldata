package com.platform.app.activity;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.platform.app.R;
import com.platform.app.utils.PreferencesUtils;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by:wulinhao
 * Time:  2019/9/22
 * Describe:欢迎界面
 * 无标题栏和全屏必须写在 setContentView前面
 */

public class SplashActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.tv_time)
    TextView mTvTime;
    private int duration = 3;      //倒计时3秒
    Timer timer = new Timer();

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_index;
    }

    @Override
    protected void init() {
        timer.schedule(task, 1000, 1000);
    }


    /**
     * 必须重写base中的setStatusBar方法.要不然用继承父类的沉浸式状态栏
     */
    @Override
    protected void setStatusBar() {
        //里面什么东西都没有
    }

    /**
     * 界面的跳转
     */
    private void jumpActivity() {

        boolean isFirst = PreferencesUtils.getBoolean(SplashActivity.this, "isFirst", true);
        //默认为第一次

        if (isFirst) {
            PreferencesUtils.putBoolean(SplashActivity.this, "isFirst", false);
            startActivity(new Intent(SplashActivity.this, GuideActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }

        finish();
    }

    /**
     * 如果点击了,停止倒计时,直接跳转
     */
    @OnClick(R.id.ll_time)
    public void onClick(View v) {
        timer.cancel();
        jumpActivity();
    }


    private TimerTask task = new TimerTask() {
        @Override
        public void run() {

            runOnUiThread(new Runnable() {      // UI thread
                @Override
                public void run() {
                    duration--;
                    mTvTime.setText(duration + "");
                    if (duration < 2) {
                        timer.cancel();
                        jumpActivity();
                    }
                }
            });

        }
    };

}
