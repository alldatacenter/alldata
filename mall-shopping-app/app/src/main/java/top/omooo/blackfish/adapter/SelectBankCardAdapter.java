package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import top.omooo.blackfish.R;
import top.omooo.blackfish.bean.BankCardsInfo;

/**
 * Created by SSC on 2018/3/27.
 */

public class SelectBankCardAdapter extends RecyclerView.Adapter<SelectBankCardAdapter.MyViewHolder> {

    private Context mContext;
    private List<BankCardsInfo> mCardsInfoList;
    private OnItemLayoutClickListener mClickListener;

    public void setOnItemClickListener(OnItemLayoutClickListener listener) {
        this.mClickListener = listener;
    }

    public SelectBankCardAdapter(Context context, List<BankCardsInfo> cardsInfoList) {
        mContext = context;
        mCardsInfoList = cardsInfoList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_select_card_item_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        String imageUrl = mCardsInfoList.get(position).getLogoUrl();
        String cardName = mCardsInfoList.get(position).getName();
        holder.mImageView.setImageURI(imageUrl);
        holder.mTextView.setText(cardName);
        holder.mRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCardsInfoList.size();
    }


    class MyViewHolder extends RecyclerView.ViewHolder{
        public SimpleDraweeView mImageView;
        public TextView mTextView;
        public RelativeLayout mRelativeLayout;

        public MyViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.iv_select_card_icon);
            mTextView = itemView.findViewById(R.id.iv_select_card_text);
            mRelativeLayout = itemView.findViewById(R.id.rl_select_card);
        }
    }

    public interface OnItemLayoutClickListener {
        void onItemClick(int position);
    }
}
