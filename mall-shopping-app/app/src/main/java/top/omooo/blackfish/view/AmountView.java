package top.omooo.blackfish.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/4/15.
 */

public class AmountView extends LinearLayout implements View.OnClickListener {


    private EditText mEditText;
    private ImageView mImageReduce;
    private ImageView mImageAdd;


    private int maxNumber;
    private static final String TAG = "AmountView";

    public void setMaxNumber(int maxNumber) {
        this.maxNumber = maxNumber;
    }

    public AmountView(Context context) {
        super(context);
    }

    public AmountView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.view_amount_layout, this);
        mEditText = view.findViewById(R.id.et_nub);
        mEditText.setOnClickListener(this);
        mEditText.setCursorVisible(false);
        mImageReduce = view.findViewById(R.id.iv_reduce);
        mImageReduce.setOnClickListener(this);
        mImageAdd = view.findViewById(R.id.iv_add);
        mImageAdd.setOnClickListener(this);

    }

    public AmountView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onClick(View v) {
        int number = Integer.valueOf(mEditText.getText().toString());
        switch (v.getId()) {
            case R.id.iv_reduce:
                if (number > 1) {
                    number--;
                }
                Log.i(TAG, "onViewClicked: -");
                break;
            case R.id.et_nub:

                Log.i(TAG, "onViewClicked: et");
                break;
            case R.id.iv_add:
                if (number < maxNumber) {
                    number++;
                    Log.i(TAG, "onViewClicked: +");
                }
                break;
        }
        if (number == 1) {
            mImageReduce.setImageResource(R.drawable.icon_reduce_gray);
        } else if (number == maxNumber) {
            mImageAdd.setImageResource(R.drawable.icon_add_gray);
        } else {
            mImageReduce.setImageResource(R.drawable.icon_reduce_yellow);
            mImageAdd.setImageResource(R.drawable.icon_add_yellow);
        }
        String text = number + "";
        mEditText.setText(text);
        mOnNumChangeListener.onChange(number);
    }


    private OnNumChangeListener mOnNumChangeListener;

    public void setOnNumChangeListener(OnNumChangeListener listener) {
        this.mOnNumChangeListener = listener;
    }

    public interface OnNumChangeListener {
        void onChange(int num);
    }

}
