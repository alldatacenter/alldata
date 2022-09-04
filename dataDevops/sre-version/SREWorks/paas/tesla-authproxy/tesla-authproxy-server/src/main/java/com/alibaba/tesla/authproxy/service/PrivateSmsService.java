package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateSmsSignatureForbidden;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.web.output.PrivateSmsConfigResult;

public interface PrivateSmsService {

    void register(String endpoint, String token) throws AuthProxyThirdPartyError, PrivateSmsSignatureForbidden, PrivateValidationError;

    PrivateSmsConfigResult getConfig();

    void sendMessage(String phone, String aliyunId, String code, String content) throws AuthProxyThirdPartyError;

}
