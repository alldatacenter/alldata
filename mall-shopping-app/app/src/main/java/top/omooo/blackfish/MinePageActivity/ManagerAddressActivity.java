package top.omooo.blackfish.MinePageActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import top.omooo.blackfish.NewBaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.utils.DensityUtil;
import top.omooo.blackfish.utils.SharePerUtil;

/**
 * Created by SSC on 2018/5/8.
 */

public class ManagerAddressActivity extends NewBaseActivity {


    @BindView(R.id.iv_back)
    ImageView mIvBack;
    @BindView(R.id.btn_new_address)
    Button mBtnNewAddress;
    @BindView(R.id.ll_no_address)
    LinearLayout mLlNoAddress;
    @BindView(R.id.ll_address)
    LinearLayout mLlAddress;
    @BindView(R.id.btn_add_address)
    Button mBtnAddAddress;

    private static final int REQUEST_CODE = 0x01;

    private static final int RESULT_CODE = 0x02;
    private static final String TAG = "ManagerAddressActivity";
    private Context mContext;
    private SharePerUtil mSharePerUtil;

    @Override
    public int getLayoutId() {
        return R.layout.activity_manager_address_layout;
    }

    @Override
    public void initViews() {
        getWindow().setStatusBarColor(getColor(R.color.colorWhite));
        mContext = ManagerAddressActivity.this;
        mSharePerUtil = new SharePerUtil(mContext);
    }

    @Override
    protected void initData() {
        int count = mSharePerUtil.getInt("childCount");
        if (count > 0) {
            mLlNoAddress.setVisibility(View.INVISIBLE);
            for (int i = 0; i <count; i++) {
                mManagerAddressListener.addAddress(mSharePerUtil.getString("name" + i), mSharePerUtil.getString("phone"+i), mSharePerUtil.getString("address"+i), mSharePerUtil.getBoolean("isDefault" + i), true);
            }
        }
    }


    @OnClick({R.id.iv_back, R.id.btn_new_address})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finshActivity();
                break;
            case R.id.btn_new_address:
                Intent intent = new Intent(this, NewAddressActivity.class);
                intent.putExtra("isDefault", !(mLlAddress.getChildCount() > 0));
                startActivityForResult(intent, REQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_CODE) {
            mLlNoAddress.setVisibility(View.INVISIBLE);
            mManagerAddressListener.addAddress(data.getStringExtra("name"), data.getStringExtra("phone"), data.getStringExtra("address"), data.getBooleanExtra("isDefault", false), false);
        }
    }


    private OnManagerAddressListener mManagerAddressListener = new OnManagerAddressListener() {
        @Override
        public void addAddress(String name, String phone, String address, boolean isDefault,boolean isCreate) {
            final int childCount = mLlAddress.getChildCount();
            View view = LayoutInflater.from(mContext).inflate(R.layout.view_address_layout, null);
            TextView textViewName = view.findViewById(R.id.tv_name);
            TextView textViewPhone = view.findViewById(R.id.tv_phone);
            TextView textViewAddress = view.findViewById(R.id.tv_address);
            AppCompatCheckBox checkBox = view.findViewById(R.id.checkbox);
            TextView textViewEdit = view.findViewById(R.id.tv_edit);
            TextView textViewDelete = view.findViewById(R.id.tv_delete);

            textViewName.setText(name);
            textViewPhone.setText(phone);
            textViewAddress.setText(address);
            checkBox.setChecked(isDefault);

            final Bundle bundle = new Bundle();
            bundle.putString("name", name);
            bundle.putString("phone", phone);
            bundle.putString("address", address);
            bundle.putBoolean("isDefaulr", isDefault);
            if (isDefault) {
                checkBox.setClickable(false);
            }
            textViewEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManagerAddressListener.editAddress(bundle);
                }
            });
            textViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManagerAddressListener.deleteAddress(childCount);
                }
            });
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mManagerAddressListener.setDefaultAddress(childCount);
                    }
                }
            });
            mLlAddress.addView(view, childCount);

            setBtnLayout(childCount);
            mBtnAddAddress.setVisibility(View.VISIBLE);
            mBtnAddAddress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, NewAddressActivity.class);
                    intent.putExtra("isDefault", !(mLlAddress.getChildCount() > 0));
                    startActivityForResult(intent, REQUEST_CODE);
                }
            });

            if (!isCreate) {
                mSharePerUtil.saveString("name" + childCount, name);
                mSharePerUtil.saveString("phone" + childCount, phone);
                mSharePerUtil.saveString("address" + childCount, address);
                mSharePerUtil.saveBoolean("isDefault" + childCount, isDefault);
                mSharePerUtil.saveInt("childCount", childCount + 1);
            }
        }

        @Override
        public void editAddress(Bundle bundle) {
            Intent intent = new Intent(mContext, NewAddressActivity.class);
            intent.putExtras(bundle);
            skipActivity(intent);
        }

        @Override
        public void deleteAddress(int index) {
            mLlAddress.removeViewAt(index);
            int count = mLlAddress.getChildCount();
            if (count == 0) {
                mBtnAddAddress.setVisibility(View.INVISIBLE);
                mLlNoAddress.setVisibility(View.VISIBLE);
            }
            setBtnLayout(count - 1);

            mSharePerUtil.deleteValue("name" + index);
            mSharePerUtil.deleteValue("phone" + index);
            mSharePerUtil.deleteValue("address" + index);
            mSharePerUtil.deleteValue("isDefault" + index);
            mSharePerUtil.saveInt("childCount", mSharePerUtil.getInt("childCount") - 1);
        }

        @Override
        public void setDefaultAddress(int index) {
            updateData(index);
        }

        private void updateData(int index) {
            String name = mSharePerUtil.getString("name0");
            String phone = mSharePerUtil.getString("phone0");
            String address = mSharePerUtil.getString("address0");

            mSharePerUtil.saveString("name0", mSharePerUtil.getString("name" + index));
            mSharePerUtil.saveString("phone0", mSharePerUtil.getString("phone" + index));
            mSharePerUtil.saveString("address0", mSharePerUtil.getString("address" + index));

            mSharePerUtil.saveString("name" + index, name);
            mSharePerUtil.saveString("phone" + index, phone);
            mSharePerUtil.saveString("address" + index, address);
            mSharePerUtil.saveBoolean("isDefault" + index, false);

            mLlNoAddress.setVisibility(View.INVISIBLE);
            int count = mLlAddress.getChildCount();
            mLlAddress.removeAllViews();
            for (int i = 0; i <count; i++) {
                mManagerAddressListener.addAddress(mSharePerUtil.getString("name" + i), mSharePerUtil.getString("phone"+i), mSharePerUtil.getString("address"+i), mSharePerUtil.getBoolean("isDefault" + i), true);
            }
        }
    };

    private interface OnManagerAddressListener {
        void addAddress(String name, String phone, String address, boolean isDefault,boolean idCreate);

        void editAddress(Bundle bundle);

        void deleteAddress(int index);

        void setDefaultAddress(int index);
    }

    private void setBtnLayout(int childCount) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mBtnAddAddress.getLayoutParams();
        layoutParams.setMargins(0, DensityUtil.dip2px(mContext, 120 * (childCount + 1) + 50), 0, 0);
        mBtnAddAddress.setLayoutParams(layoutParams);
    }
}
