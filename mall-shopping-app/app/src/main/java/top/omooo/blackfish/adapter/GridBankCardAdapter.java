package top.omooo.blackfish.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.BankCardsInfo;

/**
 * Created by SSC on 2018/3/24.
 */

public class GridBankCardAdapter extends BaseAdapter {

    private List<BankCardsInfo> mBankCardsInfos;
    private LayoutInflater mInflater;

    public GridBankCardAdapter(List<BankCardsInfo> bankCardsInfos, Context context) {
        mBankCardsInfos = new ArrayList<>();
        mBankCardsInfos = bankCardsInfos;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mBankCardsInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mBankCardsInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.grid_bank_cards_item_layout, parent, false);
            viewHolder.mImageView = convertView.findViewById(R.id.iv_item_bank_cards_logo);
            viewHolder.mTextView = convertView.findViewById(R.id.tv_item_bank_cards_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BankCardsInfo info = mBankCardsInfos.get(position);
        viewHolder.mTextView.setText(info.getName());
        viewHolder.mImageView.setImageURI(info.getLogoUrl());
        return convertView;
    }
    class ViewHolder{
        public SimpleDraweeView mImageView;
        public TextView mTextView;
    }
}
