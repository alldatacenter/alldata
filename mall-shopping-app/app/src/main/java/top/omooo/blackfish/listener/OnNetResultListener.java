package top.omooo.blackfish.listener;

/**
 * Created by SSC on 2018/3/3.
 */

/**
 * 网络请求返回接口
 */
public interface OnNetResultListener {
    void onSuccessListener(String result);
    void onFailureListener(String result);
}
