package com.platform.app.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.data.DataManager;
import com.platform.app.data.dao.User;
import com.platform.app.utils.StringUtils;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.ClearEditText;
import com.platform.app.widget.EnjoyshopToolBar;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by wulinhao
 * Time  2019/9/9
 * Describe: 登录界面
 */

public class LoginActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolBar;
    @BindView(R.id.etxt_phone)
    ClearEditText    mEtxtPhone;
    @BindView(R.id.etxt_pwd)
    ClearEditText    mEtxtPwd;
    @BindView(R.id.txt_toReg)
    TextView         mTxtToReg;

    private String imageUrl = "https://timgsa.baidu" +
            ".com/timg?image&quality=80&size=b9999_10000&sec=1535017475541&di" +
            "=a5e08ea47bc24083efd75c547b8fc083&imgtype=0&src=http%3A%2F%2Fbos.pgzs" +
            ".com%2Frbpiczy%2FWallpaper%2F2013%2F10%2F8%2F8ad17e66b69046af812665a0e24ce862.jpg";

    @Override
    protected void init() {
        initToolBar();
    }

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_login;
    }

    private void initToolBar() {

        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginActivity.this.finish();
            }
        });
    }


    @OnClick({R.id.btn_login, R.id.txt_toReg})
    public void viewclick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                login();   //登录
                break;
            case R.id.txt_toReg:
                Intent intent = new Intent(this, RegActivity.class);
                startActivity(intent);
                break;
        }
    }

    /**
     * 登录
     * 注意注意.商业项目是直接请求登录接口.这次是对数据库进行操作
     */
    private void login() {

        String phone = mEtxtPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            ToastUtils.showSafeToast(LoginActivity.this, "请输入手机号码");
            return;
        }

        if (!StringUtils.isMobileNum(phone)) {
            ToastUtils.showSafeToast(LoginActivity.this, "请核对手机号码");
            return;
        }

        String pwd = mEtxtPwd.getText().toString().trim();
        if (TextUtils.isEmpty(pwd)) {
            ToastUtils.showSafeToast(LoginActivity.this, "请输入密码");
            return;
        }

        loginlogic(phone, pwd);


        /**
         * 真实的项目这里很有可能需要对密码进行加密.基本完成代码如下
         */
        /********开始********************/
        //        Map<String, String> params = new HashMap<>();
        //        params.put("phone", phone);
        //        params.put("password", pwd);
        //
        //        OkHttpUtils.post().url(HttpContants.LOGIN).params(params).build().execute(new
        // Callback<LoginRespMsg<User>>() {
        //            @Override
        //            public LoginRespMsg<User> parseNetworkResponse(Response response, int id)
        // throws
        //                    Exception {
        //
        //                String string = response.body().string();
        //                LoginRespMsg loginRespMsg = new Gson().fromJson(string, LoginRespMsg
        // .class);
        //                return loginRespMsg;
        //
        //            }
        //
        //            @Override
        //            public void onError(Call call, Exception e, int id) {
        //            }
        //
        //            @Override
        //            public void onResponse(LoginRespMsg<User> response, int id) {
        //
        //                MallShoppingApp application = MallShoppingApp.getInstance();
        //                application.putUser(response.getData(), response.getToken());
        //                if (application.getIntent()==null) {
        //                    setResult(RESULT_OK);
        //                    finish();
        //                }else {
        //                    application.jumpToTargetActivity(LoginActivity.this);
        //                    finish();
        //                }
        //
        //            }
        //        });

        /********结束********************/
    }

    private void loginlogic(String phone, String pwd) {

        List<User> mUserDataList = DataManager.queryUser(phone);
        if (mUserDataList != null && mUserDataList.size() > 0) {

            String netPwd = mUserDataList.get(0).getPwd();
            Long netUserId = mUserDataList.get(0).getUserId();

            if (pwd.equals(netPwd)) {
                ToastUtils.showSafeToast(LoginActivity.this, "登录成功");
                MallShoppingApp application = MallShoppingApp.getInstance();

                com.platform.app.bean.User user = new com.platform.app.bean.User();
                user.setMobi(phone);
                user.setUsername("非洲小白脸");
                user.setId(netUserId);
                user.setLogo_url(imageUrl);

                application.putUser(user, "12345678asfghdssa");
                finish();

//                if (application.getIntent() == null) {
//                    setResult(RESULT_OK);
//                    finish();
//                } else {
//                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
//                    finish();
//                }
            } else {
                ToastUtils.showSafeToast(LoginActivity.this, "密码不准确");
            }
        } else {
            ToastUtils.showSafeToast(LoginActivity.this, "用户不存在");
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
