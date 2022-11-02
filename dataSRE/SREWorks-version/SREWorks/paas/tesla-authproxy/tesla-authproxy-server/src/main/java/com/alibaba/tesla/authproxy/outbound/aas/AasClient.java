package com.alibaba.tesla.authproxy.outbound.aas;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateAasVerifyFailed;
import com.alibaba.tesla.authproxy.outbound.acs.AcsClientFactory;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AAS 客户端，包含登录等功能
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class AasClient {

    @Autowired
    private AcsClientFactory acsClientFactory;

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private AuthProperties authProperties;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 直接登录 AAS 系统接口
     *
     * @param aliyunId Aliyun ID
     * @param password 密码
     * @throws AuthProxyThirdPartyError AAS 系统错误时抛出
     */
    public AasLoginResult login(String aliyunId, String password) throws AuthProxyThirdPartyError {
        FormBody.Builder formData = new FormBody.Builder();
        formData.add("username", aliyunId);
        formData.add("password", password);

        // 组装请求，发送到 AAS 之中，并生成对应的 AAS 登录结果解析内容
        Request request = new Request.Builder()
            .url(authProperties.getAasDirectLoginUrl())
            .post(formData.build())
            .build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            List<Cookie> cookies = Cookie.parseAll(request.url(), response.headers());
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("NULL body from aas doLogin");
            }
            return new AasLoginResult(responseBody.string(), cookies);
        } catch (IOException e) {
            AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS, e.getMessage());
            se.initCause(e);
            throw se;
        }
    }

    /**
     * 调用 AAS 的 HTTP 接口校验登录 Ticket 信息，并获取返回的用户详细信息
     *
     * @param ticket AAS Cookie Ticket
     * @throws PrivateAasVerifyFailed 当校验失败（ticket 失效）时抛出，此时需要重新登录
     * @throws AuthProxyThirdPartyError AAS 系统错误时抛出
     */
    public AasUserInfo loadUserInfo(String ticket) throws AuthProxyThirdPartyError, PrivateAasVerifyFailed {
        // 朝 AAS 发起用户信息校验请求
        //String response;
        //try {
        //    AliyunidClient client = acsClientFactory.getAliyunidClient();
        //    List<OAuthPair> params = new ArrayList<>();
        //    params.add(new OAuthPair("oauth_ticket", ticket));
        //    response = client.callApi(authProperties.getAasLoadTicketUrl(), params);
        //} catch (OAuthException e) {
        //    log.warn("Call aas load ticket failed, ticket={}, url={}, exception={}", ticket,
        //        authProperties.getAasLoadTicketUrl(), ExceptionUtils.getStackTrace(e));
        //    AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS, e.getMessage());
        //    se.initCause(e);
        //    throw se;
        //}
        //
        //// 检查返回结果是否正确，不正确抛出异常
        //if (response.contains("errorCode")) {
        //    log.warn("AAS ticket verify failed, ticket={}, response={}", ticket, response);
        //    throw new PrivateAasVerifyFailed();
        //}
        //AasUserInfo responseUser = JSONObject.parseObject(response, AasUserInfo.class);
        //if (responseUser == null || responseUser.getAliyunID() == null || responseUser.getAliyunID().length() == 0) {
        //    log.warn("AAS ticket verify failed, ticket={}, response={}", ticket, response);
        //    throw new PrivateAasVerifyFailed();
        //}
        //log.info("AAS ticket verify successful, ticket={}, response={}", ticket, response);
        //return responseUser;
        throw new RuntimeException("not support");
    }

    ///**
    // * 获取用户当前的 Access Keys 列表
    // *
    // * @param aliyunPk 需要获取的用户 Aliyun PK
    // * @throws AuthProxyThirdPartyError AAS 系统错误时抛出
    // */
    //public List<AccessKey> getUserAccessKeys(String aliyunPk) throws AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getAasClient();
    //    ListAccessKeysForAccountRequest request = new ListAccessKeysForAccountRequest();
    //    request.setPK(aliyunPk);
    //    request.setAKStatus("Active");
    //    request.setAKType("Symmetric");
    //    log.info("Call AAS ListAccessKeysForAccount, request={}", gson.toJson(request));
    //    ListAccessKeysForAccountResponse response;
    //    try {
    //        response = acsClient.getAcsResponse(request);
    //    } catch (ClientException e) {
    //        AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS, e.getMessage());
    //        se.initCause(e);
    //        throw se;
    //    }
    //    log.info("Call AAS ListAccessKeysForAccount, response={}", gson.toJson(response));
    //    return response.getAccessKeys();
    //}

    ///**
    // * 创建阿里云账号，只能提供阿里云 ID
    // *
    // * @param aliyunId 阿里云 ID
    // * @return 阿里云 PK
    // */
    //public String createAccount(String aliyunId) throws ClientException {
    //    IAcsClient acsClient = acsClientFactory.getAasClient();
    //    CreateAliyunAccountRequest request = new CreateAliyunAccountRequest();
    //    request.setAliyunId(aliyunId);
    //    log.info("Call AAS CreateAliyunAccountRequest, request={}", gson.toJson(request));
    //    CreateAliyunAccountResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call AAS CreateAliyunAccountResponse, response={}", gson.toJson(response));
    //    return response.getPK();
    //}

    ///**
    // * 更新用户密码
    // *
    // * @param aliyunPk 需要更新的用户 Aliyun PK
    // * @param password 新密码
    // */
    //public void updatePassword(String aliyunPk, String password) throws AuthProxyThirdPartyError, ClientException {
    //    IAcsClient acsClient = acsClientFactory.getAasClient();
    //    UpdatePasswordForAccountRequest request = new UpdatePasswordForAccountRequest();
    //    request.setPK(aliyunPk);
    //    request.setNewPassword(password);
    //    log.info("Call AAS UpdatePasswordForAccount, request={}", gson.toJson(request));
    //    UpdatePasswordForAccountResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call AAS UpdatePasswordForAccount, response={}", gson.toJson(response));
    //    if (!response.getResult().equals("Success")) {
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS, response.getResult());
    //    }
    //}

    ///**
    // * 为指定阿里云用户新增 Access Key
    // *
    // * @param aliyunPk 阿里云 PK
    // * @return AccessKey
    // */
    //public CreateAccessKeyForAccountResponse.AccessKey createAccessKey(String aliyunPk) throws ClientException {
    //    IAcsClient acsClient = acsClientFactory.getAasClient();
    //    CreateAccessKeyForAccountRequest request = new CreateAccessKeyForAccountRequest();
    //    request.setPK(aliyunPk);
    //    log.info("Call AAS CreateAccessKeyForAccount, request={}", gson.toJson(request));
    //    CreateAccessKeyForAccountResponse response = acsClient.getAcsResponse(request);
    //    log.info("Call AAS CreateAccesskeyForAccount, response={}", gson.toJson(response));
    //    return response.getAccessKey();
    //}
    //
    ///**
    // * 为指定的阿里云用户删除所有的 Access Key
    // *
    // * @param aliyunPk 阿里云 PK
    // */
    //public void deleteAccessKey(String aliyunPk) throws ClientException, AuthProxyThirdPartyError {
    //    IAcsClient acsClient = acsClientFactory.getAasClient();
    //    List<AccessKey> accessKeys = getUserAccessKeys(aliyunPk);
    //    for (AccessKey accessKey : accessKeys) {
    //        DeleteAccessKeyForAccountRequest request = new DeleteAccessKeyForAccountRequest();
    //        request.setPK(aliyunPk);
    //        request.setAKId(accessKey.getAccessKeyId());
    //        log.info("Call AAS DeleteAccessKeyForAccount, request={}", gson.toJson(request));
    //        DeleteAccessKeyForAccountResponse response = acsClient.getAcsResponse(request);
    //        log.info("Call AAS DeleteAccesskeyForAccount, response={}", gson.toJson(response));
    //        if (!response.getResult().equals("Success")) {
    //            throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS, response.getResult());
    //        }
    //    }
    //}

}
