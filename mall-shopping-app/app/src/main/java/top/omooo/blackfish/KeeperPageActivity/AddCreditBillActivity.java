package top.omooo.blackfish.KeeperPageActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.GridBankCardAdapter;
import top.omooo.blackfish.bean.BankCardsInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/23.
 */

public class AddCreditBillActivity extends BaseActivity {

    private ImageView mImageBack;
    private RelativeLayout handedLayout,importEmailLayout, importNetSilverLayout;
    private TextView mTextQQEmail, mTextOtherEmail;
    private GridView mGridBankCards;

    private List<BankCardsInfo> mBankCardsInfos;
    private AnalysisJsonUtil mJsonUtil;
    private android.os.Handler mHandler;
    private SimpleAdapter mSimpleAdapter;

    private Context mContext;
    private AdjustViewUtil mAdjustViewUtil;

    private static final String TAG = "AddCreditBillActivity";

    @Override
    public int getLayoutId() {
        return R.layout.activity_credit_bill_layout;
    }

    @Override
    public void initViews() {

        mContext = AddCreditBillActivity.this;
        mImageBack = findView(R.id.iv_keeper_add_credit_bill_back);
        handedLayout = findView(R.id.rl_handed);
        importEmailLayout = findView(R.id.rl_import_email);
        importNetSilverLayout = findView(R.id.rl_import_net_silver);
        mTextQQEmail = findView(R.id.tv_qq_email);
        mTextOtherEmail = findView(R.id.tv_other_email);

        mGridBankCards = findView(R.id.gv_bank_cards);

        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustTextViewPic(mTextQQEmail, 0, 0, 0, 130, 130);
        mAdjustViewUtil.adjustTextViewPic(mTextOtherEmail, 0, 0, 0, 130, 130);


        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        // TODO: 2018/3/25 妈耶数据显示不出来？
                        mGridBankCards.setAdapter(new GridBankCardAdapter(mBankCardsInfos,mContext));
                        Log.i(TAG, "handleMessage: " + mBankCardsInfos.size() + mBankCardsInfos.get(0).getName());
                        break;
                    default:break;
                }
                return false;
            }
        });
    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        handedLayout.setOnClickListener(this);
        importEmailLayout.setOnClickListener(this);
        importNetSilverLayout.setOnClickListener(this);
        mTextQQEmail.setOnClickListener(this);
        mTextOtherEmail.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_keeper_add_credit_bill_back:
                finish();
                break;
            case R.id.rl_handed:
                startActivity(new Intent(mContext, CreateCreditBillActivity.class));
                break;
            case R.id.rl_import_email:
                CustomToast.show(mContext, "邮箱导入");
                break;
            case R.id.rl_import_net_silver:
                CustomToast.show(mContext, "网银导入");
                break;
            case R.id.tv_qq_email:
                CustomToast.show(mContext, "QQ邮箱");
                break;
            case R.id.tv_other_email:
                CustomToast.show(mContext,"其他邮箱");
                break;

            default:break;
        }
    }

    @Override
    protected void initData() {
        mBankCardsInfos = new ArrayList<>();
        mJsonUtil = new AnalysisJsonUtil();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.bankCardsInfo, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mBankCardsInfos = mJsonUtil.getDataFromJson(result, 1);
                Log.i(TAG, "onSuccessListener: " + mBankCardsInfos.size());
                Message message = mHandler.obtainMessage(1, mBankCardsInfos);
                mHandler.sendMessage(message);
            }

            @Override
            public void onFailureListener(String result) {

            }
        });
    }
}
