package top.omooo.blackfish.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import top.omooo.blackfish.listener.OnNetChangeListener;
import top.omooo.blackfish.utils.NetworkUtil;

public class NetChangedReceiver extends BroadcastReceiver {
    private OnNetChangeListener mOnNetChangeListener;

    public void setOnNetChangeListener(OnNetChangeListener onNetChangeListener) {
        mOnNetChangeListener = onNetChangeListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            int netState = NetworkUtil.getNetworkState(context);
            if (mOnNetChangeListener != null) {
                mOnNetChangeListener.onNetChange(netState);
            }
        }
    }
}
