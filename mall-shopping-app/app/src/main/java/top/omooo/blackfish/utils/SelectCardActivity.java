package top.omooo.blackfish.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.SelectBankCardAdapter;
import top.omooo.blackfish.bean.BankCardsInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;

/**
 * Created by SSC on 2018/3/27.
 */

public class SelectCardActivity extends BaseActivity {

    private ImageView mImageBack;
    private RecyclerView mRecyclerView;
    private Context mContext;
    private List<BankCardsInfo> mCardsInfoList;
    private AnalysisJsonUtil jsonUtil;

    private static final String TAG = "SelectCardActivity";

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    Log.i(TAG, "handleMessage: " + mCardsInfoList.size());
                    SelectBankCardAdapter adapter = new SelectBankCardAdapter(mContext, mCardsInfoList);
                    adapter.setOnItemClickListener(new SelectBankCardAdapter.OnItemLayoutClickListener() {
                        @Override
                        public void onItemClick(int position) {
                            String cardName = mCardsInfoList.get(position).getName();
                            Log.i(TAG, "onItemClick: " + cardName);
                            Intent intent = new Intent();
                            intent.putExtra("cardName", cardName);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
                    mRecyclerView.setAdapter(adapter);
                    break;
                default:break;
            }
            return false;
        }

    });

    @Override
    public int getLayoutId() {
        return R.layout.activity_select_card_layout;
    }

    @Override
    public void initViews() {
        mImageBack = findView(R.id.tv_select_card_back);
        mRecyclerView = findView(R.id.rv_select_card_list);
        mContext = SelectCardActivity.this;
        mCardsInfoList = new ArrayList<>();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);


    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_select_card_back:
                finish();
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {
        jsonUtil = new AnalysisJsonUtil();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.bankCardsInfo, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mCardsInfoList = jsonUtil.getDataFromJson(result, 1);
                Log.i(TAG, "onSuccessListener: " + mCardsInfoList.size());
                Message message = mHandler.obtainMessage(0x01, mCardsInfoList);
                mHandler.sendMessage(message);
            }

            @Override
            public void onFailureListener(String result) {

            }
        });
    }

}
