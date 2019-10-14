package top.omooo.blackfish.MinePageActivity;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import top.omooo.blackfish.BaseActivity;
import top.omooo.blackfish.R;
import top.omooo.router_annotations.Router;

/**
 * Created by SSC on 2018/3/21.
 */
@Router("myBankCard")
public class ActivityMyBankCard extends BaseActivity {

    private TabLayout mTabLayout;
    private TextView mTextTitle;
    private TextView mTextSubtitle;
    private Button mButtonAdd;
    private ImageView mImageBack;

    @Override
    public int getLayoutId() {
        return R.layout.activity_mine_bank_card;
    }

    @Override
    public void initViews() {

        mTabLayout = findView(R.id.tab_layout_my_back_card);
        mTextTitle = findView(R.id.tv_without_bank_card_title);
        mTextSubtitle = findView(R.id.tv_with_bank_card_subtitle);
        mButtonAdd = findView(R.id.btn_bank_card_add);
        mImageBack = findView(R.id.tv_mine_bank_card_back);

        mTabLayout.addTab(mTabLayout.newTab().setText("储蓄卡"), true);
        mTabLayout.addTab(mTabLayout.newTab().setText("信用卡"), true);

        mTabLayout.getTabAt(0).select();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    mTextTitle.setText("暂无储蓄卡");
                    mTextSubtitle.setText("哎呀～您没有绑定储蓄卡");
                    mButtonAdd.setText("添加储蓄卡");
                } else {
                    mTextTitle.setText("暂无信用卡");
                    mTextSubtitle.setText("哎呀～您没有绑定信用卡");
                    mButtonAdd.setText("添加信用卡");
                }
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
    public void initListener() {
        mButtonAdd.setOnClickListener(this);
        mImageBack.setOnClickListener(this);
    }

    @Override
    public void processClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bank_card_add:
                startActivity(new Intent(this, CertifyCardActivity.class));
                break;
            case R.id.tv_mine_bank_card_back:
                finish();
                break;
            default:break;
        }
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTabLayout.clearOnTabSelectedListeners();
    }
}
