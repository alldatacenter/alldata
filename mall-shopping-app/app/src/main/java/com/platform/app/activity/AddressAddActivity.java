package com.platform.app.activity;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.bigkoo.pickerview.OptionsPickerView;
import com.platform.app.MallShoppingApp;
import com.platform.app.R;
import com.platform.app.bean.PickerCityAddressBean;
import com.platform.app.data.DataManager;
import com.platform.app.data.dao.Address;
import com.platform.app.utils.GetJsonDataUtil;
import com.platform.app.utils.KeyBoardUtils;
import com.platform.app.utils.ToastUtils;
import com.platform.app.widget.ClearEditText;
import com.platform.app.widget.EnjoyshopToolBar;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * Created by wulinhao
 * Time  2019/8/11
 * Describe: 添加收货地址
 */

public class AddressAddActivity extends BaseActivity {

    //三级联动 github地址   //https://github.com/saiwu-bigkoo/Android-PickerView    start:4953

    @BindView(R.id.toolbar)
    EnjoyshopToolBar mToolBar;
    @BindView(R.id.edittxt_consignee)
    ClearEditText    mEditConsignee;
    @BindView(R.id.edittxt_phone)
    ClearEditText    mEditPhone;
    @BindView(R.id.txt_address)
    TextView         mTxtAddress;
    @BindView(R.id.edittxt_add)
    ClearEditText    mEditAddr;


    private ArrayList<PickerCityAddressBean>        options1Items = new ArrayList<>();
    private ArrayList<ArrayList<String>>            options2Items = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<String>>> options3Items = new ArrayList<>();
    private boolean                                 isLoaded      = false;
    private Thread thread;
    private static final int MSG_LOAD_DATA    = 0x0001;
    private static final int MSG_LOAD_SUCCESS = 0x0002;
    private static final int MSG_LOAD_FAILED  = 0x0003;

    /**
     * 默认只有1条数据
     */
    private boolean isOnlyAddress = true;

