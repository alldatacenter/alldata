package top.omooo.blackfish.KeeperPageActivity;

import android.app.Dialog;
import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.util.Calendar;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.DetailCardPageAdapter;
import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.blackfish.utils.DensityUtil;
import top.omooo.blackfish.utils.PickerUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/28.
 */

public class ShowDetailBillActivity extends BaseActivity{

    private RelativeLayout mRelativeLayout1;
    private RelativeLayout mRelativeLayout2;
    private TextView mTextTitle,mTextCardNumber,mTextBillNumber,mTextPayMin,mTextBillLines,mTextBillDay,mTextPayBillDay, mTextNoLiXi;
    private ImageView mImageBack, mImageRefresh;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private TabLayout.Tab billTab;
    private TabLayout.Tab payHistoryTab;

    private TextView mTextRemind,mTextPayOff, mTextSign,mTextImmPay;

    private boolean isSelectRemindWay = true;
    private ImageView mImageNewRemindClose, mImageAddRemindClose, mImageSelectRemindWay;
    private TextView mTextNewRemind,mTextRemindDate, mTextRemindTime;
    private LinearLayout mNewRemindLayout;
    private Button mBtnCancel, mBtnDetermine;

    private Dialog remindDialog,addRemindDialog;

    private int childIndex = 0;
    private AdjustViewUtil mAdjustViewUtil;

    private Context mContext;
    private static final String TAG = "ShowDetailBillActivity";
    @Override
    public int getLayoutId() {
        return R.layout.activity_show_detail_bill_layout;
    }

    @Override
    public void initViews() {

        //修改状态栏颜色
        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_VISIBLE);
        Window window = getWindow();
        //取消状态栏透明
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //设置状态栏颜色
        window.setStatusBarColor(getColor(R.color.colorCardDetailHeader));

        mContext = ShowDetailBillActivity.this;

        mRelativeLayout1 = findView(R.id.rl_keeper_detail_bill_1);
        mRelativeLayout2 = findView(R.id.rl_keeper_detail_bill_2);
        mTextTitle = findView(R.id.tv_keeper_detail_card_title);
        mTextCardNumber = findView(R.id.tv_detail_card_number_name);
        mTextBillNumber = findView(R.id.tv_detail_card_bill_number);
        mTextPayMin = findView(R.id.tv_detail_card_pay_min);
        mTextBillLines = findView(R.id.tv_detail_card_bill_lines);
        mTextBillDay = findView(R.id.tv_detail_card_bill_day);
        mTextPayBillDay = findView(R.id.tv_detail_card_pay_bill_day);
        mTextNoLiXi = findView(R.id.tv_detail_card_no_li);
        mImageBack = findView(R.id.iv_keeper_detail_card_back);
        mImageRefresh = findView(R.id.iv_detail_card_refresh);

        mTabLayout = findView(R.id.tab_layout_detail_card);
        mViewPager = findView(R.id.vp_detail_card);

        mTextRemind = findView(R.id.tv_bill_detail_remind);
        mTextPayOff = findView(R.id.tv_bill_detail_pay_off);
        mTextSign = findView(R.id.tv_bill_detail_sign);
        mTextImmPay = findView(R.id.tv_bill_detail_imm_pay);

        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustTextViewPic(mTextRemind, 1, 0, 10, 70, 70);
        mAdjustViewUtil.adjustTextViewPic(mTextPayOff, 1, 0, 10, 70, 73);
        mAdjustViewUtil.adjustTextViewPic(mTextSign, 1, 0, 10, 70, 73);

