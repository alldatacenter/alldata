package top.omooo.blackfish.MinePageActivity;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.AdjustViewUtil;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/3/21.
 */

public class AboutActivity extends BaseActivity {

    private TextView mTextVersion,mTextSug,mTextContact, mTextCheck;
    private ImageView mImageBack;
    @Override
    public int getLayoutId() {
        return R.layout.activity_about_black_fish;
    }

    @Override
    public void initViews() {

        mTextVersion = findView(R.id.tv_about_version);
        mTextSug = findView(R.id.tv_about_sug);
        mTextContact = findView(R.id.tv_about_contact);
        mTextCheck = findView(R.id.tv_about_check);

        mImageBack = findView(R.id.tv_mine_about_back);

        new AdjustViewUtil().adjustTextViewPic(mTextVersion, 1, 0, 0, 130, 130);
    }

    @Override
    public void initListener() {
        mTextSug.setOnClickListener(this);
        mTextContact.setOnClickListener(this);
        mTextCheck.setOnClickListener(this);
        mImageBack.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.tv_about_sug:
                startActivity(new Intent(this, SuggestionActivity.class));
                break;
            case R.id.tv_about_contact:
                CustomToast.show(this,"联系客服：待开发");
                break;
            case R.id.tv_about_check:
//                Toast.makeText(this, "检查更新", Toast.LENGTH_SHORT).show();
                CustomToast.show(this,"当前已经是最新版本");
                break;
            case R.id.tv_mine_about_back:
                finish();
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }
}
