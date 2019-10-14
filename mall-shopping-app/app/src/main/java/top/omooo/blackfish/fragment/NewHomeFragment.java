package top.omooo.blackfish.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
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
import top.omooo.blackfish.MallPagerActivity.ClassifyGoodsActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.GeneralVLayoutAdapter;
import top.omooo.blackfish.bean.BannerInfo;
import top.omooo.blackfish.bean.HomeSortInfo;
import top.omooo.blackfish.bean.HomeSortItemInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.RecycleViewBanner;

/**
 * Created by SSC on 2018/3/16.
 */

public class NewHomeFragment extends BaseFragment{

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mRecyclerView;
    private Context mContext;

    final List<DelegateAdapter.Adapter> adapters = new LinkedList<>();
    private static final String TAG = "NewHomeFragment";
    private VirtualLayoutManager layoutManager;
    private RecyclerView.RecycledViewPool viewPool;
    private DelegateAdapter delegateAdapter;

    private List<BannerInfo> mBannerInfos;

    private List<HomeSortInfo> mHomeSortInfos;
    private List<HomeSortItemInfo> mHomeSortItemInfos;
    private Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0x01) {
                addItemViews(mHomeSortInfos);
                if (mRefreshLayout.isRefreshing()){
                    mRefreshLayout.setRefreshing(false);
                }
            }
            return false;
        }
    });

    private Toolbar mToolbar;

    private ImageView mImageHeaderMsg;

    private RecycleViewBanner mRecycleViewBanner;

    public static NewHomeFragment newInstance() {
        return new NewHomeFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_home_layout;
    }

    @Override
    public void initViews() {
        getActivity().getWindow().setStatusBarColor(Color.parseColor("#00000000"));

        mImageHeaderMsg = findView(R.id.iv_home_header_msg);

        mToolbar = findView(R.id.toolbar_home);
        mToolbar.getBackground().setAlpha(0);

        RelativeLayout headerLayout = (RelativeLayout) mToolbar.getChildAt(0);

        TextView textTitle = (TextView) headerLayout.getChildAt(1);
        textTitle.setVisibility(View.GONE);

        mContext = getActivity();
        mRefreshLayout = findView(R.id.swipe_container);
        mRecyclerView = findView(R.id.rv_fragment_home_container);

        layoutManager = new VirtualLayoutManager(getActivity());
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

    private void addItemViews(final List<HomeSortInfo> homeSortInfos) {

        //首页Banner轮播图
        SingleLayoutHelper bannerLayoutHelper = new SingleLayoutHelper();
        GeneralVLayoutAdapter bannerAdapter = new GeneralVLayoutAdapter(mContext, bannerLayoutHelper, 1){
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(mContext).inflate(R.layout.home_pager_banner_layout, parent, false);
                return new MainViewHolder(view);
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                mRecycleViewBanner = holder.itemView.findViewById(R.id.rvb_home_header);
                //Banner数据
                mBannerInfos = new ArrayList<>();
                mBannerInfos.add(new BannerInfo("https://i.loli.net/2018/04/06/5ac733bc51d0a.png"));
                mBannerInfos.add(new BannerInfo("https://i.loli.net/2018/04/06/5ac735502effe.png"));
                mBannerInfos.add(new BannerInfo("https://i.loli.net/2018/04/07/5ac8459fc9b6a.png"));
                mBannerInfos.add(new BannerInfo("https://i.loli.net/2018/04/06/5ac7339ee876e.jpg"));
                mRecycleViewBanner.setRvBannerData(mBannerInfos);
                mRecycleViewBanner.setOnSwitchRvBannerListener(new RecycleViewBanner.OnSwitchRvBannerListener() {
                    @Override
                    public void switchBanner(int position, SimpleDraweeView simpleDraweeView) {
                        simpleDraweeView.setImageURI(mBannerInfos.get(position).getUrl());
                    }
                });
                mRecycleViewBanner.setOnBannerClickListener(new RecycleViewBanner.OnRvBannerClickListener() {
                    @Override
                    public void onClick(int position) {
                        toWebActivity(UrlInfoBean.homeBannerUrls[position]);
                    }
                });
            }
        };
        adapters.add(bannerAdapter);

        //网格布局，用于加载主页两行网格布局
        GridLayoutHelper gridLayoutHelper = new GridLayoutHelper(4);
        GeneralVLayoutAdapter gridAdapter = new GeneralVLayoutAdapter(getActivity(), gridLayoutHelper, 8) {
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(getActivity()).inflate(R.layout.home_pager_two_line_grid, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);

                // TODO: 2018/3/18 加载网络图片
                ImageView imageView = holder.itemView.findViewById(R.id.iv_home_one_grid_icon);
                TextView textView = holder.itemView.findViewById(R.id.title);
                if (position == 0) {
                    textView.setText("充值中心");
                    imageView.setImageResource(R.drawable.icon_voucher_center);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_1"));
                } else if (position == 1) {
                    textView.setText("手机通讯");
                    imageView.setImageResource(R.drawable.icon_phone);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_2"));
                } else if (position == 2) {
                    textView.setText("电影票");
                    imageView.setImageResource(R.drawable.icon_movie);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_3"));
                } else if (position == 3) {
                    textView.setText("全民游戏");
                    imageView.setImageResource(R.drawable.icon_game);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_4"));
                }else if (position == 4) {
                    textView.setText("代还信用卡");
                    imageView.setImageResource(R.drawable.icon_pay_card);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_5"));
                }else if (position == 5) {
                    textView.setText("现金分期");
                    imageView.setImageResource(R.drawable.icon_cash_fenqi);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_6"));
                }else if (position == 6) {
                    textView.setText("办信用卡");
                    imageView.setImageResource(R.drawable.icon_ban_card);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_7"));
                }else if (position == 7) {
                    textView.setText("全部分类");
                    imageView.setImageResource(R.drawable.icon_all_classify);
                    imageView.setOnClickListener(new MyOnClick("iv_home_one_grid_icon_8"));

                }
            }
        };
        adapters.add(gridAdapter);


        int count = homeSortInfos.size();
        GridLayoutHelper sortHelper = new GridLayoutHelper(1);
        GeneralVLayoutAdapter sortAdapter = new GeneralVLayoutAdapter(mContext, sortHelper, count){
            @Override
            public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new MainViewHolder(LayoutInflater.from(mContext).inflate(R.layout.home_pager_goods_layout, parent, false));
            }

            @Override
            public void onBindViewHolder(MainViewHolder holder, final int position) {
                super.onBindViewHolder(holder, position);
                HomeSortInfo homeSortInfo = homeSortInfos.get(position);
                TextView textTitle = holder.itemView.findViewById(R.id.tv_home_goods_title_text);
                textTitle.setText(homeSortInfo.getTitle());
                SimpleDraweeView draweeViewHeader = holder.itemView.findViewById(R.id.iv_home_goods_big_image);
                draweeViewHeader.setImageURI(homeSortInfo.getSortImageUrl());
                draweeViewHeader.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toWebActivity(UrlInfoBean.homeHeaderUrls[position]);
                    }
                });

                TextView textDira = holder.itemView.findViewById(R.id.tv_home_goods_title_image);
                if (position == 1) {
                    textDira.setBackground(mContext.getDrawable(R.drawable.shape_home_subtitle_left2));
                } else if (position == 2) {
                    textDira.setBackground(mContext.getDrawable(R.drawable.shape_home_subtitle_left3));
                } else if (position == 3) {
                    textDira.setBackground(mContext.getDrawable(R.drawable.shape_home_subtitle_left4));
                }

                RelativeLayout headerLayout = holder.itemView.findViewById(R.id.rl_goods_title_layout);
                headerLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(mContext, ClassifyGoodsActivity.class));
                        getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                    }
                });


                /**
                 * 令人恐惧的脏代码，建议用GridView，请参考商城页的写法，不想改了
                 */
                List<HomeSortItemInfo> sortItemInfos = homeSortInfo.getItemInfos();
                Log.i(TAG, "onBindViewHolder: " + sortItemInfos.size());
                SimpleDraweeView draweeViewItem1 = holder.itemView.findViewById(R.id.iv_home_goods_item_1);
                SimpleDraweeView draweeViewItem2 = holder.itemView.findViewById(R.id.iv_home_goods_item_2);
                SimpleDraweeView draweeViewItem3 = holder.itemView.findViewById(R.id.iv_home_goods_item_3);
                SimpleDraweeView draweeViewItem4 = holder.itemView.findViewById(R.id.iv_home_goods_item_4);

                draweeViewItem1.setImageURI(sortItemInfos.get(0).getGoodsImageUrl());
                draweeViewItem2.setImageURI(sortItemInfos.get(1).getGoodsImageUrl());
                draweeViewItem3.setImageURI(sortItemInfos.get(2).getGoodsImageUrl());
                draweeViewItem4.setImageURI(sortItemInfos.get(3).getGoodsImageUrl());

                draweeViewItem1.setOnClickListener(new MyOnClick("iv_home_goods_item_1"));
                draweeViewItem2.setOnClickListener(new MyOnClick("iv_home_goods_item_2"));
                draweeViewItem3.setOnClickListener(new MyOnClick("iv_home_goods_item_3"));
                draweeViewItem4.setOnClickListener(new MyOnClick("iv_home_goods_item_4"));

            }
        };
        adapters.add(sortAdapter);

        delegateAdapter.setAdapters(adapters);
    }



    @Override
    public void initListener() {
        mImageHeaderMsg.setOnClickListener(this);
    }

    @Override
    public void initData() {

        //商品数据
        mHomeSortInfos = new ArrayList<>();
        mHomeSortItemInfos = new ArrayList<>();
        final AnalysisJsonUtil jsonUtil = new AnalysisJsonUtil();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.homeGoodsUrl, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mHomeSortInfos = jsonUtil.getDataFromJson(result, 0);
                Log.i(TAG, "onSuccessListener: " + "数据条数：" + mHomeSortInfos.size());
                Message message = mHandler.obtainMessage(0x01, mHomeSortInfos);
                mHandler.sendMessage(message);
            }

            @Override
            public void onFailureListener(String result) {
                Log.i(TAG, "onFailureListener: " + "网络请求失败" + result);
            }
        });


    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_home_header_msg:
                Log.i(TAG, "processClick: 标题 Message 被点击");
                break;
            default:break;
        }
    }

    private int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    private class MyOnClick implements View.OnClickListener {

        private String id;

        public MyOnClick(String id) {
            this.id = id;
        }

        @Override
        public void onClick(View v) {
            for (int i = 1; i <= 4; i++) {
                if (id.equals("iv_home_goods_item_" + i)) {
                    CustomToast.show(mContext, "跳转到商品详情页");
                    return;
                }

            }
            switch (id) {
                case "iv_home_one_grid_icon_1":
                    CustomToast.show(mContext, "充值中心");
                    break;
                case "iv_home_one_grid_icon_2":
                    CustomToast.show(mContext, "手机通讯");
                    break;
                case "iv_home_one_grid_icon_3":
                    CustomToast.show(mContext, "电影票");
                    break;
                case "iv_home_one_grid_icon_4":
                    toWebActivity(UrlInfoBean.gameUrl);
                    break;
                case "iv_home_one_grid_icon_5":
                    CustomToast.show(mContext, "代还信用卡");
                    break;
                case "iv_home_one_grid_icon_6":
                    CustomToast.show(mContext, "现金分期");
                    break;
                case "iv_home_one_grid_icon_7":
                    toWebActivity(UrlInfoBean.bankCard);
                    break;
                case "iv_home_one_grid_icon_8":
                    startActivity(new Intent(mContext, ClassifyGoodsActivity.class));
                    getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
                    break;
                default:break;
            }
        }

    }

    private void toWebActivity(String loadUrl) {
        Intent intent = new Intent(mContext, BaseWebViewActivity.class);
        intent.putExtra("loadUrl", loadUrl);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.activity_banner_right_in, R.anim.activity_banner_left_out);
    }
}
