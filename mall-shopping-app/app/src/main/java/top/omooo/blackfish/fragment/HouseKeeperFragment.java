package top.omooo.blackfish.fragment;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

import top.omooo.blackfish.KeeperPageActivity.AddBillActivity;
import top.omooo.blackfish.KeeperPageActivity.AddCreditBillActivity;
import top.omooo.blackfish.KeeperPageActivity.ShowDetailBillActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.blackfish.utils.DensityUtil;
import top.omooo.blackfish.utils.SpannableStringUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by Omooo on 2018/2/25.
 */

public class HouseKeeperFragment extends BaseFragment {

    private ImageView mImageAdd, mImageShowEyes;
    private TextView mTextMoney,mTextGrid1, mTextGrid2, mTextGrid3, mTextGrid4,mTextGift;
    private RelativeLayout mCardLayout;
    private Button mButtonAddBill;
    private Context mContext;

    private AdjustViewUtil mAdjustViewUtil;
    private boolean isShowMoney = false;
    private float money = 233.33f;

    public static HouseKeeperFragment newInstance() {
        return new HouseKeeperFragment();
    }
    @Override
    public int getLayoutId() {
        return R.layout.fragment_housekeeper_layout;
    }

    @Override
    public void initViews() {
        mContext = getActivity();
        mTextMoney = findView(R.id.tv_keeper_money);
        mImageAdd = findView(R.id.iv_keeper_add);
        mImageShowEyes = findView(R.id.tv_keeper_show_money);
        mTextGrid1 = findView(R.id.tv_keeper_grid_1);
        mTextGrid2 = findView(R.id.tv_keeper_grid_2);
        mTextGrid3 = findView(R.id.tv_keeper_grid_3);
        mTextGrid4 = findView(R.id.tv_keeper_grid_4);
        mTextGift = findView(R.id.tv_keeper_gift);
        mCardLayout = findView(R.id.rl_keeper_card);
        mButtonAddBill = findView(R.id.btn_keeper_add_bill);

        //调整TextView的DrawableTop的大小
        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustTextViewPic(mTextGrid1, 1, 0, 0, 180, 180);
        mAdjustViewUtil.adjustTextViewPic(mTextGrid2, 1, 0, 0, 180, 180);
        mAdjustViewUtil.adjustTextViewPic(mTextGrid3, 1, 0, 0, 180, 180);
        mAdjustViewUtil.adjustTextViewPic(mTextGrid4, 1, 0, 0, 180, 180);

        mAdjustViewUtil.adjustTextViewPic(mTextGift, 0, 0, 0, 50, 50);

        mCardLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Animation animation = setLongClickAnimation(mCardLayout);
                animation.start();
                return false;
            }
        });

        // TODO: 2018/3/31 空指针，获取不到数据 
        Bundle bundle = getArguments();
        if (null != bundle) {
            String cardType = bundle.getString("CardType");
            CustomToast.show(mContext, cardType);
        }
    }


    @Override
    public void initListener() {
        mImageAdd.setOnClickListener(this);
        mImageShowEyes.setOnClickListener(this);
        mTextGrid1.setOnClickListener(this);
        mTextGrid2.setOnClickListener(this);
        mTextGrid3.setOnClickListener(this);
        mTextGrid4.setOnClickListener(this);
        mCardLayout.setOnClickListener(this);
        mButtonAddBill.setOnClickListener(this);
    }

    @Override
    public void initData() {

    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_keeper_add:
                startActivity(new Intent(getActivity(),AddBillActivity.class));
                break;
            case R.id.tv_keeper_show_money:
                if (!isShowMoney) {
                    final DecimalFormat decimalFormat = new DecimalFormat(".00");
                    ValueAnimator animator = ValueAnimator.ofFloat(0, money);
                    animator.setDuration(1000);
                    animator.start();
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mTextMoney.setText(decimalFormat.format(animation.getAnimatedValue()));
                            SpannableStringUtil.setRelativeSizeText(mTextMoney, 0, mTextMoney.getText().length() - 3, 1.3f, mTextMoney.getText().toString());
                        }
                    });
                    isShowMoney = true;
                    mImageShowEyes.setImageResource(R.drawable.icon_open_eyes);
                } else {
                    mTextMoney.setText("*****");
                    isShowMoney = false;
                    mImageShowEyes.setImageResource(R.drawable.icon_close_eyes);
                }
                break;
            case R.id.tv_keeper_grid_1:
                showAddBillDialog(R.drawable.icon_dialog_add_bill_1, "您还没有账单，快去添加吧！", 0, new DialogListener());
                break;
            case R.id.tv_keeper_grid_2:
                showAddBillDialog(R.drawable.icon_dialog_add_bill_2, "免息期告诉你今天刷哪张卡最划算", 1, new DialogListener());
                break;
            case R.id.tv_keeper_grid_3:
                CustomToast.show(mContext,"办信用卡");
                break;
            case R.id.tv_keeper_grid_4:
                CustomToast.show(mContext,"我要贷款");
                break;
            case R.id.rl_keeper_card:
                setClickAnimation(mCardLayout);
                break;
            case R.id.btn_keeper_add_bill:
                startActivity(new Intent(mContext, AddBillActivity.class));
                break;
            default:break;
        }
    }

    private void showAddBillDialog(int drawableId, String text, final int dialogIndex, final OnDialogPosBtnClickListener listener) {
        final Dialog dialog = new Dialog(mContext, R.style.CustomDialog);
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_keeper_custom_dialog_layout, null);
        ImageView imageExit = view.findViewById(R.id.iv_dialog_exit);
        ImageView imageIcon = view.findViewById(R.id.iv_dialog_icon);
        imageIcon.setImageResource(drawableId);
        TextView textTitle = view.findViewById(R.id.tv_dialog_title);
        textTitle.setText(text);
        TextView textCancel = view.findViewById(R.id.tv_dialog_cancel);
        TextView textAddBill = view.findViewById(R.id.tv_dialog_add_bill);
        textCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        textAddBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onBtnClick(dialogIndex);
                dialog.dismiss();
            }
        });
        imageExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);

        //曾经跪在这，Mark下
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.height = DensityUtil.dip2px(mContext, 250);
        layoutParams.width = DensityUtil.dip2px(mContext, 260);
        layoutParams.gravity = Gravity.CENTER;
        window.setAttributes(layoutParams);
        dialog.show();
    }

    private interface OnDialogPosBtnClickListener {
        void onBtnClick(int index);
    }

    private class DialogListener implements OnDialogPosBtnClickListener {
        @Override
        public void onBtnClick(int index) {
            if (index == 0) {
                startActivity(new Intent(mContext, AddBillActivity.class));
            } else if (index == 1) {
                startActivity(new Intent(mContext, AddCreditBillActivity.class));
            } else {
                return;
            }
        }
    }

    //CardView的缩放效果
    private void setClickAnimation(View view) {
        Animation animation = new ScaleAnimation(0.95f, 1, 0.95f, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(300);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(getActivity(),ShowDetailBillActivity.class));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animation);
    }

    // TODO: 2018/3/25 CardView长按缩放效果
    private Animation setLongClickAnimation(View view) {
        Animation animation = new ScaleAnimation(0.95f, 1, 0.95f, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(300);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startActivity(new Intent(getActivity(),AddBillActivity.class));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        return animation;
    }
}
