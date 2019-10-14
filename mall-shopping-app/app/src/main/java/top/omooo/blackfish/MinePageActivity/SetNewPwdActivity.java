package top.omooo.blackfish.MinePageActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;

import java.sql.Connection;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.LoginActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.SqlOpenHelperUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/26.
 */

public class SetNewPwdActivity extends BaseActivity {

    private Context mContext;
    private ImageView mImageBack;
    private EditText mEditPwd;
    private Button mButtonSubmit;
    private LottieAnimationView mLottieAnimationView;

    private String phone;

    private SqlOpenHelperUtil mSqlOpenHelperUtil;
    private Connection mConnection;

    @Override
    public int getLayoutId() {
        return R.layout.activity_mine_set_new_pwd_layout;
    }

    @Override
    public void initViews() {
        mContext = SetNewPwdActivity.this;
        phone = getIntent().getStringExtra("phone");

        mImageBack = findView(R.id.tv_mine_set_new_pwd_back);
        mEditPwd = findView(R.id.et_mine_set_new_pwd);
        mButtonSubmit = findView(R.id.btn_mine_set_new_pwd_submit);
        mLottieAnimationView = findView(R.id.lottie_set_new_pwd_success);
    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mButtonSubmit.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_mine_set_new_pwd_back:
                finish();
                break;
            case R.id.btn_mine_set_new_pwd_submit:
                String newPwd = mEditPwd.getText().toString();
                if (newPwd.length() >= 8 && newPwd.matches("^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{8,16}$")) {
                    new SqlAsyncTask().execute(new String[]{phone, newPwd});
                    // TODO: 2018/3/26 强制退出重新登录
                    Intent intent = new Intent(mContext, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                    startActivity(intent);
                    finish();
                } else {
                    CustomToast.show(mContext, "密码必须为8-16位，且不能为纯字母或纯数字");
                }
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }

    class SqlAsyncTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                CustomToast.show(mContext,"密码重置成功");
            } else {
                CustomToast.show(mContext,"密码重置失败");
            }
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String phone = strings[0];
            String pwd = strings[1];
            mSqlOpenHelperUtil = new SqlOpenHelperUtil();
            mConnection = mSqlOpenHelperUtil.connDB();
            if (null != mConnection) {
                String sql = "update userinfo set password='" + pwd + "' where phone='" + phone + "'";
                return mSqlOpenHelperUtil.updateDB(mConnection, sql);
            } else {
                return false;
            }
        }
    }

}