    /**
     * 数据库的操作类型.
     * 如果是0 就是增加(默认)
     * 1 就是修改
     */
    private int addressDoType = 0;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_DATA:
                    if (thread == null) {//如果已创建就不再重新创建子线程了
                        thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // 写子线程中的操作,解析省市区数据
                                initJsonData();
                            }
                        });
                        thread.start();
                    }
                    break;
                case MSG_LOAD_SUCCESS:
                    isLoaded = true;
                    break;
                case MSG_LOAD_FAILED:
                    ToastUtils.showSafeToast(AddressAddActivity.this, "数据获取失败,请重试");
                    break;

            }
        }
    };

    @Override
    protected int getContentResourseId() {
        return R.layout.activity_address_add;
    }


    @Override
    protected void init() {

        mHandler.sendEmptyMessage(MSG_LOAD_DATA);

        String name = (String) getIntent().getSerializableExtra("name");

        if (name != null) {
            addressDoType = 1;
            editAddress();
        } else {
            addressDoType = 0;
        }


        mToolBar.setRightButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAddress();
            }
        });
    }


    @OnClick({R.id.txt_address})
    public void viewClick(View view) {
        switch (view.getId()) {
            case R.id.txt_address:
                if (isLoaded) {
                    KeyBoardUtils.closeKeyboard(mEditConsignee,AddressAddActivity.this);
                    KeyBoardUtils.closeKeyboard(mEditPhone,AddressAddActivity.this);
                    KeyBoardUtils.closeKeyboard(mEditAddr,AddressAddActivity.this);
                    ShowPickerView();
                } else {
                    ToastUtils.showSafeToast(AddressAddActivity.this, "请稍等,数据获取中");
                }
                break;
        }
    }

    /**
     * 修改新的地址
     */
    private void editAddress() {

        String name = (String) getIntent().getSerializableExtra("name");
        String phone = (String) getIntent().getSerializableExtra("phone");
        String bigAddress = (String) getIntent().getSerializableExtra("BigAddress");
        String smallAddress = (String) getIntent().getSerializableExtra("SmallAddress");

        mEditConsignee.setText(name);
        mEditConsignee.setSelection(name.length());
        mEditPhone.setText(phone);
        mTxtAddress.setText(bigAddress);
        mEditAddr.setText(smallAddress);
    }

    /**
     * 创建新的地址
     */
    public void createAddress() {

        String consignee = mEditConsignee.getText().toString();    //收件人
        String phone = mEditPhone.getText().toString();

        String smallAddress = mEditAddr.getText().toString();
        String bigAddress = mTxtAddress.getText().toString();
        String address = bigAddress + smallAddress;

        //进行非空判断
        if (TextUtils.isEmpty(consignee)) {
            ToastUtils.showSafeToast(AddressAddActivity.this, "收件人为空,请检查");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            ToastUtils.showSafeToast(AddressAddActivity.this, "联系电话为空,请检查");
            return;
        }

        if (TextUtils.isEmpty(smallAddress) || TextUtils.isEmpty(bigAddress)) {
            ToastUtils.showSafeToast(AddressAddActivity.this, "地址不完整,请检查");
            return;
        }

        Long userId = MallShoppingApp.getApplication().getUser().getId();

        List<Address> mAddressDataList = DataManager.queryAddress(userId);
        if (mAddressDataList != null && mAddressDataList.size() == 0) {
            isOnlyAddress = true;
        } else {
            isOnlyAddress = false;
        }

        Address addBean = new Address();
        addBean.setUserId(userId);
        addBean.setName(consignee);
        addBean.setPhone(phone);
        if (isOnlyAddress){
            addBean.setIsDefaultAddress(true);
        }else {
            addBean.setIsDefaultAddress(false);
        }

        addBean.setBigAddress(bigAddress);
        addBean.setSmallAddress(smallAddress);
        addBean.setAddress(address);
        if (addressDoType == 0) {
            DataManager.insertAddress(addBean);
            ToastUtils.showSafeToast(AddressAddActivity.this, "地址添加成功");
        } else if (addressDoType == 1) {
            Long addressId = (Long) getIntent().getSerializableExtra("addressId");
            addBean.setAddressId(addressId);
            DataManager.updateAddress(addBean);
            ToastUtils.showSafeToast(AddressAddActivity.this, "地址修改成功");
        }

        finish();

        //
        //        ToastUtils.showSafeToast(RegSecondActivity.this, "注册成功");
        //        startActivity(new Intent(RegSecondActivity.this, LoginActivity.class));
        //        finish();
        //
        //
        //
        //
        //
        //
        //        String url = HttpContants.ADDRESS_CREATE + "?user_id=" + id + "&consignee=" +
        // consignee +
        //                "&phone=" + phone +
        //                "&addr=" + address + "&zip_code=" + "000000";

        //        OkHttpUtils.post().url(url).build().execute(new StringCallback() {
        //            @Override
        //            public void onError(Call call, Exception e, int id) {
        //                LogUtil.e("添加新地址", "失败" + e, true);             //   java.io
        // .IOException: request
        //
        //                ToastUtils.showSafeToast(AddressAddActivity.this,"地址添加失败,请重试");
        //            }
        //
        //            @Override
        //            public void onResponse(String response, int id) {
        //                LogUtil.e("添加新地址", "成功" + response, true);
        //
        //                ToastUtils.showSafeToast(AddressAddActivity.this,"地址添加成功");
        //            }
        //        });


    }


    private void ShowPickerView() {// 弹出选择器

        OptionsPickerView pvOptions = new OptionsPickerView.Builder(this, new OptionsPickerView
                .OnOptionsSelectListener() {
            @Override
            public void onOptionsSelect(int options1, int options2, int options3, View v) {
                //返回的分别是三个级别的选中位置
                String tx = options1Items.get(options1).getPickerViewText() +
                        options2Items.get(options1).get(options2) +
                        options3Items.get(options1).get(options2).get(options3);

                mTxtAddress.setText(tx);
            }
        })

                .setTitleText("城市选择")
                .setDividerColor(Color.BLACK)
                .setTextColorCenter(Color.BLACK) //设置选中项文字颜色
                .setContentTextSize(20)
                .build();

        pvOptions.setPicker(options1Items, options2Items, options3Items);       //三级选择器
        pvOptions.show();
    }


    private void initJsonData() {//解析数据

        /**
         * 注意：assets 目录下的Json文件仅供参考，实际使用可自行替换文件
         * 关键逻辑在于循环体
         *
         * */
        String JsonData = GetJsonDataUtil.getJson(this, "province.json");
        //获取assets目录下的json文件数据

        ArrayList<PickerCityAddressBean> jsonBean = parseData(JsonData);     //用Gson 转成实体

        /**
         * 添加省份数据
         *
         * 注意：如果是添加的JavaBean实体，则实体类需要实现 IPickerViewData 接口，
         * PickerView会通过getPickerViewText方法获取字符串显示出来。
         */
        options1Items = jsonBean;

        for (int i = 0; i < jsonBean.size(); i++) {//遍历省份
            ArrayList<String> CityList = new ArrayList<>();//该省的城市列表（第二级）
            ArrayList<ArrayList<String>> Province_AreaList = new ArrayList<>();//该省的所有地区列表（第三极）

            for (int c = 0; c < jsonBean.get(i).getCityList().size(); c++) {//遍历该省份的所有城市
                String CityName = jsonBean.get(i).getCityList().get(c).getName();
                CityList.add(CityName);//添加城市

                ArrayList<String> City_AreaList = new ArrayList<>();//该城市的所有地区列表

                //如果无地区数据，建议添加空字符串，防止数据为null 导致三个选项长度不匹配造成崩溃
                if (jsonBean.get(i).getCityList().get(c).getArea() == null
                        || jsonBean.get(i).getCityList().get(c).getArea().size() == 0) {
                    City_AreaList.add("");
                } else {

                    for (int d = 0; d < jsonBean.get(i).getCityList().get(c).getArea().size();
                         d++) {//该城市对应地区所有数据
                        String AreaName = jsonBean.get(i).getCityList().get(c).getArea().get(d);

                        City_AreaList.add(AreaName);//添加该城市所有地区数据
                    }
                }
                Province_AreaList.add(City_AreaList);//添加该省所有地区数据
            }

            //添加城市数据
            options2Items.add(CityList);
            //添加地区数据
            options3Items.add(Province_AreaList);
        }

        mHandler.sendEmptyMessage(MSG_LOAD_SUCCESS);

    }


    public ArrayList<PickerCityAddressBean> parseData(String result) {    //Gson 解析
        ArrayList<PickerCityAddressBean> detail = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(result);
            Gson gson = new Gson();
            for (int i = 0; i < data.length(); i++) {
                PickerCityAddressBean entity = gson.fromJson(data.optJSONObject(i).toString(),
                        PickerCityAddressBean.class);
                detail.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(MSG_LOAD_FAILED);
        }
        return detail;
    }


}
