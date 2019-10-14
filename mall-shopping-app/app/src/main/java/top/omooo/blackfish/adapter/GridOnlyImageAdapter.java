package top.omooo.blackfish.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.BannerInfo;
import top.omooo.blackfish.view.CustomToast;

/**
 * Created by SSC on 2018/4/8.
 */

public class GridOnlyImageAdapter extends BaseAdapter {

    private Context mContext;
    private List<BannerInfo> mBannerInfos;

    public GridOnlyImageAdapter(Context context, List<BannerInfo> bannerInfos) {
        mContext = context;
        mBannerInfos = bannerInfos;
    }

    @Override
    public int getCount() {
        return mBannerInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return mBannerInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        GridViewHolder gridViewHolder;
        if (convertView == null) {
            gridViewHolder = new GridViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.view_one_image_layout, null);
            gridViewHolder.mSimpleDraweeView = convertView.findViewById(R.id.iv_grid_item);
            convertView.setTag(gridViewHolder);
        } else {
            gridViewHolder = (GridViewHolder) convertView.getTag();
        }
        gridViewHolder.mSimpleDraweeView.setImageURI(mBannerInfos.get(position).getUrl());
        gridViewHolder.mSimpleDraweeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.show(mContext, "Item " + position + " 跳转到商品详情页");
            }
        });
        return convertView;
    }

    class GridViewHolder {
        public SimpleDraweeView mSimpleDraweeView;
    }
}
