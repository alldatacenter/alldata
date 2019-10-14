package top.omooo.blackfish.KeeperPageActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.HashMap;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.MainActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.listener.OnSuperEditClickListener;
import top.omooo.blackfish.listener.OnSuperEditLayoutClickListener;
import top.omooo.blackfish.utils.PickerUtil;
import top.omooo.blackfish.utils.SelectCardActivity;
import top.omooo.blackfish.view.CustomToast;
import top.omooo.blackfish.view.SuperEditText;

/**
 * Created by SSC on 2018/3/26.
 */

public class CreateCreditBillActivity extends BaseActivity {

    private Context mContext;
    private ImageView mImageBack;
    private SuperEditText mSuperCardNumber, mSuperBank, mSuperCardType,mSuperUsername,mSuperLines,mSuperBill,mSuperBillDay, mSuperPayBillDay;
    private Button mButtonSave;

    private static final String TAG = "CreateCreditActivity";

    private String bankName = "";
    private String cardType = "信用卡";
    private String billDay = "15 日";
    private String payBillDay = "5 日";

    private PickerUtil mPickerUtil;

    @Override
    public int getLayoutId() {
        return R.layout.activity_create_credit_bill_layout;
    }

    @Override
    public void initViews() {
        mContext = CreateCreditBillActivity.this;
        mImageBack = findView(R.id.tv_keeper_create_credit_bill_back);
        mSuperCardNumber = findView(R.id.super_edit_card_number);
        mSuperBank = findView(R.id.super_edit_belong_to_bank);
        mSuperCardType = findView(R.id.super_edit_card_type);
        mSuperUsername = findView(R.id.super_edit_username);
        mSuperLines = findView(R.id.super_edit_lines);
        mSuperBill = findView(R.id.super_edit_bill);
        mSuperBillDay = findView(R.id.super_edit_bill_day);
        mSuperPayBillDay = findView(R.id.super_edit_pay_bill_day);

        mButtonSave = findView(R.id.btn_create_save);

        mPickerUtil = new PickerUtil();
    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mButtonSave.setOnClickListener(this);

        // TODO: 2018/3/27 脏！
        /**
         * 为了解决点击EditText导致整体Layout点击事件不响应
         * 所以写了两个监听器，一个监听Layout，一个监听EditText
         * 还好一个点击事件不会被两个监听器同时响应
         * 而且，对于Layout的响应，其实没什么可做
         * 不过，代码着实脏！
         */
        mSuperCardNumber.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperBank.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperCardType.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperUsername.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperLines.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperBill.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperBillDay.setOnSuperEditClickListener(new SuperListenerLayout());
        mSuperPayBillDay.setOnSuperEditClickListener(new SuperListenerLayout());

        mSuperBank.setOnSuperClickListener(new OnSuperClick());
        mSuperCardType.setOnSuperClickListener(new OnSuperClick());
        mSuperBillDay.setOnSuperClickListener(new OnSuperClick());
        mSuperPayBillDay.setOnSuperClickListener(new OnSuperClick());
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_keeper_create_credit_bill_back:
                finish();
                break;
            case R.id.btn_create_save:
                getEditText();
                break;
            default:break;
        }
    }

    //获取数据
    private void getEditText() {

        HashMap<String, String> mCardNumberMap = mSuperCardNumber.getStringHashMap();
        String cardNumber = mCardNumberMap.get("卡号");
        Log.i(TAG, "processClick: cardNumber   " + cardNumber);

        HashMap<String, String> mUsernameMap = mSuperUsername.getStringHashMap();
        String username = mUsernameMap.get("用户名");
        Log.i(TAG, "processClick: username   " + username);

        HashMap<String, String> mLinesMap = mSuperLines.getStringHashMap();
        String lines = mLinesMap.get("信用额度");
        Log.i(TAG, "processClick: lines   " + lines);

        HashMap<String, String> mBillMap = mSuperBill.getStringHashMap();
        String bills = mBillMap.get("账单金额");
        Log.i(TAG, "processClick: bills   " + bills);

        if (null == cardNumber || cardNumber.length() < 19) {
            showToast("请正确填写卡号");
        } else if (null == username || username.equals("")) {
            showToast("请正确填写用户名");
        } else if (null == lines || lines.equals("")) {
            showToast("请正确填写信用额度");
        } else if (null == bills || bills.equals("")) {
            showToast("请正确填写账单金额");
        } else if (bankName.equals("")) {
            showToast("请选择所属银行");
        } else {
            Intent intent = new Intent(mContext, MainActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("CardNumber", cardNumber);
            bundle.putString("BankName", bankName);
            bundle.putString("CardType", cardType);
            bundle.putString("Username", username);
            bundle.putString("Lines", lines);
            bundle.putString("Bills", bills);
            bundle.putString("BillDay", billDay);
            bundle.putString("PayBillDay", payBillDay);
            intent.putExtras(bundle);
            intent.putExtra("flag", "CreateCreditBillActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
        }
    }

    private void showToast(String msg) {
        CustomToast.show(mContext, msg);
    }
    @Override
    protected void initData() {

    }

    class SuperListenerLayout implements OnSuperEditLayoutClickListener {

        @Override
        public void onSuperEditClick(String id) {
            Log.i(TAG, "onSuperEditClick: " + id);
            switch (id) {
                case "卡号":
//                    CustomToast.show(mContext, "卡号 被点击" + mSuperCardNumber.getData());
                    break;
                case "所属银行":
                    selectBank();
                    break;
                case "卡片类型":
                    selectCardType();
                    break;
                case "用户名":
//                    CustomToast.show(mContext, "用户名 被点击" + mSuperCardNumber.getData());
                    break;
                case "信用额度":
//                    CustomToast.show(mContext, "信用额度 被点击" + mSuperCardNumber.getData());
                    break;
                case "账单金额":
//                    CustomToast.show(mContext, "账单金额 被点击" + mSuperCardNumber.getData());
                    break;
                case "账单日":
                    selectBillDay();
                    break;
                case "还款日":
                    selectPayBillDay();
                    break;
                default:break;
            }
        }
    }

    class OnSuperClick implements OnSuperEditClickListener {

        @Override
        public void onSuperClick(String id) {
            switch (id) {
                case "所属银行":
                    selectBank();
                    break;
                case "卡片类型":
                    selectCardType();
                    break;
                case "账单日":
                    selectBillDay();
                    break;
                case "还款日":
                    selectPayBillDay();
                    break;
                default:break;
            }
        }
    }

    //选择所属银行
    private void selectBank() {
        startActivityForResult(new Intent(mContext, SelectCardActivity.class), 0x001);
    }

    //选择卡片类型
    private void selectCardType() {
        mPickerUtil.showCustomPicker(this, R.array.CardType, new PickerUtil.OnSelectFinshListener() {
            @Override
            public String onSelected(String result) {
                cardType = result;
                mSuperCardType.setEditText("        " + cardType);
                return null;
            }

        });
    }

    //选择账单日
    private void selectBillDay() {
        mPickerUtil.showCustomPicker(this, R.array.BillDay, new PickerUtil.OnSelectFinshListener() {
            @Override
            public String onSelected(String result) {
                billDay = result;
                mSuperBillDay.setEditText("        " + billDay);
                return null;
            }
        });
    }

    //选择还款日
    private void selectPayBillDay() {
        mPickerUtil.showCustomPicker(this, R.array.BillDay, new PickerUtil.OnSelectFinshListener() {
            @Override
            public String onSelected(String result) {
                payBillDay = result;
                mSuperPayBillDay.setEditText("        " + payBillDay);
                return null;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x001 && resultCode == RESULT_OK) {
            bankName = data.getStringExtra("cardName");
            Log.i(TAG, "onActivityResult: " + bankName);
            mSuperBank.setEditText("        " + bankName);
        }
    }


}
