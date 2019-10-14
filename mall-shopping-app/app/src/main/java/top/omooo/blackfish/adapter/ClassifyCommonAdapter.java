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
import top.omooo.blackfish.bean.ClassifyGridInfo;

/**
 * Created by SSC on 2018/4/5.
 */

public class ClassifyCommonAdapter extends BaseAdapter {

    private Context mContext;
    private List<ClassifyGridInfo> mGridInfos;

    public ClassifyCommonAdapter(Context context, List<ClassifyGridInfo> gridInfos) {
        mGridInfos = new ArrayList<>();
        mContext = context;
        mGridInfos = gridInfos;
    }

    @Override
    public int getCount() {
        return mGridInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mGridInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MyViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new MyViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.view_classify_common_item_layout, null);
            viewHolder.mDraweeView = convertView.findViewById(R.id.iv_classify_common_item);
            viewHolder.mTextView = convertView.findViewById(R.id.tv_classify_common_item);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (MyViewHolder) convertView.getTag();
        }
        viewHolder.mDraweeView.setImageURI(mGridInfos.get(position).getImageUrl());
        viewHolder.mTextView.setText(mGridInfos.get(position).getName());
        return convertView;
    }

    class MyViewHolder{
        public SimpleDraweeView mDraweeView;
        public TextView mTextView;
    }
}
