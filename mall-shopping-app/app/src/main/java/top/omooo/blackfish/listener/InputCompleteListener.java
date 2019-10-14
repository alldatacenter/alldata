package top.omooo.blackfish.listener;

/**
 * Created by SSC on 2018/3/19.
 */

/**
 * 输入完成接口
 * 用于验证码控件输入完成回调
 */
public interface InputCompleteListener {
    void inputComplete();

    void invalidContent();
}
