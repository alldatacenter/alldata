package top.omooo.blackfish.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import top.omooo.blackfish.R;
import top.omooo.blackfish.listener.InputCompleteListener;

/**
 * Created by SSC on 2018/3/19.
 */

/**
 * 自定义验证码控件
 * 参考自：https://github.com/jb274585381/VerifyCodeViewDemo
 */
public class VerifyCodeView extends RelativeLayout {

    private EditText mEditText;
    private TextView[] mTextViews;
    private static int MAX = 6;
    private String inputContent;


    public VerifyCodeView(Context context) {
        super(context);
        initView(context);
    }

    public VerifyCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VerifyCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View.inflate(context, R.layout.view_verify_code_layout, this);

        mTextViews = new TextView[MAX];
        mTextViews[0] = findViewById(R.id.tv_verify_code1);
        mTextViews[1] = findViewById(R.id.tv_verify_code2);
        mTextViews[2] = findViewById(R.id.tv_verify_code3);
        mTextViews[3] = findViewById(R.id.tv_verify_code4);
        mTextViews[4] = findViewById(R.id.tv_verify_code5);
        mTextViews[5] = findViewById(R.id.tv_verify_code6);
        mEditText = findViewById(R.id.et_code_verify_text);

        //隐藏光标
        mEditText.setCursorVisible(false);

        setEditTextListener();
    }

    private void setEditTextListener() {
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                inputContent = mEditText.getText().toString();
                if (mInputCompleteListener != null) {
                    if (inputContent.length() >= MAX) {
                        mInputCompleteListener.inputComplete();
                    } else {
                        mInputCompleteListener.invalidContent();
                    }
                }
                for (int i = 0; i < MAX; i++) {
                    if (i < inputContent.length()) {
                        mTextViews[i].setText(String.valueOf(inputContent.charAt(i)));
                    } else {
                        mTextViews[i].setText("");
                    }
                }
            }
        });
    }

    private InputCompleteListener mInputCompleteListener;

    public void setInputCompleteListener(InputCompleteListener inputCompleteListener) {
        mInputCompleteListener = inputCompleteListener;
    }

    public String getEditContent() {
        return inputContent;
    }

    public void setEditContentEmpty() {
        mEditText.setText("");
    }
}
