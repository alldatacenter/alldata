package top.omooo.blackfish.MinePageActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.KeyBoardUtil;
import top.omooo.blackfish.utils.SqlOpenHelperUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/25.
 */

public class SetPwdActivity extends BaseActivity {

    private ImageView mImageBack;
    private TextView mTextTitle,mTextSubtitle, mTextForgetPwd;
    private EditText mEditPwd;
    private Button mButtonSubmit;

    private Context mContext;

    private String phone;
    private SqlOpenHelperUtil mSqlOpenHelperUtil = new SqlOpenHelperUtil();
    private Connection mConnection;
    private boolean isExist = false;
    private String oldPwd;

    private static final String TAG = "SetPwdActivity";

    @Override
    public int getLayoutId() {
        return R.layout.activity_mine_set_pwd_layout;
    }

    @Override
    public void initViews() {
        mContext = SetPwdActivity.this;
        mImageBack = findView(R.id.tv_mine_set_pwd_back);
        mTextTitle = findView(R.id.tv_mine_set_pwd_title);
        mTextSubtitle = findView(R.id.tv_mine_set_pwd_subtitle);
        mTextForgetPwd = findView(R.id.tv_mine_forget_pwd);
        mEditPwd = findView(R.id.et_mine_set_pwd);
        mButtonSubmit = findView(R.id.btn_mine_set_pwd_submit);

        phone = getIntent().getStringExtra("phoneNumber");
        Log.i(TAG, "initViews: " + phone + " " + isExist);

        // TODO: 2018/3/26  第一次进入之后用SharedPreferences保存是否存在密码，没必要每次进入都要做SQL操作 我都觉得烦！
        //执行异步任务判断是否存在密码，根据是否存在显示不同UI
        new SqlAsyncTask().execute(phone);

        KeyBoardUtil.showKeyBoard(mEditPwd);
    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mTextForgetPwd.setOnClickListener(this);
        mButtonSubmit.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_mine_set_pwd_back:
                finish();
                break;
            case R.id.tv_mine_forget_pwd:
                CustomToast.show(this,"忘记密码");
                break;
            case R.id.btn_mine_set_pwd_submit:
                String pwd = mEditPwd.getText().toString();
                if (null == pwd || pwd.length() < 8) {
                    CustomToast.show(this, "密码不少于八位，请重新填写");
                    break;
                } else {
                    if (isExist) {
                        //存在密码，填写旧密码密码
                        new SqlAsyncTask().execute(new String[]{phone, pwd, "flag"});
                    } else {
                        //不存在密码，设置密码
                        new SqlAsyncTask().execute(new String[]{phone, pwd});
                        finish();
                    }
                }
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }


    class SqlAsyncTask extends AsyncTask<String, Void, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i(TAG, "onPostExecute: " + s);
            if ("false".equals(s)) {
                isExist = false;
                mTextTitle.setText("设置登录密码");
                mTextSubtitle.setText("请设置登录密码");
                mEditPwd.setHint("输入登录密码");
                mButtonSubmit.setText("提交");
            } else if ("true".equals(s)) {
                isExist = true;
                mTextTitle.setText("修改登录密码");
                mTextSubtitle.setText("请输入旧登录密码");
                mEditPwd.setHint("输入当前登录密码");
                mButtonSubmit.setText("下一步");
                mTextForgetPwd.setVisibility(View.VISIBLE);
            } else if ("连接失败".equals(s)) {
                Log.i(TAG, "onPostExecute: 数据库连接失败");
            } else if ("设置密码成功".equals(s)) {
                CustomToast.show(mContext, s);
            } else if ("设置密码失败".equals(s)) {
                CustomToast.show(mContext, s);
            } else if ("参数出错".equals(s)) {
                Log.i(TAG, "onPostExecute: /********参数出错*********/");
            } else if ("旧密码填写错误".equals(s)) {
                CustomToast.show(mContext, "请输入正确的登录密码");
            } else if ("旧密码填写正确".equals(s)) {
                //跳转到设置新密码界面
                Intent intent = new Intent(mContext, SetNewPwdActivity.class);
                intent.putExtra("phone", phone);
                startActivity(intent);
                finish();
            } else {
                return;
            }

        }

        /**
         * 以参数的长度做标识 strings.length
         * 1 ---> 判断密码是否存在
         * 2 ---> 设置密码
         * 3 ---> 判断旧密码是否正确
         */
        @Override
        protected String doInBackground(String... strings) {
            String phoneNumber = strings[0];
            int length = strings.length;
            Log.i(TAG, "doInBackground: strings.length：" + length);
            mConnection = mSqlOpenHelperUtil.connDB();
            if (mConnection != null) {
                if (length == 1) {
                    //判断是否存在密码
                    String sql = "select password from userinfo where phone= '" + phoneNumber + "'";
                    ResultSet resultSet = mSqlOpenHelperUtil.executeSql(mConnection, sql);
                    String pwd = null;
                    try {
                        while (resultSet.next()) {
                            pwd = resultSet.getString("password");
                        }
                        if (null == pwd || pwd.equals("")) {
                            //不存在密码
                            return "false";
                        } else {
                            //存在密码
                            oldPwd = pwd;
                            Log.i(TAG, "doInBackground: 旧密码为：" + oldPwd);
                            return "true";
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else if (length == 2) {
                    //设置密码
                    String phone = strings[0];
                    String pwd = strings[1];
                    String sql = "update userinfo set password='" + pwd + "' where phone='" + phone + "'";
                    boolean isFinish = mSqlOpenHelperUtil.updateDB(mConnection, sql);
                    if (isFinish) {
                        return "设置密码成功";
                    } else {
                        return "设置密码失败";
                    }
                } else if (length == 3) {
                    //修改密码,先判断旧密码是否正确
                    String pwd = strings[1];
                    if (pwd.equals(oldPwd)) {
                        //旧密码填写正确
                        return "旧密码填写正确";
                    } else {
                        return "旧密码填写错误";
                    }
                } else {
                    return "参数出错";
                }
            } else {
                return "连接失败";
            }
            return null;
        }
    }
}
