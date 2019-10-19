package com.platform.app.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.platform.app.R;
import com.platform.app.data.DataManager;
import com.platform.app.data.dao.User;
import com.platform.app.utils.CountTimerView;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.ClearEditText;
import com.platform.app.widget.EnjoyshopToolBar;
import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.OnClick;
import dmax.dialog.SpotsDialog;

/**
 * Created by wulinhao
 * Time  2019/9/12
 * Describe: 接收验证码的注册界面,即注册界面二
 */

public class RegSecondActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolBar;

    @BindView(R.id.txtTip)
    TextView mTxtTip;

    @BindView(R.id.btn_reSend)
    Button mBtnResend;

    @BindView(R.id.edittxt_code)
    ClearEditText mEtCode;

    private String phone;
    private String pwd;

    private SpotsDialog dialog;
    private Gson mGson = new Gson();

    @Override
    protected void init() {

        initToolBar();
        dialog = new SpotsDialog(this);

        phone = getIntent().getStringExtra("phone");
        pwd = getIntent().getStringExtra("pwd");
    }

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_reg_second;
    }

    /**
     * 标题栏 完成
     */
    private void initToolBar() {
        mToolBar.setRightButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCode();
            }
        });
    }

    /**
     * 提交验证码
     */
    private void submitCode() {

        String vCode = mEtCode.getText().toString().trim();

        if (TextUtils.isEmpty(vCode)) {
            ToastUtils.showSafeToast(RegSecondActivity.this, "请填写验证码");
        }

        if (!"1234".equals(vCode)) {
            ToastUtils.showSafeToast(RegSecondActivity.this, "验证码不准确,请重新获取");
        } else {
            addUser();
        }

    }

    @OnClick(R.id.btn_reSend)
    public void getVcode(View view) {

        mTxtTip.setText("验证码为:  1234");

        //倒计时
        CountTimerView timerView = new CountTimerView(mBtnResend);
        timerView.start();

        ToastUtils.showSafeToast(RegSecondActivity.this, "验证码为: 1234");
    }

    private void addUser() {
        User user = new User();
        user.setPhone(phone);
        user.setPwd(pwd);
        DataManager.insertUser(user);

        ToastUtils.showSafeToast(RegSecondActivity.this, "注册成功");
        startActivity(new Intent(RegSecondActivity.this, LoginActivity.class));
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
