package top.omooo.blackfish.MinePageActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.lljjcoder.Interface.OnCityItemClickListener;
import com.lljjcoder.bean.CityBean;
import com.lljjcoder.bean.DistrictBean;
import com.lljjcoder.bean.ProvinceBean;
import com.lljjcoder.citywheel.CityConfig;
import com.lljjcoder.style.citypickerview.CityPickerView;

import butterknife.BindView;
import butterknife.OnClick;
import top.omooo.blackfish.NewBaseActivity;
import top.omooo.blackfish.R;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/5/8.
 */

public class NewAddressActivity extends NewBaseActivity {


    @BindView(R.id.iv_back)
    ImageView mIvBack;
    @BindView(R.id.et_name)
    EditText mEtName;
    @BindView(R.id.et_phone)
    EditText mEtPhone;
    @BindView(R.id.tv_area)
    TextView mTvArea;
    @BindView(R.id.et_address)
    EditText mEtAddress;
    @BindView(R.id.btn_save)
    Button mBtnSave;
    @BindView(R.id.checkbox)
    AppCompatCheckBox mCheckbox;
    @BindView(R.id.tv_title)
    TextView mTvTitle;

    private String mArea;
    private boolean isDefault;

    private CityPickerView mPickerView = new CityPickerView();


    @Override
    public int getLayoutId() {
        return R.layout.activity_new_address_layout;
    }

    @Override
    public void initViews() {
        getWindow().setStatusBarColor(getColor(R.color.colorWhite));
        isDefault = getIntent().getBooleanExtra("isDefault", false);
        if (isDefault) {
            mCheckbox.setChecked(true);
            mCheckbox.setClickable(false);
        }


        CityConfig cityConfig = new CityConfig.Builder().build();
        mPickerView.setConfig(cityConfig);
        mPickerView.init(this);
    }

    @Override
    protected void initData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String name = bundle.getString("name");
            String phone = bundle.getString("phone");
            String address = "所在地区：" + bundle.getString("address");
            boolean isDefault = bundle.getBoolean("isDefault");
            mEtName.setText(name);
            mEtPhone.setText(phone);
//            mTvArea.setText(address);
            mCheckbox.setChecked(isDefault);
        }
    }

    @OnClick({R.id.iv_back, R.id.tv_area, R.id.btn_save})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_back:
                finshActivity();
                break;
            case R.id.tv_area:
                mPickerView.setOnCityItemClickListener(new OnCityItemClickListener() {
                    @Override
                    public void onSelected(ProvinceBean province, CityBean city, DistrictBean district) {
                        mArea = " " + province.getName() + " " + city.getName() + " " + district.getName() + "  ";
                        String text = "所在地区：" + mArea;
                        mTvArea.setText(text);
                    }
                });
                mPickerView.showCityPicker();
                break;
            case R.id.btn_save:
                String name = mEtName.getText().toString();
                String phone = mEtPhone.getText().toString();
                String address = mArea + mEtAddress.getText().toString();
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(phone) && !TextUtils.isEmpty(address) && !TextUtils.isEmpty(mArea)) {
                    CustomToast.show(this, "保存");
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("name", name);
                    bundle.putString("phone", phone);
                    bundle.putString("address", address);
                    bundle.putBoolean("isDefault", mCheckbox.isChecked());
                    intent.putExtras(bundle);
                    setResult(0x02, intent);
                    finshActivity();
                } else {
                    CustomToast.show(this, "请正确填写信息");
                }
                break;
        }
    }

}
