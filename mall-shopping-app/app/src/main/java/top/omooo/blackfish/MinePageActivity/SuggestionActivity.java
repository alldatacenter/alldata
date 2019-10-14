package top.omooo.blackfish.MinePageActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.RequestPermissionUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/21.
 */

public class SuggestionActivity extends BaseActivity {

    private TextView mTextSugFace, mTextOther;
    private EditText mEditSug, mEditPhone;
    private SimpleDraweeView mAddImage;
    private Button mButtonSubmit;
    private ImageView mImageBack;

    private Context mContext;
    private static final int REQUEST_CODE = 1;

    @Override
    public int getLayoutId() {
        return R.layout.activity_suggestion_layout;
    }

    @Override
    public void initViews() {

        if (Build.VERSION.SDK_INT >= 23) {
//            requestPermission();
            RequestPermissionUtil.reqPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE,"申请必要权限用于读取内存卡",REQUEST_CODE);
        }
        mContext = SuggestionActivity.this;
        getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        mTextSugFace = findView(R.id.tv_sug_interface);
        mTextOther = findView(R.id.tv_sug_other);

        mEditSug = findView(R.id.et_sug);
        mEditPhone = findView(R.id.et_sug_phone_text);
        mAddImage = findView(R.id.iv_sug_add_image);
        mButtonSubmit = findView(R.id.btn_sug_submit);

        mImageBack = findView(R.id.tv_mine_sug_back);

        //默认选择反馈类型为：体验与界面
        switchType(mTextSugFace,true);
        mAddImage.setImageURI(getUriFromDrawableRes(mContext,R.drawable.icon_sug_add_image));
    }

    @Override
    public void initListener() {
        mTextSugFace.setOnClickListener(this);
        mTextOther.setOnClickListener(this);

        mAddImage.setOnClickListener(this);
        mButtonSubmit.setOnClickListener(this);
        mImageBack.setOnClickListener(this);

    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_sug_interface:
                switchType(mTextSugFace, true);
                switchType(mTextOther,false);
                break;
            case R.id.tv_sug_other:
                switchType(mTextSugFace, false);
                switchType(mTextOther,true);
                break;
            case R.id.iv_sug_add_image:
                selectImage(0x01);
                break;
            case R.id.btn_sug_submit:
                if (mEditSug.getText().length() > 3) {
                    CustomToast.show(mContext, "已提交，谢谢反馈。");
                    finish();
                    overridePendingTransition(0,R.anim.activity_login_top_out);
                } else {
                    CustomToast.show(mContext,"请填写反馈内容，不少于三个字");
                }
                break;
            case R.id.tv_mine_sug_back:
                finish();
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }

    private void switchType(TextView textView, boolean isSelected) {
        if (isSelected) {
            textView.setBackground(getDrawable(R.drawable.shape_sug_text_view_selected));
            textView.setTextColor(getColor(R.color.splash_main_title_color));
        } else {
            textView.setBackground(getDrawable(R.drawable.shape_sug_text_view_unselected));
            textView.setTextColor(getColor(R.color.colorDivider));
        }
    }

    private void selectImage(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x01 && resultCode == Activity.RESULT_OK && null != data) {
            Uri uri = data.getData();
            mAddImage.setImageURI(uri);
        } else {
            return;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //同意
                CustomToast.show(this,"权限申请成功");
            } else {
                //权限被拒绝
                CustomToast.show(this,"权限已被拒绝");
            }
        }
    }
}
