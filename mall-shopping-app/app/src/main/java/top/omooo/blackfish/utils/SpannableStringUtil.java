package top.omooo.blackfish.utils;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

/**
 * Created by SSC on 2018/3/22.
 */

/**
 * SpannableString工具类
 */
public class SpannableStringUtil {

    private static SpannableString mSpannableString;
    /**
     * 设置前景色
     * @param textView
     * @param start
     * @param end
     * @param color
     * @param text
     */
    public static void setForegroundText(TextView textView, int start, int end, int color, String text) {
        mSpannableString = new SpannableString(text);
        ForegroundColorSpan span = new ForegroundColorSpan(color);
        mSpannableString.setSpan(span, start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(mSpannableString);
    }

    public static void setRelativeSizeText(TextView textView, int start, int end,float relativeSize, String text) {
        mSpannableString = new SpannableString(text);
        RelativeSizeSpan sizeSpan = new RelativeSizeSpan(relativeSize);
        mSpannableString.setSpan(sizeSpan, start, end, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(mSpannableString);
    }

    public SpannableString setMallGoodsPrice(String text,int start,int end) {
        mSpannableString = new SpannableString(text);
        ForegroundColorSpan spanColor = new ForegroundColorSpan(Color.parseColor("#EB5640"));
        mSpannableString.setSpan(spanColor, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        RelativeSizeSpan sizeSpan = new RelativeSizeSpan(1.5f);
        mSpannableString.setSpan(sizeSpan, start + 1, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return mSpannableString;
    }
}
