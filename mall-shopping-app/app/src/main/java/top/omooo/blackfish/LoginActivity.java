package top.omooo.blackfish;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.blackfish.utils.KeyBoardUtil;
import top.omooo.blackfish.view.NumberKeyBoardView;

/**
 * Created by SSC on 2018/3/18.
 */

public class LoginActivity extends BaseActivity implements View.OnTouchListener,TextWatcher{

    private ImageView mImageExitActivity;
    private TextView mTextMessage,mTextToSmsLogin,mTextForgetPwd, mTextKeyFinish;
    private Button mButtonLogin;
    private EditText mEditPhone, mEditPwd;

    private boolean isPwdLogin = false;

    private RelativeLayout mPwdLayout;
    private RelativeLayout mKeyBoardLayout;
    private NumberKeyBoardView mBoardView;

    private static final String TAG = "LoginActivity";
    private boolean isPwdVisible = false;
    private boolean isShowBoard = true;


    private AdjustViewUtil mAdjustViewUtil;


    @Override
    public int getLayoutId() {
        return R.layout.activity_login_layout;
    }

    @Override
    public void initViews() {
        mImageExitActivity = findView(R.id.iv_login_cancel);
        mTextMessage = findView(R.id.tv_hint_message);
        mTextToSmsLogin = findView(R.id.tv_to_sms_login);
        mTextForgetPwd = findView(R.id.tv_forget_pwd);
        mButtonLogin = findView(R.id.btn_login);
        mEditPhone = findView(R.id.et_login_phone);
        mEditPwd = findView(R.id.et_login_pwd);

        mPwdLayout = findView(R.id.rl_pwd_layout);
        mKeyBoardLayout = findView(R.id.rl_key_board_layout);
        mTextKeyFinish = (TextView) mKeyBoardLayout.getChildAt(3);
        mBoardView = (NumberKeyBoardView) mKeyBoardLayout.getChildAt(2);

        //自定义安全键盘
        // TODO: 2018/4/4 太多坑了，比如光标问题等等 
        mBoardView.setEditText(mEditPhone);


        mButtonLogin.setBackground(getDrawable(R.drawable.shape_btn_pressed));
        mButtonLogin.setClickable(false);

        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustEditTextPic(mEditPhone, 0, 0, 0, 55, 55);
        mAdjustViewUtil.adjustEditTextPic(mEditPwd, 0, 0, 0, 55, 55);

//        KeyBoardUtil.showKeyBoard(mEditPhone);

    }


    @Override
    public void initListener() {
        mImageExitActivity.setOnClickListener(this);
        mButtonLogin.setOnClickListener(this);
        mTextMessage.setOnClickListener(this);
        mTextToSmsLogin.setOnClickListener(this);
        mTextForgetPwd.setOnClickListener(this);

        mEditPhone.addTextChangedListener(this);
        mEditPhone.setOnTouchListener(this);
        mEditPwd.setOnTouchListener(this);

        mTextKeyFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeyBoardLayout.setVisibility(View.GONE);
                isShowBoard = false;
            }
        });
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_login_cancel:
                finish();
                this.overridePendingTransition(0, R.anim.activity_login_top_out);
                break;
            case R.id.tv_hint_message:
                mTextMessage.setVisibility(View.GONE);
                mPwdLayout.setVisibility(View.VISIBLE);
                mButtonLogin.setText("登录");
                isPwdLogin = true;
                break;
            case R.id.btn_login:
                if (!isPwdLogin) {
                    Bundle bundle = new Bundle();
                    bundle.putString("phone_number", mEditPhone.getText().toString());
                    Intent intent = new Intent(LoginActivity.this, VerifyCodeActivity.class);
                    intent.putExtras(bundle);
                    startActivity(intent);
                } else {
                    if (mEditPwd.getText().equals("")) {
                        Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                    } else {
                        // TODO: 2018/4/4 密码登录验证
                        Toast.makeText(this, "登录失败", Toast.LENGTH_SHORT).show();
                    } 
                }
                break;
            case R.id.tv_to_sms_login:
                mTextMessage.setVisibility(View.VISIBLE);
                mPwdLayout.setVisibility(View.GONE);
                mButtonLogin.setText("下一步");
                isPwdLogin = false;
                break;
            case R.id.tv_forget_pwd:
                Toast.makeText(this, "忘记密码", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void initData() {

    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {

            switch (v.getId()) {
                case R.id.et_login_phone:
                    KeyBoardUtil.closeKeyBoard(mEditPwd);
                    mKeyBoardLayout.setVisibility(View.VISIBLE);
                    isShowBoard = true;
                    break;
                case R.id.et_login_pwd:
                    mKeyBoardLayout.setVisibility(View.GONE);
//                    KeyBoardUtil.showKeyBoard(mEditPwd);
                    isShowBoard = false;
                    break;
                default:break;
            }

            Drawable drawable = mEditPhone.getCompoundDrawables()[2];
            Drawable drawable1 = mEditPwd.getCompoundDrawables()[2];
            Drawable[] drawables = mEditPwd.getCompoundDrawables();
            // TODO: 2018/3/19 按下垂直距离的处理
            if (drawable != null && event.getX() > mEditPhone.getWidth() - mEditPhone.getPaddingRight() - drawable.getIntrinsicWidth()) {
                Log.i(TAG, "onTouch: 0");
                mEditPhone.setText("");
            }
            if (drawable1 != null && event.getX() > mEditPwd.getWidth() - mEditPwd.getPaddingRight() - drawable1.getIntrinsicWidth()) {
                Log.i(TAG, "onTouch: 1");
                if (!isPwdVisible) {
                    Log.i(TAG, "onTouch: " + "密码可见");
                    mEditPwd.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    Drawable pwdDrawableRight = getDrawable(R.drawable.icon_login_pwd_visiable);
                    pwdDrawableRight.setBounds(drawable1.getBounds());
                    mEditPwd.setCompoundDrawables(drawables[0], drawables[1], pwdDrawableRight, drawables[3]);
                    isPwdVisible = true;
                } else {
                    Log.i(TAG, "onTouch: " + "密码不可见");
                    mEditPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    Drawable pwdDrawableRight = getDrawable(R.drawable.icon_login_pwd_right);
                    pwdDrawableRight.setBounds(drawable1.getBounds());
                    mEditPwd.setCompoundDrawables(drawables[0], drawables[1], pwdDrawableRight, drawables[3]);
                    isPwdVisible = false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {

        return super.equals(obj);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mEditPhone.getText().length() == 11) {
            mButtonLogin.setClickable(true);
            mButtonLogin.setPressed(true);
            mButtonLogin.setBackground(getDrawable(R.drawable.shape_btn_no_pressed));
        } else {
            mButtonLogin.setClickable(false);
            mButtonLogin.setPressed(false);
            mButtonLogin.setBackground(getDrawable(R.drawable.shape_btn_pressed));
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && isShowBoard == true) {
            mKeyBoardLayout.setVisibility(View.GONE);
            isShowBoard = false;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && isShowBoard == false) {
            mEditPhone.setText("");
            finish();
        }
        return false;
    }
}
