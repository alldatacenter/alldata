package com.platform.mall.service;


/**
 * @author AllDataDC
 */
public interface RegisterService {

    /**
     * 勾选
     * @param param
     * @param type
     * @return
     */
    boolean checkData(String param, int type);

    /**
     * 注册
     * @param userName
     * @param userPwd
     * @return
     */
    int register(String userName,String userPwd);
}
