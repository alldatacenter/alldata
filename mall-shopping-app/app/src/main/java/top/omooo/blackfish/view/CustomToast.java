package top.omooo.blackfish.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/3/21.
 */

/**
 * 自定义Toast
 * 总结起来就是：
 * Toast.setView(view)
 */
public class CustomToast {

    private static Toast toast;

    public static void show(Context context,String msg) {
        show(context,msg, Toast.LENGTH_SHORT);
    }

    private static void show(Context context, String msg, int time) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_custom_layout, null);
        TextView textView = view.findViewById(R.id.tv_custom_toast_text);
        textView.setText(msg);
        if (toast == null) {
            toast = new Toast(context);
        }
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(time);
        toast.setView(view);
        toast.show();
    }

    public static void cancelToast() {
        if (toast != null) {
            toast.cancel();
        }
    }
}
