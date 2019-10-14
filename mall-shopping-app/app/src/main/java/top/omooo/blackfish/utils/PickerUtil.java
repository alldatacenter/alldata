package top.omooo.blackfish.utils;

import android.app.Activity;
import android.app.Dialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.cncoderx.wheelview.OnWheelChangedListener;
import com.cncoderx.wheelview.Wheel3DView;
import com.cncoderx.wheelview.WheelView;

import top.omooo.blackfish.R;
import top.omooo.blackfish.adapter.PickerAdapter;

/**
 * Created by SSC on 2018/3/29.
 */

public class PickerUtil {
    private Dialog mDialog;
    private TextView mTextCancel, mTextDeterMine;
    private RecyclerView mRecyclerView;
    private Wheel3DView mWheel3DView;

    /**
     * 怕是要丢掉咯
     * @param activity
     * @param strings
     */
    public void showPicker(Activity activity, String[] strings) {
        mDialog = new Dialog(activity, R.style.BottomDialogStyle);
        View view = LayoutInflater.from(activity).inflate(R.layout.view_picker_layout, null);

        mTextCancel = view.findViewById(R.id.tv_picker_cancel);
        mTextDeterMine = view.findViewById(R.id.tv_picker_determine);
        mRecyclerView = view.findViewById(R.id.rv_picker);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        mRecyclerView.setAdapter(new PickerAdapter(activity,strings));

        mTextCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        mTextDeterMine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mDialog.setContentView(view);
        Window window = mDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = DensityUtil.getScreenWidth(activity);
        layoutParams.height = DensityUtil.dip2px(activity, 300);
        layoutParams.gravity = Gravity.BOTTOM;
        window.setAttributes(layoutParams);
        mDialog.show();
    }

    public void showCustomPicker(Activity activity, int arrayId, final OnSelectFinshListener listener) {
        mDialog = new Dialog(activity, R.style.BottomDialogStyle);
        View view = LayoutInflater.from(activity).inflate(R.layout.view_custom_picker_layout, null);

        mTextCancel = view.findViewById(R.id.tv_picker_custom_cancel);
        mTextDeterMine = view.findViewById(R.id.tv_picker_custom_determine);
        mWheel3DView = view.findViewById(R.id.wheel_view_picker);

        String[] arrayList = activity.getResources().getStringArray(arrayId);
        mWheel3DView.setEntries(arrayList);
        mTextCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });

        mWheel3DView.setOnWheelChangedListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView view, int oldIndex, int newIndex) {
                listener.onSelected(view.getItem(newIndex).toString());
            }
        });

        // TODO: 2018/3/30 取消和确定按钮不能占用Wheel3DView的控件 ，否则点击事件不响应
        /**
         * 还是自己写选择器比较靠谱，留个坑吧。
         */
        mTextDeterMine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWheel3DView.setOnWheelChangedListener(new OnWheelChangedListener() {
                    @Override
                    public void onChanged(WheelView view, int oldIndex, int newIndex) {
                        listener.onSelected(view.getItem(newIndex).toString());
                    }
                });
                mDialog.dismiss();
            }
        });
        mDialog.setContentView(view);
        Window window = mDialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = DensityUtil.getScreenWidth(activity);
        layoutParams.height = DensityUtil.dip2px(activity, 300);
        layoutParams.gravity = Gravity.BOTTOM;
        window.setAttributes(layoutParams);
        mDialog.show();
    }

    public interface OnSelectFinshListener {
        String onSelected(String result);
    }
}
