package top.omooo.blackfish.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import top.omooo.blackfish.R;

/**
 * Created by SSC on 2018/3/29.
 */

public class PickerAdapter extends RecyclerView.Adapter<PickerAdapter.PickerViewHolder> {
    private Context mContext;
    private String[] mStrings;
    private static final String TAG = "PickerAdapter";

    public PickerAdapter(Context context, String[] strings) {
        mContext = context;
        mStrings = new String[strings.length];
        mStrings = strings;
    }

    @Override
    public PickerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_picker_item_layout, parent, false);
        return new PickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PickerViewHolder holder, final int position) {
        holder.mTextView.setText(mStrings[position]);
        holder.mFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: " + mStrings[position]);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStrings.length;
    }

    class PickerViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextView;
        private FrameLayout mFrameLayout;

        private PickerViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.tv_picker_item);
            mFrameLayout = itemView.findViewById(R.id.fl_picker_item);
        }
    }
}
