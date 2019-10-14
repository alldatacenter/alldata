package top.omooo.blackfish.fragment;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import top.omooo.blackfish.LoginActivity;
import top.omooo.blackfish.MinePageActivity.AboutActivity;
import top.omooo.blackfish.MinePageActivity.ActivityMyBankCard;
import top.omooo.blackfish.MinePageActivity.MineSettingActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.event.LoginSuccessEvent;
import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.router.EasyRouter;

/**
 * Created by SSC on 2018/3/20.
 */

public class NewMineFragment extends BaseFragment {

    private Context mContext;

    private ImageView mImageSetting;
    private SimpleDraweeView mSimpleDraweeView; //头像
    private TextView mTextPhone, mTextPerCenter;

    private TextView mTextBankCard, mTextCoupons, mTextFav, mTextHistory, mTextHelpCenter, mTextAbout;

    private TextView mTextPay, mTextSend, mTextGet, mTextAfterSale;

    private AdjustViewUtil mAdjustViewUtil;

    public static NewMineFragment newInstance() {
        return new NewMineFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_mine_new_layout;
    }

    @Override
    public void initViews() {
        mContext = getActivity();

        mImageSetting = findView(R.id.iv_mine_header_setting);
        mSimpleDraweeView = findView(R.id.iv_mine_portrait);
        mTextPerCenter = findView(R.id.tv_mine_personal_center);
        mTextPhone = findView(R.id.tv_mine_header_phone);

        mTextBankCard = findView(R.id.tv_mine_bank_card);
        mTextCoupons = findView(R.id.tv_mine_coupons);
        mTextFav = findView(R.id.tv_mine_fav);
        mTextHistory = findView(R.id.tv_mine_history);
        mTextHelpCenter = findView(R.id.tv_mine_help_center);
        mTextAbout = findView(R.id.tv_mine_about_bf);

        mTextPay = findView(R.id.tv_mine_grid_pay);
        mTextSend = findView(R.id.tv_mine_grid_send_goods);
        mTextGet = findView(R.id.tv_mine_grid_get_goods);
        mTextAfterSale = findView(R.id.tv_mine_grid_after_sale);

        //调整drawableLeft图片的大小
        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustTextViewPic(mTextBankCard, 0, 10, 5, 70, 60);
        mAdjustViewUtil.adjustTextViewPic(mTextCoupons, 0, 10, 5, 70, 60);
        mAdjustViewUtil.adjustTextViewPic(mTextFav, 0, 10, 5, 70, 60);
        mAdjustViewUtil.adjustTextViewPic(mTextHistory, 0, 10, 5, 70, 60);
        mAdjustViewUtil.adjustTextViewPic(mTextHelpCenter, 0, 10, 5, 75, 70);
        mAdjustViewUtil.adjustTextViewPic(mTextAbout, 0, 10, 5, 70, 60);

        //调整drawableTop图片的大小
        mAdjustViewUtil.adjustTextViewPic(mTextPay, 1, 0, 15, 90, 90);
        mAdjustViewUtil.adjustTextViewPic(mTextSend, 1, 0, 15, 90, 90);
        mAdjustViewUtil.adjustTextViewPic(mTextGet, 1, 0, 15, 90, 90);
        mAdjustViewUtil.adjustTextViewPic(mTextAfterSale, 1, 0, 15, 90, 90);

        mSimpleDraweeView.setImageURI(getUriFromDrawableRes(mContext, R.drawable.image_mine_pager_user));

    }


    @Override
    public void initListener() {
        mImageSetting.setOnClickListener(this);
        mSimpleDraweeView.setOnClickListener(this);
        mTextPerCenter.setOnClickListener(this);

        mTextBankCard.setOnClickListener(this);
        mTextCoupons.setOnClickListener(this);
        mTextFav.setOnClickListener(this);
        mTextHistory.setOnClickListener(this);
        mTextHelpCenter.setOnClickListener(this);
        mTextAbout.setOnClickListener(this);

        mTextPay.setOnClickListener(this);
        mTextSend.setOnClickListener(this);
        mTextGet.setOnClickListener(this);
        mTextAfterSale.setOnClickListener(this);
    }

    @Override
    public void initData() {

    }

    @Override
    public void processClick(View view) {

        switch (view.getId()) {
            case R.id.iv_mine_header_setting:
                //设置
                startActivity(new Intent(getActivity(), MineSettingActivity.class));
                break;
            case R.id.iv_mine_portrait:
                //个人头像
                break;
            case R.id.tv_mine_personal_center:
                //个人中心
                startActivity(new Intent(getActivity(), LoginActivity.class));
                break;
            case R.id.tv_mine_grid_pay:
                //待付款
                break;
            case R.id.tv_mine_grid_send_goods:
                //待发货
                break;
            case R.id.tv_mine_grid_get_goods:
                //待收货
                break;
            case R.id.tv_mine_grid_after_sale:
                //退换/售后
                break;
            case R.id.tv_mine_bank_card:
                //我的银行卡
//                startActivity(new Intent(getActivity(), ActivityMyBankCard.class));
                EasyRouter.getInstance().with(mContext).navigate("myBankCard");
                break;
            case R.id.tv_mine_coupons:
                //我的优惠券
                break;
            case R.id.tv_mine_fav:
                //我的收藏
                break;
            case R.id.tv_mine_history:
                //浏览历史
                break;
            case R.id.tv_mine_help_center:
                //帮助中心
                break;
            case R.id.tv_mine_about_bf:
                //关于小黑鱼
                startActivity(new Intent(getActivity(), AboutActivity.class));
                break;
            default:
                break;
        }
    }

    public Uri getUriFromDrawableRes(Context context, int id) {
        Resources resources = context.getResources();
        String path = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + resources.getResourcePackageName(id) + "/"
                + resources.getResourceTypeName(id) + "/"
                + resources.getResourceEntryName(id);
        return Uri.parse(path);
    }
}
