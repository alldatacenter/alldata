package top.omooo.blackfish.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/4/5.
 */

public class ClassifyTitleAdapter extends RecyclerView.Adapter<ClassifyTitleAdapter.TitleViewHolder> {

    private Context mContext;
    private ArrayList<String> mListTitle;

    private OnClassifyItemClickListener mItemClickListener;

    public ClassifyTitleAdapter(Context context, ArrayList<String> listTitle) {
        mListTitle = new ArrayList<>();
        mContext = context;
        mListTitle = listTitle;
    }

    @Override
    public TitleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TitleViewHolder(LayoutInflater.from(mContext).inflate(R.layout.view_classify_goods_title_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final TitleViewHolder holder, final int position) {
        
        if (position == 0) {
            holder.mTextTitle.setTextColor(Color.parseColor("#FECD15"));
            holder.mFrameLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
        holder.mTextTitle.setText(mListTitle.get(position));
        holder.mFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mListTitle.size();
    }

    class TitleViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextTitle;
        public FrameLayout mFrameLayout;

        public TitleViewHolder(View itemView) {
            super(itemView);
            mTextTitle = itemView.findViewById(R.id.tv_classify_goods_title_item);
            mFrameLayout = itemView.findViewById(R.id.fl_classify_layout);
        }
    }

    public void setOnClassifyItemClickListener(OnClassifyItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public interface OnClassifyItemClickListener {
        void onItemClick(int position);
    }

}
