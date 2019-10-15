package com.platform.app.adapter;

import android.widget.CheckBox;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.platform.app.R;
import com.platform.app.data.dao.Address;

import java.util.List;

/**
 * Created by wulinhao
 * Time  2019/8/11
 * Describe: 收货地址 适配器
 */

public class AddressListAdapter extends BaseQuickAdapter<Address, BaseViewHolder> {

    public AddressListAdapter(List<Address> datas) {
        super(R.layout.template_address, datas);
    }

    @Override
    protected void convert(BaseViewHolder holder, Address item) {

        holder.setText(R.id.txt_name, item.getName())
                .setText(R.id.txt_phone, item.getPhone())
                .setText(R.id.txt_address, item.getAddress())
                .addOnClickListener(R.id.cb_is_defualt)
                .addOnClickListener(R.id.txt_edit)
                .addOnClickListener(R.id.txt_del);

        ((CheckBox)holder.getView(R.id.cb_is_defualt)).setChecked(item.getIsDefaultAddress());

    }
}


