package top.omooo.blackfish.MallPagerActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.ClassifyCommonAdapter;
import top.omooo.blackfish.adapter.ClassifyTitleAdapter;
import top.omooo.blackfish.bean.ClassifyGoodsInfo;
import top.omooo.blackfish.bean.ClassifyGridInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.view.GridViewForScroll;
import top.omooo.router_annotations.Router;

/**
 * Created by SSC on 2018/4/5.
 */
@Router("classifyGoods")
public class ClassifyGoodsActivity extends BaseActivity {

    private Context mContext;

    private RecyclerView mRecyclerViewLeft;
    private SimpleDraweeView mDraweeViewHeader;
    private GridViewForScroll mGridCommon;
    private GridViewForScroll mGridHot;
    private ImageView mImageBack, mImageMsg;
    private RelativeLayout mLayoutHeader;

    private ArrayList<String> mListTitles;

    private List<ClassifyGoodsInfo> mClassifyGoodsInfos;
    private ClassifyTitleAdapter mTitleAdapter;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    Log.i(TAG, "handleMessage: " + mClassifyGoodsInfos.size());
                    for (int i = 0; i < mClassifyGoodsInfos.size(); i++) {
                        String title = mClassifyGoodsInfos.get(i).getTitle();
                        mListTitles.add(title);
                        if (i == 0) {
                            setItemData(i);
                        }
                    }
                    mTitleAdapter = new ClassifyTitleAdapter(mContext, mListTitles);
                    mTitleAdapter.setOnClassifyItemClickListener(new ClassifyTitleAdapter.OnClassifyItemClickListener() {
                        @Override
                        public void onItemClick(int position) {
                            Log.i(TAG, "onItemClick: " + position);
                            // TODO: 2018/4/6 滑动错位
                            for (int i = 0; i < mRecyclerViewLeft.getChildCount(); i++) {
                                FrameLayout frameLayout = (FrameLayout) mRecyclerViewLeft.getChildAt(i);
                                TextView textView = (TextView) frameLayout.getChildAt(0);
                                if (i == position) {
                                    frameLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                    textView.setTextColor(Color.parseColor("#FECD15"));
                                } else {
                                    frameLayout.setBackgroundColor(Color.parseColor("#FAFAFA"));
                                    textView.setTextColor(Color.parseColor("#222222"));
                                }
                            }
                            setItemData(position);
                        }
                    });
                    mRecyclerViewLeft.setAdapter(mTitleAdapter);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private static final String TAG = "ClassifyGoodsActivity";

    @Override
    public int getLayoutId() {
        return R.layout.activity_classify_goods_layout;
    }

    @Override
    public void initViews() {
        mContext = ClassifyGoodsActivity.this;

        mRecyclerViewLeft = findView(R.id.rv_classify_goods_left_title);
        mRecyclerViewLeft.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewLeft.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mDraweeViewHeader = findView(R.id.iv_classify_goods_details_header);
        mGridCommon = findView(R.id.gv_classify_common);
        mGridHot = findView(R.id.gv_classify_hot);

        mLayoutHeader = findView(R.id.rl_classify_header_layout);
        mImageBack = findView(R.id.iv_classify_goods_back);
        mImageMsg = findView(R.id.iv_classify_header_msg);
    }

    @Override
    public void initListener() {
        mLayoutHeader.setOnClickListener(this);
        mImageMsg.setOnClickListener(this);
        mImageBack.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.rl_classify_header_layout:
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                break;
            case R.id.iv_classify_goods_back:
                finish();
                overridePendingTransition(R.anim.activity_banner_left_in, R.anim.activity_banner_right_out);
                break;
            case R.id.iv_classify_header_msg:
                Toast.makeText(mContext, "消息中心", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void initData() {

        mClassifyGoodsInfos = new ArrayList<>();
        mListTitles = new ArrayList<>();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.classifyGoodsUrl, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                AnalysisJsonUtil jsonUtil = new AnalysisJsonUtil();
                mClassifyGoodsInfos = jsonUtil.getDataFromJson(result, 2);
                Message message = mHandler.obtainMessage(0x01, mClassifyGoodsInfos);
                mHandler.sendMessage(message);
            }

            @Override
            public void onFailureListener(String result) {
                Log.i(TAG, "onFailureListener: 网络请求失败");
            }
        });
    }

    private void setItemData(int position) {
        List<ClassifyGridInfo> mGridInfosCommon = new ArrayList<>();
        List<ClassifyGridInfo> mGridInfosHot = new ArrayList<>();
        String headerImageUrl;
        headerImageUrl = mClassifyGoodsInfos.get(position).getHeaderImageUrl();
        int commonSize = mClassifyGoodsInfos.get(position).getGridImageUrls1().size();
        int hotSize = mClassifyGoodsInfos.get(position).getGridImageUrls2().size();
        for (int j = 0; j < commonSize; j++) {
            mGridInfosCommon.add(mClassifyGoodsInfos.get(position).getGridImageUrls1().get(j));
        }
        for (int j = 0; j < hotSize; j++) {
            mGridInfosHot.add(mClassifyGoodsInfos.get(position).getGridImageUrls2().get(j));
        }
        mDraweeViewHeader.setImageURI(headerImageUrl);
        mGridCommon.setAdapter(new ClassifyCommonAdapter(mContext, mGridInfosCommon));
        mGridHot.setAdapter(new ClassifyCommonAdapter(mContext, mGridInfosHot));

    }
}
