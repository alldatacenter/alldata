package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;

/**
 * Created by SSC on 2018/3/1.
 */

public class GeneralVLayoutAdapter extends DelegateAdapter.Adapter<GeneralVLayoutAdapter.MainViewHolder> {

    private Context mContext;
    private LayoutHelper mLayoutHelper;
    private VirtualLayoutManager.LayoutParams mLayoutParams;
    private int mCount = 0;

    public GeneralVLayoutAdapter(Context context, LayoutHelper layoutHelper, VirtualLayoutManager.LayoutParams layoutParams, int count) {
        mContext = context;
        mLayoutHelper = layoutHelper;
        mLayoutParams = layoutParams;
        mCount = count;
    }

    public GeneralVLayoutAdapter(Context context, LayoutHelper layoutHelper, int count) {
        this(context, layoutHelper, null, count);
    }

    @Override
    public LayoutHelper onCreateLayoutHelper() {
        return mLayoutHelper;
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(MainViewHolder holder, int position) {
        if (mLayoutParams != null) {
            holder.itemView.setLayoutParams(new VirtualLayoutManager.LayoutParams(mLayoutParams));
        }
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    public class MainViewHolder extends RecyclerView.ViewHolder {
        public MainViewHolder(View itemView) {
            super(itemView);
        }
    }
}