        mViewPager.setAdapter(new DetailCardPageAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
        billTab = mTabLayout.getTabAt(0);
        payHistoryTab = mTabLayout.getTabAt(1);
        layoutInAndOutAnim(false);

    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mImageRefresh.setOnClickListener(this);

        mTextRemind.setOnClickListener(this);
        mTextPayOff.setOnClickListener(this);
        mTextSign.setOnClickListener(this);
        mTextImmPay.setOnClickListener(this);

    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_keeper_detail_card_back:
                exitActivity();
                break;
            case R.id.iv_detail_card_refresh:
                CustomToast.show(mContext, "刷新");
                break;
            case R.id.tv_bill_detail_remind:
                showRemindDialog();
                break;
            case R.id.tv_bill_detail_pay_off:
                CustomToast.show(mContext,"标记未还清");
                break;
            case R.id.tv_bill_detail_sign:
                CustomToast.show(mContext,"标记还部分");
                break;
            case R.id.tv_bill_detail_imm_pay:
                CustomToast.show(mContext,"立即还款");
                break;
            //BottomDialog的点击事件
            case R.id.iv_remind_dialog_close:
                addRemindDialog.dismiss();
                break;
            case R.id.tv_new_remind:
                if (mNewRemindLayout.getChildCount() < 5) {
                    showAddRemindDialog();
                } else {
                    Toast.makeText(mContext, "最多设置五条提醒", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.iv_new_remind_close:
                remindDialog.dismiss();
                break;
            case R.id.btn_remind_cancel:
                addRemindDialog.dismiss();
                break;
            case R.id.iv_remind_select_way:
                if (isSelectRemindWay) {
                    mImageSelectRemindWay.setImageResource(R.drawable.icon_remind_way_unchecked);
                    isSelectRemindWay = false;
                } else {
                    mImageSelectRemindWay.setImageResource(R.drawable.icon_remind_way_checked);
                    isSelectRemindWay = true;
                }
                break;
            case R.id.btn_remind_determine:
                if (isSelectRemindWay) {
                    Toast.makeText(mContext, "保存成功", Toast.LENGTH_SHORT).show();
                    addRemindItem(new OnRemoveChildViewFromIndexListener() {
                        @Override
                        public void onRemoveChildView(int index) {
                            Log.i(TAG, "onRemoveChildView: " + index);
                            // TODO: 2018/3/30 点击删除总是从最后一个开始删除 
                            mNewRemindLayout.removeView(mNewRemindLayout.getChildAt(index-1));
                        }
                    });
                    addRemindDialog.dismiss();
                } else {
                    Toast.makeText(mContext, "请选择提醒方式", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tv_remind_date:
                selectRemindDate();
                break;
            case R.id.tv_remind_time:
                selectRemindTime();
                break;
            default:break;
        }
    }

    private void addRemindItem(final OnRemoveChildViewFromIndexListener listener) {
        //添加已经选择完提醒
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_remind_item_layout, null);
        TextView textView = view.findViewById(R.id.tv_remind_item_time);
        mAdjustViewUtil = new AdjustViewUtil();
        mAdjustViewUtil.adjustTextViewPic(textView, 2, 0, 0, 50, 50);
        String text = "·   " + mTextRemindDate.getText() + "    " + mTextRemindTime.getText();
        Log.i(TAG, "addRemindItem: " + text);
        textView.setText(text);
        ImageView imageView = view.findViewById(R.id.iv_remind_item_delete);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRemoveChildView(childIndex);
                childIndex--;
            }
        });
        mNewRemindLayout.addView(view, childIndex);
        childIndex++;
    }

    public interface OnRemoveChildViewFromIndexListener {
        void onRemoveChildView(int index);
    }

    private void selectRemindTime() {
//        //Android原生时间选择器，丑的鸭皮
//        Calendar calendar = Calendar.getInstance();
//        new TimePickerDialog( mContext,
//                new TimePickerDialog.OnTimeSetListener() {
//                    @Override
//                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//                        Log.i(TAG, "onTimeSet: " + hourOfDay + "  " + minute);
//                    }
//                }
//                // 设置初始时间
//                , calendar.get(Calendar.HOUR_OF_DAY)
//                , calendar.get(Calendar.MINUTE)
//                ,false).show();
        // https://github.com/wdullaer/MaterialDateTimePicker
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
                String time = hourOfDay + ":" + minute;
                mTextRemindTime.setText(time);
                mTextRemindTime.setTextColor(getResources().getColor(R.color.splash_main_title_color));
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show(getFragmentManager(),"timePickerDialog");
    }

    private void selectRemindDate() {
        PickerUtil pickerUtil = new PickerUtil();
        pickerUtil.showCustomPicker(this, R.array.pick_remind_date, new PickerUtil.OnSelectFinshListener() {
            @Override
            public String onSelected(String result) {
                mTextRemindDate.setText(result);
                mTextRemindDate.setTextColor(getResources().getColor(R.color.splash_main_title_color));
                return null;
            }
        });
    }

    @Override
    protected void initData() {

    }

    private void layoutInAndOutAnim(boolean isBack) {
        Log.i(TAG, "layoutInAndOutAnim: " + isBack);
        if (!isBack) {
            Animation animationTopIn = AnimationUtils.loadAnimation(this, R.anim.view_layout_top_in);
            animationTopIn.setFillEnabled(true);
            animationTopIn.setFillAfter(true);
            mRelativeLayout1.setAnimation(animationTopIn);
            Animation animationBottomIn = AnimationUtils.loadAnimation(this, R.anim.view_layout_bottom_in);
            animationBottomIn.setFillEnabled(true);
            animationBottomIn.setFillAfter(true);
            mRelativeLayout2.setAnimation(animationBottomIn);
        } else {
            Animation animationTopOut = AnimationUtils.loadAnimation(this, R.anim.view_layout_top_out);
            animationTopOut.setFillEnabled(true);
            animationTopOut.setFillAfter(true);
            mRelativeLayout1.setAnimation(animationTopOut);
            Animation animationBottomOut = AnimationUtils.loadAnimation(this, R.anim.view_layout_bottom_out);
            animationBottomOut.setFillEnabled(true);
            animationBottomOut.setFillAfter(true);
            mRelativeLayout2.setAnimation(animationBottomOut);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitActivity();
        }
        return false;
    }

    private void exitActivity() {
        mRelativeLayout1.clearAnimation();
        mRelativeLayout1.invalidate();
        mRelativeLayout2.clearAnimation();
        mRelativeLayout2.invalidate();
        layoutInAndOutAnim(true);
        mRelativeLayout1.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
                //去掉返回后透明Activity退出闪烁问题
                overridePendingTransition(0, 0);
            }
        }, 500);
    }

    // TODO: 2018/3/30 保存已经添加的提醒 
    private void showRemindDialog() {
        remindDialog = new Dialog(mContext, R.style.BottomDialogStyle);
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_remind_bottom_dialog_layout, null);

        mImageNewRemindClose = view.findViewById(R.id.iv_new_remind_close);
        mTextNewRemind = view.findViewById(R.id.tv_new_remind);
        mNewRemindLayout = view.findViewById(R.id.ll_new_remind_);

        mImageNewRemindClose.setOnClickListener(this);
        mTextNewRemind.setOnClickListener(this);

        remindDialog.setContentView(view);
        Window window = remindDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = DensityUtil.getScreenWidth(this);
