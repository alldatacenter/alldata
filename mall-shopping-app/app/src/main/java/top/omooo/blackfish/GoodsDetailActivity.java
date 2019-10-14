package top.omooo.blackfish;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import top.omooo.blackfish.MallPagerActivity.ShowImageActivity;
import top.omooo.blackfish.MallPagerActivity.SubmitOrderActivity;
import top.omooo.blackfish.adapter.SimilarRecoAdapter;
import top.omooo.blackfish.bean.GoodsDetailsInfo;
import top.omooo.blackfish.bean.OptionalTypeInfo;
import top.omooo.blackfish.bean.SimilarRecoInfo;
import top.omooo.blackfish.bean.UrlInfoBean;
import top.omooo.blackfish.listener.OnNetResultListener;
import top.omooo.blackfish.utils.AnalysisJsonUtil;
import top.omooo.blackfish.utils.OkHttpUtil;
import top.omooo.blackfish.utils.SpannableStringUtil;
import top.omooo.blackfish.view.AmountView;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.GridViewForScroll;
import top.omooo.blackfish.view.RecycleViewBanner;
import top.omooo.blackfish.view.TagsLayout;

/**
 * Created by SSC on 2018/4/12.
 */

public class GoodsDetailActivity extends NewBaseActivity {


    @BindView(R.id.iv_back)
    ImageView mIvBack;
    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;
    @BindView(R.id.iv_more)
    ImageView mIvMore;
    @BindView(R.id.rv_banner)
    RecycleViewBanner mRvBanner;
    @BindView(R.id.tv_price)
    TextView mTvPrice;
    @BindView(R.id.tv_single_price)
    TextView mTvSinglePrice;
    @BindView(R.id.tv_desc)
    TextView mTvDesc;
    @BindView(R.id.tv_choose_type)
    TextView mTvChooseType;
    @BindView(R.id.iv_more_type)
    ImageView mIvMoreType;
    @BindView(R.id.tv_choose_address)
    TextView mTvChooseAddress;
    @BindView(R.id.iv_more_address)
    ImageView mIvMoreAddress;
    @BindView(R.id.gv_similar_reco)
    GridViewForScroll mGvSimilarReco;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.rl_dialog)
    RelativeLayout mRlDialog;
    @BindView(R.id.ll_period_info)
    LinearLayout mLinearPeriodInfo;
    @BindView(R.id.tv_fav)
    TextView mTextFav;
    @BindView(R.id.tv_imm_pay)
    TextView mTextImmPay;

    private Context mContext;

    private boolean isFav = false;
    private BottomSheetDialog mDialog;
    private static final String TAG = "GoodsDetailActivity";

    private List<GoodsDetailsInfo> mGoodsDetailsInfos;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0x01) {
                GoodsDetailsInfo detailsInfo = mGoodsDetailsInfos.get(0);

                final List<String> bannerList = detailsInfo.getBannerList();
                mRvBanner.setRvBannerData(bannerList);
                mRvBanner.setOnSwitchRvBannerListener(new RecycleViewBanner.OnSwitchRvBannerListener() {
                    @Override
                    public void switchBanner(int position, SimpleDraweeView simpleDraweeView) {
                        simpleDraweeView.setImageURI(bannerList.get(position));
                    }
                });
                mRvBanner.setOnBannerClickListener(new RecycleViewBanner.OnRvBannerClickListener() {
                    @Override
                    public void onClick(int position) {
                        skipActivity(new Intent(mContext, ShowImageActivity.class));
                    }
                });

                String totalPrice = "¥" + detailsInfo.getTotalPrice();
                SpannableString spannableString = new SpannableStringUtil().setMallGoodsPrice(totalPrice, 0, totalPrice.length());
                mTvPrice.setText(spannableString);
                String singlePrice = "月供 " + detailsInfo.getSinglePrice() + " 元起";
                mTvSinglePrice.setText(singlePrice);
                mTvDesc.setText(detailsInfo.getDesc());
                mTvChooseType.setText(detailsInfo.getDefaultType());
                mTvChooseAddress.setText("上海市 浦东新区");

                setDialogData(detailsInfo.getOptionalTypeInfos());

                // TODO: 2018/4/13 又一个GridView不显示的问题，烦得很
                List<SimilarRecoInfo> similarRecoInfos = detailsInfo.getSimilarRecoInfos();
                Toast.makeText(mContext, "" + similarRecoInfos.size(), Toast.LENGTH_SHORT).show();
                mGvSimilarReco.setAdapter(new SimilarRecoAdapter(mContext, similarRecoInfos));
            }
            return false;
        }
    });

    @Override
    public int getLayoutId() {
        return R.layout.activity_goods_detail_layout;
    }

    @Override
    public void initViews() {
        getWindow().setStatusBarColor(Color.parseColor("#F7F7F7"));
        mContext = GoodsDetailActivity.this;

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Toast.makeText(mContext, "TabLayout" + tab.getPosition(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void initData() {
        mGoodsDetailsInfos = new ArrayList<>();
        final AnalysisJsonUtil jsonUtil = new AnalysisJsonUtil();
        OkHttpUtil.getInstance().startGet(UrlInfoBean.goodsDetailsUrl, new OnNetResultListener() {
            @Override
            public void onSuccessListener(String result) {
                mGoodsDetailsInfos = jsonUtil.getDataFromJson(result, 4);
                Message message = mHandler.obtainMessage(0x01, mGoodsDetailsInfos);
                mHandler.sendMessage(message);
            }

            @Override
            public void onFailureListener(String result) {

            }
        });
    }


    @OnClick({R.id.iv_back, R.id.iv_more, R.id.iv_more_type, R.id.iv_more_address, R.id.rl_dialog, R.id.ll_period_info, R.id.tv_fav, R.id.tv_imm_pay})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finshActivity();
                break;
            case R.id.iv_more:
                CustomToast.show(mContext, "显示 PopupWindow");
                break;
            case R.id.iv_more_type:
                mDialog.show();
                break;
            case R.id.iv_more_address:
                CustomToast.show(mContext, "选择配送地址");
                break;
            case R.id.rl_dialog:
                showServiceDialog();
                break;
            case R.id.ll_period_info:
                CustomToast.show(mContext, "月供详情");
                break;
            case R.id.tv_fav:
                Drawable[] drawables = mTextFav.getCompoundDrawables();
                if (!isFav) {
                    mTextFav.setText("已收藏");
                    Drawable drawable = getDrawable(R.drawable.icon_fav_checked);
                    drawable.setBounds(0, 0, 50, 50);
                    mTextFav.setCompoundDrawables(drawables[0], drawable, drawables[2], drawables[3]);
                    isFav = true;
                } else {
                    mTextFav.setText("收藏");
                    Drawable drawable = getDrawable(R.drawable.icon_fav_uncheck);
                    drawable.setBounds(0, 0, 50, 50);
                    mTextFav.setCompoundDrawables(drawables[0], drawable, drawables[2], drawables[3]);
                    isFav = false;
                }
                break;
            case R.id.tv_imm_pay:
                mDialog.show();
                break;
            default:
                break;
        }
    }

    private void showServiceDialog() {
        final Dialog dialog = new BottomSheetDialog(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_service_info_layout, null);
        view.findViewById(R.id.iv_close_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void setDialogData(List<OptionalTypeInfo> typeInfos) {
        mDialog = new BottomSheetDialog(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_choose_type_layout, null);
        view.findViewById(R.id.iv_close_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        SimpleDraweeView draweeView = view.findViewById(R.id.iv_goods);
        draweeView.setImageURI(UrlInfoBean.dialogImage);
        draweeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipActivity(new Intent(mContext, ShowImageActivity.class));
            }
        });

        TextView textPrice = view.findViewById(R.id.tv_top_price);
        TextView textType = view.findViewById(R.id.tv_type);
        final TextView textAmount = view.findViewById(R.id.tv_num);
        final TextView textSingle = view.findViewById(R.id.tv_price_bottom);
        TextView textPay = view.findViewById(R.id.tv_diter_pay);
        textPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
                skipActivity(new Intent(mContext, SubmitOrderActivity.class));
            }
        });

        TagsLayout tagsLayout = view.findViewById(R.id.labels_view);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < typeInfos.size(); i++) {
            TextView textView = new TextView(mContext);
            textView.setPadding(10, 10, 10, 10);
            textView.setBackground(getDrawable(R.drawable.shape_search));
            String string = "   " + typeInfos.get(i).getType() + "   ";
            textView.setText(string);

            //默认选择第二个
            if (i == 1) {
                textView.setBackground(getDrawable(R.drawable.shape_selected_text));
                textView.setTextColor(getColor(R.color.colorWhite));
            }

            tagsLayout.addView(textView, lp);
        }


        //默认选择最后一个
        final OptionalTypeInfo typeInfo = typeInfos.get(2);
        String totalPrice = "¥" + typeInfo.getTotalPrice();
        textPrice.setText(totalPrice);
        final double singlePrice = typeInfo.getSinglePrice();
        final BigDecimal decimal = new BigDecimal(singlePrice);
        setSpann(textSingle, decimal.doubleValue());
        AmountView amountView = view.findViewById(R.id.amount_view);
        amountView.setMaxNumber(20);
        amountView.setOnNumChangeListener(new AmountView.OnNumChangeListener() {
            @Override
            public void onChange(int num) {
                BigDecimal multiply = new BigDecimal(num);
                String string = "数量：" + num;
                textAmount.setText(string);
                setSpann(textSingle, decimal.multiply(multiply).doubleValue());
            }
        });
        mDialog.setContentView(view);
    }

    private void setSpann(TextView textView, double price) {
        String singlePrice = "月供 ¥" + price + " 起";
        SpannableString spannableString = new SpannableStringUtil().setMallGoodsPrice(singlePrice, 3, 4 + String.valueOf(price).length());
        textView.setText(spannableString);
    }


}
