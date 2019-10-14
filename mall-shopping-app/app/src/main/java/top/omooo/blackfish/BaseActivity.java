package top.omooo.blackfish;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;

import org.greenrobot.eventbus.EventBus;

import top.omooo.blackfish.R;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/16.
 */

public abstract class BaseActivity extends FragmentActivity implements View.OnClickListener {

    private SparseArray<View> mView;

    public abstract int getLayoutId();

    public abstract void initViews();

    public abstract void initListener();

    public abstract void processClick(View view);

    protected abstract void initData();

    public void onClick(View view) {
        processClick(view);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new SparseArray<>();
        setContentView(getLayoutId());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        initViews();
        initListener();
        initData();
        Log.i("BaseActivity", "Activity: " + getPackageName() + "." + getLocalClassName());
    }

    /**
     * 通过ID找到View
     *
     * @param viewId
     * @param <E>
     * @return
     */
    public <E extends View> E findView(int viewId) {
        E view = (E) mView.get(viewId);
        if (view == null) {
            view = findViewById(viewId);
            mView.put(viewId, view);
        }
        return view;
    }

    public <E extends View> void setOnClick(E view) {
        view.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        CustomToast.cancelToast();
        super.onBackPressed();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
