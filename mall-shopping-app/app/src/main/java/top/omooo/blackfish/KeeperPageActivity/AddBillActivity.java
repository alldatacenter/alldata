package top.omooo.blackfish.KeeperPageActivity;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/23.
 */

public class AddBillActivity extends BaseActivity {

    private ImageView mImageBack;
    private RelativeLayout mAddBankCardBillLayout,mAddWdLayout,mAddZfbBillLayout, mAddGjjBillLayout;

    private Context mContext;
    @Override
    public int getLayoutId() {
        return R.layout.activity_add_bill_layout;
    }

    @Override
    public void initViews() {
        mContext = AddBillActivity.this;

        mImageBack = findView(R.id.iv_keeper_add_bill_back);
        mAddBankCardBillLayout = findView(R.id.rl_add_bank_bill);
        mAddWdLayout = findView(R.id.rl_add_wd_bill);
        mAddZfbBillLayout = findView(R.id.rl_add_zfb_bill);
        mAddGjjBillLayout = findView(R.id.rl_add_gjj_bill);
    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mAddBankCardBillLayout.setOnClickListener(this);
        mAddWdLayout.setOnClickListener(this);
        mAddZfbBillLayout.setOnClickListener(this);
        mAddGjjBillLayout.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_keeper_add_bill_back:
                finish();
                break;
            case R.id.rl_add_bank_bill:
                startActivity(new Intent(this, AddCreditBillActivity.class));
                break;
            case R.id.rl_add_wd_bill:
                CustomToast.show(mContext,"网贷账单");
                break;
            case R.id.rl_add_zfb_bill:
                CustomToast.show(mContext,"支付宝");
                break;
            case R.id.rl_add_gjj_bill:
                CustomToast.show(mContext,"公积金");
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }
}
