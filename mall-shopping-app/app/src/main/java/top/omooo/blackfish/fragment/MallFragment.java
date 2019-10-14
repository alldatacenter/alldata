package top.omooo.blackfish.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.SingleLayoutHelper;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import top.omooo.blackfish.BaseWebViewActivity;
import top.omooo.blackfish.GoodsDetailActivity;
import top.omooo.blackfish.MallPagerActivity.ClassifyGoodsActivity;
import top.omooo.blackfish.MallPagerActivity.SearchActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.GeneralVLayoutAdapter;
import top.omooo.blackfish.adapter.GridOnlyImageAdapter;
import top.omooo.blackfish.adapter.MallHotClassifyGridAdapter;
import top.omooo.blackfish.adapter.RecommendGoodsAdapter;
import top.omooo.blackfish.bean.BannerInfo;
import top.omooo.blackfish.bean.GridInfo;
import top.omooo.blackfish.bean.MallGoodsInfo;
import top.omooo.blackfish.bean.MallGoodsItemInfo;
import top.omooo.blackfish.bean.MallHotClassifyGridInfo;
import top.omooo.blackfish.bean.MallPagerInfo;
import top.omooo.blackfish.bean.RecommendGoodsInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.listener.OnViewItemClickListener;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.utils.SpannableStringUtil;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.GridViewForScroll;
import top.omooo.blackfish.view.RecycleViewBanner;
import top.omooo.router.EasyRouter;

/**
 * Created by Omooo on 2018/2/25.
 */

public class MallFragment extends BaseFragment {

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private Context mContext;

    private Toolbar mToolbar;
    private ImageView mImageMenu, mImageMsg;
    private RelativeLayout mHeaderLayout;

    private RecycleViewBanner mBanner;
    private List<BannerInfo> mBannerInfos;

    final List<DelegateAdapter.Adapter> adapters = new LinkedList<>();
    private static final String TAG = "MallFragment";
    private VirtualLayoutManager layoutManager;
    private RecyclerView.RecycledViewPool viewPool;
    private DelegateAdapter delegateAdapter;

    private SimpleDraweeView mImageGridItem;
    private TextView mTextGridItem;

    private AnalysisJsonUtil mJsonUtil = new AnalysisJsonUtil();
    private List<MallPagerInfo> mMallPagerInfos;
    private List<GridInfo> mGridInfos;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    addItemViews(mMallPagerInfos.get(0), mOnViewItemClickListener);

