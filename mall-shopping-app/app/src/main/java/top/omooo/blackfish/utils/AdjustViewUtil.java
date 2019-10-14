package top.omooo.blackfish.utils;

/**
 * Created by SSC on 2018/3/20.
 */

import android.graphics.drawable.Drawable;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 调整View大小工具类
 */
public class AdjustViewUtil {

    private static int leftType = 0;
    private static int topType = 1;
    private static int rightType = 2;
    private static int bottomType = 3;

    public void adjustTextViewPic(TextView textView, int type, int marginLeft, int marginTop, int picWidth, int picHeight) {

        Drawable[] drawables = textView.getCompoundDrawables();
        if (type == leftType) {
            //调整TextView DrawableLeft的图片大小
            Drawable drawable = drawables[0];
            drawable.setBounds(marginLeft, marginTop, picWidth, picHeight);
            textView.setCompoundDrawables(drawable, drawables[1], drawables[2], drawables[3]);
        } else if (type == topType) {
            Drawable drawable = drawables[1];
            drawable.setBounds(marginLeft, marginTop, picWidth, picHeight);
            textView.setCompoundDrawables(drawables[0], drawable, drawables[2], drawables[3]);
        } else if (type == rightType) {
            Drawable drawable = drawables[2];
            drawable.setBounds(marginLeft, marginTop, picWidth, picHeight);
            textView.setCompoundDrawables(drawables[0], drawables[1], drawable, drawables[3]);
        } else if (type == bottomType) {
            Drawable drawable = drawables[3];
            drawable.setBounds(marginLeft, marginTop, picWidth, picHeight);
            textView.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawable);
        } else {
            return;
        }
    }

    public void adjustEditTextPic(EditText editText, int type, int marginLeft, int marginTop, int picWidth, int picHeight) {
        Drawable[] drawables = editText.getCompoundDrawables();
        if (type == 0) {
            Drawable drawable = drawables[0];
            drawable.setBounds(marginLeft, marginTop, picWidth, picHeight);
            editText.setCompoundDrawables(drawable, drawables[1], drawables[2], drawables[3]);
        } else {
            return;
        }
    }

}
