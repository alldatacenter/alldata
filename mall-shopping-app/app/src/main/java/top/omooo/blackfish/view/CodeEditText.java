package top.omooo.blackfish.view;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by SSC on 2018/3/19.
 */

/**
 * 验证码控件，自定义EditText
 * 以去掉传统EditText长按或者双击会选中EditText的内容
 * 和去掉光标位置会随点击而改变
 */
public class CodeEditText extends AppCompatEditText {

    private long lastTime = 0;

    public CodeEditText(Context context) {
        super(context);
    }

    public CodeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CodeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        this.setSelection(this.getText().length());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime < 500) {
                    lastTime = currentTime;
                    return true;
                } else {
                    lastTime = currentTime;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

}
