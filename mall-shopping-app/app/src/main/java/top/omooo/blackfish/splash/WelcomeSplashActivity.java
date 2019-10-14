package top.omooo.blackfish.splash;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.MainActivity;
import top.omooo.blackfish.R;

/**
 * Created by Omooo on 2018/2/24.
 */

public class WelcomeSplashActivity extends Activity {

    private ViewPager mViewPager;
    private List mViewList;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_welcome_splash_layout);

        initView();
    }

    private void initView() {
        mViewPager = findViewById(R.id.vp_splash_guide);

        LayoutInflater inflater = LayoutInflater.from(this);

        View view1 = inflater.inflate(R.layout.view_splash_layout_1, null);
        View view2 = inflater.inflate(R.layout.view_splash_layout_2, null);
        View view3 = inflater.inflate(R.layout.view_splash_layout_3, null);
        View view4 = inflater.inflate(R.layout.view_splash_layout_4, null);

        view4.findViewById(R.id.btn_splash_view_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeSplashActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.activity_translate_right_in, R.anim.activity_translate_right_out);
                finish();
            }
        });

        mViewList = new ArrayList();
        mViewList.add(view1);
        mViewList.add(view2);
        mViewList.add(view3);
        mViewList.add(view4);

        mViewPager.setAdapter(new AdapterViewPager(mViewList));

    }

    public class AdapterViewPager extends PagerAdapter {

        private List<View> mViewList;

        public AdapterViewPager(List<View> list) {
            mViewList = list;
        }

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));
        }
    }

}
