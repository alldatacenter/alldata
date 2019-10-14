package top.omooo.blackfish.utils;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

/**
 * Created by SSC on 2018/3/19.
 */

/**
 * View 倒计时
 */
public class CountDownUtil extends CountDownTimer {

    private TextView mTextView;

    public CountDownUtil(long millisInFuture, long countDownInterval, TextView textView) {
        super(millisInFuture, countDownInterval);
        mTextView = textView;
    }

    //倒计时期间被调用
    @Override
    public void onTick(long millisUntilFinished) {
        mTextView.setClickable(false);
        mTextView.setText(millisUntilFinished / 1000 + " s后可重新获取");
        SpannableString spannableString = new SpannableString(mTextView.getText().toString());
        ForegroundColorSpan span = new ForegroundColorSpan(Color.parseColor("#FF6666"));
        spannableString.setSpan(span, 0, 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        mTextView.setText(spannableString);
    }

    //倒计时完成之后调用
    @Override
    public void onFinish() {
        mTextView.setText("点击重新获取验证码");
        mTextView.setClickable(true);
        mTextView.setTextColor(Color.parseColor("#66CCFF"));
    }
}
