package top.omooo.blackfish.MallPagerActivity;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import top.omooo.blackfish.NewBaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.ShowImageAdapter;
import top.omooo.blackfish.bean.UrlInfoBean;

/**
 * Created by SSC on 2018/4/14.
 */

public class ShowImageActivity extends NewBaseActivity {

    @BindView(R.id.tv_indicator)
    TextView mTvIndicator;
    @BindView(R.id.vp_image)
    ViewPager mVpImage;
    @BindView(R.id.rl_show_image)
    RelativeLayout mRlShowImage;

    private Context mContext;
    private static final String TAG = "ShowImageActivity";

    @Override
    public int getLayoutId() {
        return R.layout.activity_show_image_layout;
    }

    @Override
    public void initViews() {
        mContext = ShowImageActivity.this;
        getWindow().setStatusBarColor(getColor(R.color.colorBlack));

        ShowImageAdapter adapter = new ShowImageAdapter(mContext, UrlInfoBean.bigImageUrls);
        mVpImage.setAdapter(adapter);
        mVpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                String text = position + 1 + "/" + UrlInfoBean.bigImageUrls.length;
                mTvIndicator.setText(text);
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mVpImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
        adapter.setOnImageClickListener(new ShowImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick() {
                finishActivity();
            }
        });
    }

    @Override
    protected void initData() {

    }


    @OnClick(R.id.rl_show_image)
    public void onViewClicked() {
        finishActivity();
    }

    private void finishActivity() {
        finish();
        overridePendingTransition(R.anim.activity_banner_left_in,R.anim.activity_banner_right_out);
    }
}
