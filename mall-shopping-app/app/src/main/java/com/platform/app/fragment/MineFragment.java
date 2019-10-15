package com.platform.app.fragment;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.activity.AddressListActivity;
import com.platform.app.activity.LoginActivity;
import com.platform.app.activity.MyFavoriteActivity;
import com.platform.app.activity.MyOrdersActivity;
import com.platform.app.bean.User;
import com.platform.app.contants.Contants;
import com.platform.app.utils.GlideUtils;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.CircleImageView;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/08/02
 *     desc   : 我的 fragment
 *     version: 1.0
 * </pre>
 */
public class MineFragment extends BaseFragment {

    @BindView(R.id.img_head)
    CircleImageView mImageHead;
    @BindView(R.id.txt_username)
    TextView        mTxtUserName;
    @BindView(R.id.btn_logout)
    Button          mbtnLogout;

    @Override
    protected void init() {
        User user = MallShoppingApp.getInstance().getUser();
        showUser(user);
    }

    @Override
    protected int getContentResourseId() {
        return R.layout.fragment_mine;
    }


    @OnClick({R.id.txt_my_address, R.id.txt_my_favorite, R.id.txt_my_orders, R.id.txt_username, R
            .id.img_head, R.id.btn_logout})
    public void onclick(View view) {
        switch (view.getId()) {
            //收货地址
            case R.id.txt_my_address:
                startActivity(new Intent(getActivity(), AddressListActivity.class), true);
                break;
            //我的收藏
            case R.id.txt_my_favorite:
                startActivity(new Intent(getActivity(), MyFavoriteActivity.class), true);
                break;
            //我的订单
            case R.id.txt_my_orders:
                startActivity(new Intent(getActivity(), MyOrdersActivity.class), true);
                break;
            case R.id.txt_username:
            case R.id.img_head:
                User user = MallShoppingApp.getInstance().getUser();
                if (user == null) {
                    Intent intent2 = new Intent(getActivity(), LoginActivity.class);
                    startActivityForResult(intent2, Contants.REQUEST_CODE);
                } else {
                    ToastUtils.showSafeToast(getContext(), "更换头像或修改昵称");
                }
                break;
            case R.id.btn_logout:
                MallShoppingApp.getInstance().clearUser();
                showUser(null);
                break;
            default:
                break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        User user = MallShoppingApp.getInstance().getUser();
        showUser(user);
    }

    private void showUser(User user) {
        if (user != null) {
            mTxtUserName.setText(user.getUsername());
            GlideUtils.portrait(getContext(), user.getLogo_url(), mImageHead);
        } else {
            mTxtUserName.setText("请登陆");
            GlideUtils.portrait(getContext(), null, mImageHead);
        }
    }
}
