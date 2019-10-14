package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/3/16.
 */

public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder> {

    private Context mContext;
    private String[] imageUrl = new String[4];

    public HomeBannerAdapter(Context context, String[] imageUrl) {
        mContext = context;
        this.imageUrl = imageUrl;
    }

    @Override
    public BannerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.home_pager_banner_item, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BannerViewHolder holder, int position) {
        holder.mBannerView.setImageURI(imageUrl[position]);
    }

    @Override
    public int getItemCount() {
        return imageUrl.length;
    }

    class BannerViewHolder extends RecyclerView.ViewHolder {
        public SimpleDraweeView mBannerView;
        public BannerViewHolder(View itemView) {
            super(itemView);
            mBannerView = itemView.findViewById(R.id.iv_home_banner);
        }
    }
}
