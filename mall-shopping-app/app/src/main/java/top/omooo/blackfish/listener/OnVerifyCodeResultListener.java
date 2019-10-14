package top.omooo.blackfish.listener;

/**
 * Created by SSC on 2018/3/19.
 */

/**
 * 验证码状态
 */
public interface OnVerifyCodeResultListener {
    void sendCodeSuccess(); //发送成功

    void sendCodeFailure(); //发送失败

    void submitCodeSuccess(String phoneNumber, String date); //验证成功

    void submitCodeFailure(); //验证失败
}