                    break;
                default:
                    break;
            }
            if (mRefreshLayout.isRefreshing()) {
                mRefreshLayout.setRefreshing(false);
            }
            return false;
        }
    });

    private SpannableStringUtil mSpannableStringUtil = new SpannableStringUtil();

    public static MallFragment newInstance() {
        return new MallFragment();
    }


    @Override
    public int getLayoutId() {
        return R.layout.fragment_mall_layout;
    }

    @Override
    public void initViews() {

        getActivity().getWindow().setStatusBarColor(Color.parseColor("#00000000"));
        mToolbar = findView(R.id.toolbar_mall);
        mToolbar.getBackground().setAlpha(0);

        mContext = getActivity();
        mRecyclerView = findView(R.id.rv_fragment_mall_container);
        mRefreshLayout = findView(R.id.swipe_container);

        mImageMenu = findView(R.id.iv_mall_header_menu);
        mImageMsg = findView(R.id.iv_mall_header_msg);
        mHeaderLayout = findView(R.id.rl_mall_header_layout);

        layoutManager = new VirtualLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);

        viewPool = new RecyclerView.RecycledViewPool();
        mRecyclerView.setRecycledViewPool(viewPool);
        viewPool.setMaxRecycledViews(0, 20);

        delegateAdapter = new DelegateAdapter(layoutManager, false);
        mRecyclerView.setAdapter(delegateAdapter);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initData();
            }
        });
    }

    private void addItemViews(final MallPagerInfo mallPagerInfo, final OnViewItemClickListener listener) {

        //轮播图
        SingleLayoutHelper bannerHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter bannerAdapter = new GeneralVLayoutAdapter(mContext, bannerHelper, 1) {

            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_banner_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                mBanner = holder.itemView.findViewById(R.id.rvb_mall_header);
                mBannerInfos = mallPagerInfo.getBannerInfos();
                mBanner.setRvBannerData(mBannerInfos);
                mBanner.setOnSwitchRvBannerListener(new RecycleViewBanner.OnSwitchRvBannerListener() {
                    @Override
                    public void switchBanner(int position, SimpleDraweeView simpleDraweeView) {
                        simpleDraweeView.setImageURI(mBannerInfos.get(position).getUrl());
                    }
                });
                mBanner.setOnBannerClickListener(new RecycleViewBanner.OnRvBannerClickListener() {
                    @Override
                    public void onClick(int position) {
                        Intent intent = new Intent(mContext, BaseWebViewActivity.class);
                        intent.putExtra("loadUrl", "https://github.com/omooo");
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                    }
                });
            }
        };
        adapters.add(bannerAdapter);

        GridLayoutHelper gridLayoutHelper = new GridLayoutHelper(5);
        GeneralVLayoutAdapter gridAdapter = new GeneralVLayoutAdapter(mContext, gridLayoutHelper, 10) {
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_two_line_grid, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, final int position) {
                super.onBindViewHolder(holder, position);
                mImageGridItem = holder.itemView.findViewById(R.id.iv_mall_grid_item);
                mTextGridItem = holder.itemView.findViewById(R.id.tv_mall_grid_item);
                mGridInfos = mallPagerInfo.getClassifyInfos();
                mImageGridItem.setImageURI(mGridInfos.get(position).getImageUrl());
                mTextGridItem.setText(mGridInfos.get(position).getTitle());

                mImageGridItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick("ClassifyGridItem" + position);
                    }
                });
            }
        };
        adapters.add(gridAdapter);

        SingleLayoutHelper fourGoodsHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter fourGoodsAdapter = new GeneralVLayoutAdapter(mContext, fourGoodsHelper, 1) {

            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_four_goods_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                SimpleDraweeView headerImage = holder.itemView.findViewById(R.id.iv_four_header_image);
                GridViewForScroll gridFourGoods = holder.itemView.findViewById(R.id.gv_four_goods);
                gridFourGoods.setAdapter(new GridOnlyImageAdapter(mContext, mallPagerInfo.getGridGoodsInfos()));
                headerImage.setImageURI(mallPagerInfo.getSingleImageUrl());

                headerImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick("SingleHeaderImage");
                    }
                });
            }
        };
        adapters.add(fourGoodsAdapter);

        GridLayoutHelper gridHotClassifyHelper = new GridLayoutHelper(1);
        GeneralVLayoutAdapter hotClassifyAdapter = new GeneralVLayoutAdapter(mContext, gridHotClassifyHelper, mallPagerInfo.getMallGoodsInfos().size()) {
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_hot_classify_grid_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, final int position) {
                super.onBindViewHolder(holder, position);

                List<MallHotClassifyGridInfo> mallHotClassifyGridInfos = new ArrayList<>();

                SimpleDraweeView headerImage = holder.itemView.findViewById(R.id.iv_hot_classify_header_image);
                GridViewForScroll gridGoods = holder.itemView.findViewById(R.id.gv_hot_classify);
                List<MallGoodsInfo> mallGoodsInfos = mallPagerInfo.getMallGoodsInfos();
                headerImage.setImageURI(mallGoodsInfos.get(position).getHeaderImageUrl());

                headerImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick("HotGoodsHeaderImage" + position);
                    }
                });

                int itemSize = mallGoodsInfos.get(position).getMallGoodsItemInfos().size();
                Log.i(TAG, "onBindViewHolder: " + itemSize);
                for (int i = 0; i < itemSize; i++) {
                    MallGoodsItemInfo mallGoodsItemInfo = mallGoodsInfos.get(position).getMallGoodsItemInfos().get(i);
                    String goodsImage = mallGoodsItemInfo.getImageUrl();
                    String goodsDesc = mallGoodsItemInfo.getDesc();
                    String goodsPeriods = "¥" + mallGoodsItemInfo.getSinglePrice() + " x " + mallGoodsItemInfo.getPeriods() + "期";
                    String goodsPrice = "¥" + mallGoodsItemInfo.getPrice();
                    mallHotClassifyGridInfos.add(new MallHotClassifyGridInfo(goodsImage, goodsDesc, goodsPeriods, goodsPrice));
                }
                MallHotClassifyGridAdapter adapter = new MallHotClassifyGridAdapter(mContext, mallHotClassifyGridInfos);
                adapter.setOnViewItemClickListener(mOnViewItemClickListener);
                gridGoods.setAdapter(adapter);

            }
        };
        adapters.add(hotClassifyAdapter);

        SingleLayoutHelper recoHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter recoAdapter = new GeneralVLayoutAdapter(mContext, recoHelper, 1) {
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.mall_pager_recommend_goods_list, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                RecyclerView recyclerView = holder.itemView.findViewById(R.id.rv_mall_recommend);
                List<RecommendGoodsInfo> recommendGoodsInfos = mallPagerInfo.getRecommendGoodsInfos();
                recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
                recyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
                recyclerView.setAdapter(new RecommendGoodsAdapter(mContext, recommendGoodsInfos));
            }
        };
        adapters.add(recoAdapter);

        delegateAdapter.setAdapters(adapters);
    }

    @Override
    public void initListener() {
        mImageMenu.setOnClickListener(this);
        mImageMsg.setOnClickListener(this);
        mHeaderLayout.setOnClickListener(this);
    }

    @Override
    public void initData() {
        mJsonUtil = new AnalysisJsonUtil();
        mMallPagerInfos = new ArrayList<>();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.mallGoodsUrl, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mMallPagerInfos = mJsonUtil.getDataFromJson(result, 3);

                Message message = mHandler.obtainMessage(0x01, mMallPagerInfos);
                mHandler.sendMessage(message);

            }

            @Override
            public void onFailureListener(String result) {
                Log.i(TAG, "onFailureListener: " + result);
            }
        });
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_mall_header_menu:
                startActivity(new Intent(mContext, ClassifyGoodsActivity.class));
                getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                break;
            case R.id.iv_mall_header_msg:
                CustomToast.show(mContext, "消息中心");
                break;
            case R.id.rl_mall_header_layout:
                skipActivity(new Intent(getActivity(), SearchActivity.class));
                break;
            default:
                break;
        }
    }

    private OnViewItemClickListener mOnViewItemClickListener = new OnViewItemClickListener() {
        @Override
        public void onItemClick(String id) {
            for (int i = 0; i < 10; i++) {
                if (id.equals("ClassifyGridItem" + i)) {
                    CustomToast.show(mContext, "第 " + i + " 个Item被点击");
                    return;
                }
            }
            if (id.equals("SingleHeaderImage")) {
                Intent intent = new Intent(mContext, BaseWebViewActivity.class);
                intent.putExtra("loadUrl", "https://h5.blackfish.cn/m/promotion/2/89?line&memberId=18800209572&deviceId=f39498916c9b5cda");
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                return;
            }
            for (int i = 0; i < 5; i++) {
                if (id.equals("HotGoodsHeaderImage" + i)) {
                    Intent intent = new Intent(mContext, BaseWebViewActivity.class);
                    intent.putExtra("loadUrl", UrlInfoBean.hotGoodsHeaderUrls[i]);
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                    return;
                }
            }
            if (id.equals("HotGoodsItem")) {
                startActivity(new Intent(mContext, GoodsDetailActivity.class));
                getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                return;
            }
        }
    };
}
