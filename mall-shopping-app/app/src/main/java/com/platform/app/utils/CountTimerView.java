package com.platform.app.utils;

import android.os.CountDownTimer;
import android.widget.TextView;

import com.platform.app.R;


/**
 * Created by wulinhao
 * Time  2019/9/12
 * Describe: 定时器
 */
public class CountTimerView extends CountDownTimer {

    public static final int TIME_COUNT = 61000;//时间防止从59s开始显示（以倒计时60s为例子）
    private TextView btn;
    private int      endStrRid;


    /**
     * 参数 millisInFuture       倒计时总时间（如60S，120s等）
     * 参数 countDownInterval    渐变时间（每次倒计1s）
     * 参数 btn   点击的按钮(因为Button是TextView子类，为了通用我的参数设置为TextView）
     * 参数 endStrRid   倒计时结束后，按钮对应显示的文字
     */
    public CountTimerView(long millisInFuture, long countDownInterval, TextView btn, int
            endStrRid) {
        super(millisInFuture, countDownInterval);
        this.btn = btn;
        this.endStrRid = endStrRid;
    }


    /**
     * 参数上面有注释
     */
    public CountTimerView(TextView btn, int endStrRid) {
        super(TIME_COUNT, 1000);
        this.btn = btn;
        this.endStrRid = endStrRid;
    }

    public CountTimerView(TextView btn) {
        super(TIME_COUNT, 1000);
        this.btn = btn;
        this.endStrRid = R.string.code_end;
    }


    // 计时完毕时触发
    @Override
    public void onFinish() {
        btn.setText(endStrRid);
        btn.setEnabled(true);
    }

    // 计时过程显示
    @Override
    public void onTick(long millisUntilFinished) {
        btn.setEnabled(false);
        btn.setText(millisUntilFinished / 1000 + " 秒后可重新发送");

    }
}
