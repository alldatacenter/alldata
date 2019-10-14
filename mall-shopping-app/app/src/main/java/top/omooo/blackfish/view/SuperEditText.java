package top.omooo.blackfish.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;

import top.omooo.blackfish.R;
import top.omooo.blackfish.listener.OnSuperEditClickListener;
import top.omooo.blackfish.listener.OnSuperEditLayoutClickListener;
import top.omooo.blackfish.utils.KeyBoardUtil;

/**
 * Created by SSC on 2018/3/26.
 */

public class SuperEditText extends RelativeLayout {

    private RelativeLayout mLayout;
    private ImageView mImageLeft,mImageRight;
    private TextView mTextView;
    private EditText mEditText;
    private int typeMode;
    private HashMap<String, String> mStringHashMap = new HashMap<>();

    private static final String TAG = "SuperEditText";

    private OnSuperEditLayoutClickListener mClickListener;
    private OnSuperEditClickListener mSuperListener;


//    public void setStringHashMap(HashMap<String, String> map) {
//        this.mStringHashMap = map;
//    }

    //SuperEditText给外布局传数据，适用于纯EditText
    public HashMap<String, String> getStringHashMap() {
        return mStringHashMap;
    }

    //外布局给SuperEditText传数据，适用于非纯EdiText
    public void setEditText(String text) {
        mEditText.setText(text);
    }

    public void setOnSuperEditClickListener(OnSuperEditLayoutClickListener listener) {
        this.mClickListener = listener;
    }

    public void setOnSuperClickListener(OnSuperEditClickListener superListener) {
        this.mSuperListener = superListener;
    }

    public String getData() {
        if (typeMode == 0) {
            return mEditText.getText().toString();
        } else {
            return mEditText.getHint().toString();
        }
    }
    public SuperEditText(Context context) {
        super(context);
    }

    public SuperEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.view_super_edit_text_layout, this);
        mLayout = view.findViewById(R.id.super_text_layout);
        mImageLeft = view.findViewById(R.id.iv_create_icon);
        mImageRight = view.findViewById(R.id.iv_create_to_right);
        mTextView = view.findViewById(R.id.tv_create_title);
        mEditText = view.findViewById(R.id.et_create_right);

        mLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mClickListener) {
                    mClickListener.onSuperEditClick(mTextView.getText().toString());
                    Log.i(TAG, "onClick: " + v.getId());
                    if (Build.VERSION.SDK_INT >= 26) {
                        //minSdkVersion=26
                        if (mEditText.getFocusable() == FOCUSABLE) {
                            mEditText.requestFocus();
                            KeyBoardUtil.showKeyBoard(mEditText);
                        } else {
                            mEditText.setInputType(InputType.TYPE_NULL);
                            KeyBoardUtil.closeKeyBoard(mEditText);
                        }
                    } else {
                        //需要点击EditText才会弹出软键盘
                    }
                }
            }
        });
        
        mEditText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mSuperListener) {
                    mSuperListener.onSuperClick(mTextView.getText().toString());
                    if (Build.VERSION.SDK_INT >= 26) {
                        //minSdkVersion=26
                        if (mEditText.getFocusable() == FOCUSABLE) {
                            mEditText.requestFocus();
                            KeyBoardUtil.showKeyBoard(mEditText);
                        } else {
                            mEditText.setInputType(InputType.TYPE_NULL);
                            KeyBoardUtil.closeKeyBoard(mEditText);
                        }
                    } else {
                        //需要点击EditText才会弹出软键盘
                    }
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO: 2018/3/31 每个HashMap只存了一个数据，有点浪费，选择更好的数据结构吧，但是每个SuperText只是一个单独的单元，有点难搞 
                mStringHashMap.put(mTextView.getText().toString(), mEditText.getText().toString());
            }
        });


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SuperEditText);
        int drawableId = typedArray.getResourceId(R.styleable.SuperEditText_iconLeft, 0);
        String leftText = typedArray.getString(R.styleable.SuperEditText_titleText);
        String hintText = typedArray.getString(R.styleable.SuperEditText_hintTextRight);
        int typeMode = typedArray.getInteger(R.styleable.SuperEditText_typeMode, 0);
        int hintColor = typedArray.getColor(R.styleable.SuperEditText_hintTextColor, getResources().getColor(R.color.splash_main_title_color));

        /**
         * typeMode用于标识样式
         * 0 --> 右边为纯EditText
         * 1 --> 右边为纯TextView 无箭头
         * 2 --> 右边为TextView 有箭头
         * 3 --> 右边为TextView 有箭头，字体为黑色
         */
        mTextView.setText(leftText);
        mImageLeft.setImageResource(drawableId);
        mEditText.setHint(hintText);

        if (typeMode == 0) {

        } else if (typeMode == 1) {
            mEditText.setHintTextColor(hintColor);
            mEditText.setCursorVisible(false);
            mEditText.setClickable(true);
            mEditText.setFocusable(false);
            mEditText.setInputType(InputType.TYPE_NULL);
        } else if (typeMode == 2) {
            mEditText.setCursorVisible(false);
            mEditText.setClickable(true);
            mEditText.setFocusable(false);
            mEditText.setInputType(InputType.TYPE_NULL);
            mImageRight.setVisibility(VISIBLE);
        } else if (typeMode == 3) {
            mEditText.setCursorVisible(false);
            mEditText.setClickable(true);
            mEditText.setFocusable(false);
            mEditText.setInputType(InputType.TYPE_NULL);
            mImageRight.setVisibility(VISIBLE);
            mEditText.setHintTextColor(getResources().getColor(R.color.splash_main_title_color));
        }
        //释放资源
        typedArray.recycle();
    }

    public SuperEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