//        lp.height = DensityUtil.dip2px(mContext, 130);
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);
        remindDialog.show();
    }

    private void showAddRemindDialog() {
        addRemindDialog = new Dialog(mContext, R.style.BottomDialogStyle);
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_new_remind_dialog, null);

        mImageAddRemindClose = view.findViewById(R.id.iv_remind_dialog_close);
        mImageSelectRemindWay = view.findViewById(R.id.iv_remind_select_way);
        mBtnCancel = view.findViewById(R.id.btn_remind_cancel);
        mBtnDetermine = view.findViewById(R.id.btn_remind_determine);
        mTextRemindDate = view.findViewById(R.id.tv_remind_date);
        mTextRemindTime = view.findViewById(R.id.tv_remind_time);

        mImageAddRemindClose.setOnClickListener(this);
        mImageSelectRemindWay.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
        mBtnDetermine.setOnClickListener(this);
        mTextRemindDate.setOnClickListener(this);
        mTextRemindTime.setOnClickListener(this);

        addRemindDialog.setContentView(view);
        Window window = addRemindDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = DensityUtil.getScreenWidth(this);
        layoutParams.height = DensityUtil.dip2px(mContext, 350);
        layoutParams.gravity = Gravity.BOTTOM;
        window.setAttributes(layoutParams);
        addRemindDialog.show();
    }

}
