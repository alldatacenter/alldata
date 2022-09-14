package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.outbound.acs.AcsClientFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 专有云场景下的 access key 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PrivateAccessKeyServiceImpl {

    @Autowired
    private AcsClientFactory acsClientFactory;

    ///**
    // * 根据 Access Id 获取对应的 Access Key 信息
    // *
    // * @param accessId Access Id
    // * @return AccessKeyInfo 对象
    // * @throws AuthProxyThirdPartyError 当 AK 系统出错时抛出
    // */
    //public AccessKeyInfo getAccessKeyInfo(@NonNull String accessId) throws AuthProxyThirdPartyError {
    //    AkClient client = acsClientFactory.getAkClient();
    //    try {
    //        return client.getAccessKeyInfo(accessId);
    //    } catch (IOException e) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AK, e.getMessage());
    //    }
    //}
}
