package top.omooo.blackfish.MinePageActivity;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.TestActivity;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by lenovo on 2018/3/22.
 */

public class MineSettingActivity extends BaseActivity {


    private Context mContext;

    private ImageView mImageBack;

    private TextView mTextLoginPwd;
    private TextView mTextPayPwd;
    private TextView mTextManagerAddress;
    private TextView mTextExitLogin;

    private String phone = "18800209572";

    @Override
    public int getLayoutId() {
        return R.layout.activity_mine_setting_layout;
    }

    @Override
    public void initViews() {
        mContext = MineSettingActivity.this;
        mTextLoginPwd = findView(R.id.tv_setting_login_pwd);
        mTextPayPwd = findView(R.id.tv_setting_pay_pwd);
        mTextManagerAddress = findView(R.id.tv_setting_manger_address);
        mTextExitLogin = findView(R.id.btn_setting_exit_login);

        mImageBack = findView(R.id.tv_mine_setting_back);
    }

    @Override
    public void initListener() {
        mTextLoginPwd.setOnClickListener(this);
        mTextPayPwd.setOnClickListener(this);
        mTextManagerAddress.setOnClickListener(this);
        mTextExitLogin.setOnClickListener(this);
        mImageBack.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.tv_setting_login_pwd:
                intent = new Intent(this, SetPwdActivity.class);
                intent.putExtra("phoneNumber", phone);
                startActivity(intent);
                break;
            case R.id.tv_setting_pay_pwd:
                CustomToast.show(mContext, "待开发");
                break;
            case R.id.tv_setting_manger_address:
                skipActivity(new Intent(this, ManagerAddressActivity.class));
                break;
            case R.id.btn_setting_exit_login:
                startActivity(new Intent(this, TestActivity.class));
                break;
            case R.id.tv_mine_setting_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void initData() {

    }
}
