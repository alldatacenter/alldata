package com.alibaba.tesla.authproxy.outbound.acs;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACS 客户端生成器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class AcsClientFactory {

    //private final AuthProperties authProperties;
    //
    //private final LocaleUtil locale;
    //
    ///**
    // * 全局唯一的 ak 客户端
    // */
    //private final AkClient akClient;
    //
    ///**
    // * 根据 aliyun pk + bid 来存储不同用户对应的 IAcsClient (OAM)
    // */
    //private final ConcurrentHashMap<String, IAcsClient> oamAcsClient;
    //
    ///**
    // * 全局唯一的 AAS 客户端
    // */
    //private final IAcsClient aasAcsClient;
    //
    ///**
    // * 全局唯一的超级客户端
    // */
    //private final IAcsClient superClient;
    //
    ///**
    // * 全局唯一的 aliyunid client
    // */
    //private final AliyunidClient aliyunidClient;
    //
    ///**
    // * 全局唯一的 Access Key 读取器
    // */
    //private final AccessKeyReader accessKeyReader;
    //
    //@Autowired
    //public AcsClientFactory(AuthProperties authProperties, LocaleUtil locale) throws ClientException {
    //    String endpointName = authProperties.getPopEndpointName();
    //    String regionId = authProperties.getPopRegionId();
    //    String popOamDomain = authProperties.getPopOamDomain();
    //    String popAasDomain = authProperties.getPopAasDomain();
    //    String popAasKey = authProperties.getPopAasKey();
    //    String popAasSecret = authProperties.getPopAasSecret();
    //    String superKey = authProperties.getAasKey();
    //    String superSecret = authProperties.getAasSecret();
    //
    //    this.authProperties = authProperties;
    //    this.locale = locale;
    //
    //    // ak 客户端初始化
    //    this.akClient = new AkClient();
    //    String aasGetAkUrl = authProperties.getAasGetAkUrl();
    //    String aasKey = authProperties.getAasKey();
    //    String aasSecret = authProperties.getAasSecret();
    //    if (!StringUtil.isEmpty(aasGetAkUrl) && !StringUtil.isEmpty(aasKey) && !StringUtil.isEmpty(aasSecret)) {
    //        akClient.setBaseUrl(aasGetAkUrl);
    //        akClient.setKey(aasKey);
    //        akClient.setSecret(aasSecret);
    //    }
    //
    //    // OAM 客户端对应内容初始化
    //    this.oamAcsClient = new ConcurrentHashMap<>();
    //    DefaultProfile.addEndpoint(endpointName, regionId, "Oam", popOamDomain);
    //
    //    // AAS 客户端初始化
    //    DefaultProfile.addEndpoint(endpointName, regionId, "Aas", popAasDomain);
    //
    //    // 生成 IAcsClient
    //    IClientProfile profile = DefaultProfile.getProfile(regionId, popAasKey, popAasSecret);
    //    log.info("Get aas profile successful, regionId={}, key={}, secret={}", regionId, popAasKey, popAasSecret);
    //    this.aasAcsClient = new DefaultAcsClient(profile);
    //    log.info("AAS acs client has generated");
    //
    //    // 生成超级客户端
    //    IClientProfile superProfile = DefaultProfile.getProfile(regionId, superKey, superSecret);
    //    log.info("Get super profile successful, regionId={}, key={}, secret={}", regionId, superKey, superSecret);
    //    this.superClient = new DefaultAcsClient(superProfile);
    //    log.info("Super acs client has generated");
    //
    //    // 生成 Access Key 读取器
    //    this.accessKeyReader = new AccessKeyReader();
    //    this.accessKeyReader.setAkClient(this.akClient);
    //
    //    // 生成 aliyun id client
    //    try {
    //        this.aliyunidClient = new AliyunidClient(authProperties.getAasKey(), authProperties.getAasSecret());
    //        this.aliyunidClient.setTimeoutInMilliSeconds(5000);
    //    } catch (OAuthException e) {
    //        throw new ClientException(e);
    //    }
    //}
    //
    ///**
    // * 根据 aliyun pk 和 bid 来获取对应的 IAcsClient
    // *
    // * @param aliyunPk AliyunPK
    // * @param bid      BID
    // * @return IAcsClient 客户端
    // */
    //public IAcsClient getOamClient(String aliyunPk, String bid) throws AuthProxyThirdPartyError {
    //    String key = aliyunPk + bid;
    //    if (oamAcsClient.containsKey(key)) {
    //        return oamAcsClient.get(key);
    //    }
    //
    //    // 获取用户 AK 列表
    //    List<AccessKeyInfo> userAks;
    //    try {
    //        userAks = akClient.getUserAccessKeyList(
    //            Long.valueOf(aliyunPk), bid,
    //            AccessKeyStatus.ENABLED, AccessKeyType.SYMMETRIC
    //        );
    //    } catch (IOException e) {
    //        log.warn("Get user access key failed, aliyunPk={}, bid={}, message={}", aliyunPk, bid, e.getMessage());
    //        AuthProxyThirdPartyError se = new AuthProxyThirdPartyError(Constants.THIRD_PARTY_ACS, e.getMessage());
    //        se.initCause(e);
    //        throw se;
    //    }
    //
    //    // 取出 userKey 和 userSecret
    //    String userKey, userSecret;
    //    if (null != userAks && userAks.size() > 0) {
    //        userKey = userAks.get(0).getKey();
    //        userSecret = userAks.get(0).getSecret();
    //        log.info("Get user access key successful, userAks={}", JSONObject.toJSONString(userAks));
    //    } else {
    //        log.warn("Get empty user access key, aliyunPk={}, bid={}", aliyunPk, bid);
    //        throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_AAS,
    //            locale.msg("private.acs.emptyUserAk", aliyunPk));
    //    }
    //
    //    // 生成 IAcsClient
    //    IClientProfile profile = DefaultProfile.getProfile(authProperties.getPopRegionId(), userKey, userSecret);
    //    log.info("Get profile successful, regionId={}, userKey={}, userSecret={}",
    //        authProperties.getPopRegionId(), userKey, userSecret);
    //    IAcsClient acsClient = new DefaultAcsClient(profile);
    //    oamAcsClient.putIfAbsent(key, acsClient);
    //    log.info("ACS client with aliyunPk={},bid={} has generated", aliyunPk, bid);
    //    return acsClient;
    //}
    //
    ///**
    // * 返回当前的 AAS Acs Client
    // *
    // * @return IAcsClient 客户端
    // */
    //public IAcsClient getAasClient() {
    //    return aasAcsClient;
    //}
    //
    ///**
    // * 返回当前的 Super Acs Client
    // *
    // * @return IAcsClient 客户端
    // */
    //public IAcsClient getSuperClient() {
    //    return superClient;
    //}
    //
    ///**
    // * 返回当前的 Access Key Reader
    // *
    // * @return AccessKeyReader
    // */
    //public AccessKeyReader getAccessKeyReader() {
    //    return accessKeyReader;
    //}
    //
    ///**
    // * 返回当前的 ak Client
    // *
    // * @return ak 客户端
    // */
    //public AkClient getAkClient() {
    //    return akClient;
    //}
    //
    ///**
    // * 返回当前的 aliyunid client
    // * @return aliyunid client
    // */
    //public AliyunidClient getAliyunidClient() {
    //    return aliyunidClient;
    //}
}
