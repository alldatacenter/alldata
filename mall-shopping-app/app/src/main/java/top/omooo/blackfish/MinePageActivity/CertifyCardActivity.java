package top.omooo.blackfish.MinePageActivity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.DensityUtil;
import top.omooo.blackfish.utils.SpannableStringUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/22.
 */

public class CertifyCardActivity extends BaseActivity {


    private ImageView mImageBack, mImageCamera,mImageCheck;
    private TextView mTextSupportBank,mTextAgreement;
    private EditText mEditName,mEditIdNumber, mEditBankCard,mEditPhoneNumber, mEditVerifyCode;
    private Button mButtonRequestCode,mButtonSubmit;

    private Context mContext;
    private boolean isCheck = false;

    @Override
    public int getLayoutId() {
        return R.layout.activity_certify_card_layout;
    }

    @Override
    public void initViews() {

        mContext = CertifyCardActivity.this;

        mImageBack = findView(R.id.iv_certify_card_back);
        mImageCamera = findView(R.id.iv_camera);
        mImageCheck = findView(R.id.iv_certify_check);
        mTextSupportBank = findView(R.id.tv_certify_support_bank);
        mTextAgreement = findView(R.id.tv_certify_agreement);
        mEditName = findView(R.id.et_certify_name);
        mEditIdNumber = findView(R.id.et_certify_id_number);
        mEditBankCard = findView(R.id.et_certify_saving_card_number);
        mEditPhoneNumber = findView(R.id.et_certify_reserved_phone);
        mEditVerifyCode = findView(R.id.et_certify_code);
        mButtonRequestCode = findView(R.id.btn_certify_request_code);
        mButtonSubmit = findView(R.id.btn_certify_submit);

        //初始化文本
        String agreement = "我已阅读并同意《代扣授权书》、《个人征信授权书》";
        SpannableStringUtil.setForegroundText(mTextAgreement, 7, agreement.length(), getColor(R.color.colorSugType), agreement);

    }

    @Override
    public void initListener() {
        mImageBack.setOnClickListener(this);
        mTextSupportBank.setOnClickListener(this);
        mButtonRequestCode.setOnClickListener(this);
        mImageCamera.setOnClickListener(this);
        mImageCheck.setOnClickListener(this);
        mTextAgreement.setOnClickListener(this);
        mButtonSubmit.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.iv_certify_card_back:
                showDialog();
                break;
            case R.id.tv_certify_support_bank:
                CustomToast.show(this,"待开发");
                break;
            case R.id.btn_certify_request_code:
                CustomToast.show(this,"写过的不写，懒～");
                break;
            case R.id.iv_camera:
                CustomToast.show(this, "待开发");
                break;
            case R.id.iv_certify_check:
                if (!isCheck) {
                    mImageCheck.setImageResource(R.drawable.icon_btn_checked);
                    isCheck = true;
                } else {
                    mImageCheck.setImageResource(R.drawable.icon_btn_unchecked);
                    isCheck = false;
                }
                break;
            case R.id.tv_certify_agreement:
                showBottomDialog();
                break;
            case R.id.btn_certify_submit:
                CustomToast.show(this, "提交");
                break;
            case R.id.tv_bottom_text_1:
                CustomToast.show(this, "Item 1 被点击");
                break;
            case R.id.tv_bottom_text_2:
                CustomToast.show(this, "Item 2 被点击");
                break;

            default:break;
        }
    }

    @Override
    protected void initData() {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showDialog();
        }
        return false;
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("您还未完成认证，是否确认返回？")
                .setPositiveButton("继续认证", mOnClickListener)
                .setNegativeButton("去意已决", mOnClickListener).create();
        builder.setCancelable(false);
        builder.show();
    }

    DialogInterface.OnClickListener mOnClickListener=new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    finish();
                    break;
                default:break;
            }
        }
    };

    private void showBottomDialog() {
        final Dialog dialog = new Dialog(mContext, R.style.BottomDialogStyle);
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_bottom_dialog_layout, null);
        TextView textView1 = view.findViewById(R.id.tv_bottom_text_1);
        TextView textView2 = view.findViewById(R.id.tv_bottom_text_2);
        TextView textView3 = view.findViewById(R.id.tv_bottom_text_3);
        textView1.setOnClickListener(this);
        textView2.setOnClickListener(this);
        textView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        //设置Dialog从窗体底部弹出
        window.setGravity(Gravity.BOTTOM);
        //获得窗体的属性，并设置宽高
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = (int) (DensityUtil.getScreenWidth(this) * 0.95);
        lp.y = 20;
        window.setAttributes(lp);
        dialog.show();
    }

}
