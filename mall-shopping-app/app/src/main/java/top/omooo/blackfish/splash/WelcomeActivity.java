package top.omooo.blackfish.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.Normalizer2;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;

import top.omooo.blackfish.MainActivity;
import top.omooo.blackfish.R;

/**
 * Created by Omooo on 2018/2/24.
 */

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";
    private Handler mHandler = new Handler();
    private boolean isFirstLaunch;
    private SharedPreferences.Editor mEditor;
    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_layout);


        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        isFirstLaunch = preferences.getBoolean("firstLaunch", true);
        mEditor = preferences.edit();
        isFirstLaunch();
    }

    private void isFirstLaunch() {

        if (isFirstLaunch) {
            //APP首次安装启动
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(WelcomeActivity.this, WelcomeSplashActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    overridePendingTransition(R.anim.activity_translate_right_in, R.anim.activity_translate_right_out);
                }
            }, 1500);
            mEditor.putBoolean("firstLaunch", false);
            mEditor.commit();
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    overridePendingTransition(R.anim.activity_translate_right_in, R.anim.activity_translate_right_out);
                }
            }, 1500);
        }
    }

}
