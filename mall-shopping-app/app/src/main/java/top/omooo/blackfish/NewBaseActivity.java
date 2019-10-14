package top.omooo.blackfish;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/4/10.
 */

public abstract class NewBaseActivity extends FragmentActivity {

    private Unbinder mUnbinder;

    public abstract int getLayoutId();

    public abstract void initViews();

    protected abstract void initData();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mUnbinder = ButterKnife.bind(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        initViews();
        initData();
        Log.i("BaseActivity", "Activity: " + getPackageName() + "." + getLocalClassName());
    }

    @Override
    public void onBackPressed() {
        CustomToast.cancelToast();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUnbinder.unbind();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            overridePendingTransition(R.anim.activity_banner_left_in, R.anim.activity_banner_right_out);
        }
        return true;
    }

    public void skipActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
    }

    public void finshActivity() {
        finish();
        overridePendingTransition(R.anim.activity_banner_left_in, R.anim.activity_banner_right_out);
    }

}
