package top.omooo.blackfish.MallPagerActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import top.omooo.blackfish.NewBaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.UrlInfoBean;

/**
 * Created by SSC on 2018/4/15.
 */

public class SubmitOrderActivity extends NewBaseActivity {

    @BindView(R.id.iv_goods)
    SimpleDraweeView mIvGoods;
    @BindView(R.id.btn_submit_order)
    Button mBtnSubmitOrder;
    @BindView(R.id.iv_back)
    ImageView mIvBack;

    private Context mContext;
    private static final String TAG = "SubmitOrderActivity";

    @Override
    public int getLayoutId() {
        return R.layout.activity_submit_order_layout;
    }

    @Override
    public void initViews() {
        getWindow().setStatusBarColor(getColor(R.color.colorKeyBoardBg));
        mContext = SubmitOrderActivity.this;

        mIvGoods.setImageURI(UrlInfoBean.dialogImage);

    }

    @Override
    protected void initData() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    @OnClick({R.id.iv_back, R.id.btn_submit_order})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finshActivity();
                break;
            case R.id.btn_submit_order:
                skipActivity(new Intent(mContext, PayTypeActivity.class));
                break;
        }
    }
}
