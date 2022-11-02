package com.alibaba.tesla.authproxy.service.impl;

import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateSmsSignatureForbidden;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.mapper.ConfigMapper;
import com.alibaba.tesla.authproxy.model.ConfigDO;
import com.alibaba.tesla.authproxy.service.PrivateSmsService;
import com.alibaba.tesla.authproxy.util.LocaleUtil;
import com.alibaba.tesla.authproxy.util.StringUtil;
import com.alibaba.tesla.authproxy.web.output.PrivateSmsConfigResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PrivateSmsServiceImpl implements PrivateSmsService {

    @Autowired
    private LocaleUtil locale;

    @Autowired
    private ConfigMapper configMapper;

    @Autowired
    private OkHttpClient okHttpClient;

    private Gson gson = new GsonBuilder().serializeNulls().create();

    /**
     * 短信网关注册
     *
     * @param endpoint Endpoint
     * @param token    Token
     */
    @Override
    public void register(String endpoint, String token) throws PrivateValidationError {
        if (StringUtil.isEmpty(endpoint) && StringUtil.isEmpty(token)) {
            ConfigDO endpointConfig = ConfigDO.builder().name(Constants.CONFIG_PRIVATE_SMS_ENDPOINT).value("").build();
            ConfigDO tokenConfig = ConfigDO.builder().name(Constants.CONFIG_PRIVATE_SMS_TOKEN).value("").build();
            configMapper.save(endpointConfig);
            configMapper.save(tokenConfig);
            return;
        }

        if (StringUtil.isEmpty(endpoint) || StringUtil.isEmpty(token)) {
            throw new PrivateValidationError("token", locale.msg("private.sms.register_invalid_combination"));
        }
        ConfigDO endpointConfig = ConfigDO.builder()
                .name(Constants.CONFIG_PRIVATE_SMS_ENDPOINT)
                .value(endpoint)
                .build();
        ConfigDO tokenConfig = ConfigDO.builder()
                .name(Constants.CONFIG_PRIVATE_SMS_TOKEN)
                .value(token)
                .build();
        configMapper.save(endpointConfig);
        configMapper.save(tokenConfig);
    }

    /**
     * 获取当前的 endpoint / token 配置
     *
     * @return PrivateSmsConfigResult 对象
     */
    @Override
    public PrivateSmsConfigResult getConfig() {
        ConfigDO endpointConfig = configMapper.getByName(Constants.CONFIG_PRIVATE_SMS_ENDPOINT);
        ConfigDO tokenConfig = configMapper.getByName(Constants.CONFIG_PRIVATE_SMS_TOKEN);
        if (endpointConfig == null || tokenConfig == null) {
            return PrivateSmsConfigResult.builder().endpoint("").token("").build();
        }
        return PrivateSmsConfigResult.builder()
                .endpoint(endpointConfig.getValue())
                .token(tokenConfig.getValue())
                .build();
    }

    /**
     * 向指定 phone 发送指定 content
     * @param phone 手机号码
     * @param content 内容
     */
    @Override
    public void sendMessage(String phone, String aliyunId, String code, String content) throws
        AuthProxyThirdPartyError {
        ConfigDO endpointConfig = configMapper.getByName(Constants.CONFIG_PRIVATE_SMS_ENDPOINT);
        ConfigDO tokenConfig = configMapper.getByName(Constants.CONFIG_PRIVATE_SMS_TOKEN);
        if (endpointConfig == null || tokenConfig == null) {
            log.info("No sms gateway configured, skip");
            return;
        }

        String endpoint = endpointConfig.getValue();
        String token = tokenConfig.getValue();
        Long timestamp = getTimestamp();
        Integer nonce = getNonce();
        String signature = buildSignature(token, String.valueOf(timestamp), String.valueOf(nonce));

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(endpoint)).newBuilder();
        String jsonBody = gson.toJson(SmsContent.builder()
                .phone(phone)
                .aliyunId(aliyunId)
                .code(code)
                .content(content)
                .signature(signature)
                .timestamp(timestamp)
                .nonce(nonce)
                .timestamp(timestamp)
                .build());
        try {
            postRequest(urlBuilder.build(), jsonBody);
        } catch (Exception e) {
            throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_SMS, locale.msg("private.sms.check_failed",
                    e.getMessage()));
        }
    }

    /**
     * 检查 endpoint 和 token 是否合法
     *
     * @param endpoint Endpoint
     * @param token    Token
     * @throws AuthProxyThirdPartyError       当第三方系统出错时抛出
     * @throws PrivateSmsSignatureForbidden 当签名不合法时抛出
     */
    public void checkEndpoint(String endpoint, String token) throws AuthProxyThirdPartyError,
            PrivateSmsSignatureForbidden, PrivateValidationError {
        String timestamp = String.valueOf(getTimestamp());
        String nonce = String.valueOf(getNonce());
        String signature = buildSignature(token, timestamp, nonce);

        HttpUrl url = HttpUrl.parse(endpoint);
        if (url == null) {
            throw new PrivateValidationError("endpoint", locale.msg("private.sms.endpoint_not_url"));
        }

        //  进行正确签名检查
        HttpUrl.Builder urlBuilder = url.newBuilder();
        urlBuilder.addQueryParameter("timestamp", timestamp);
        urlBuilder.addQueryParameter("nonce", nonce);
        urlBuilder.addQueryParameter("signature", signature);
        try {
            getRequest(urlBuilder.build());
        } catch (IOException e) {
            throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_SMS, locale.msg("private.sms.check_failed",
                    e.getMessage()));
        }

        // 进行错误签名检查
        urlBuilder = url.newBuilder();
        urlBuilder.addQueryParameter("timestamp", String.valueOf(getTimestamp()));
        urlBuilder.addQueryParameter("nonce", String.valueOf(getNonce()));
        urlBuilder.addQueryParameter("signature", signature);
        try {
            getRequest(urlBuilder.build());
        } catch (IOException e) {
            throw new AuthProxyThirdPartyError(Constants.THIRD_PARTY_SMS, locale.msg("private.sms.check_failed",
                    e.getMessage()));
        } catch (PrivateSmsSignatureForbidden e) {
            return;
        }
        throw new PrivateValidationError("endpoint", locale.msg("private.sms.register_check_invalid_signature_failed"));
    }

    /**
     * 根据 token / timestamp / nonce 来构造对应的签名
     *
     * @param token     Token
     * @param timestamp Timestamp UNIX 时间戳
     * @param nonce     Nonce
     * @return 计算出来的签名
     */
    private String buildSignature(String token, String timestamp, String nonce) {
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder arrStr = new StringBuilder();
        for (String item : arr) {
            arrStr.append(item);
        }
        return DigestUtils.sha1Hex(arrStr.toString());
    }

    /**
     * 获取当前系统时间的 UNIX Timestamp 形式
     *
     * @return long
     */
    private long getTimestamp() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取一个随机数 nonce
     *
     * @return 位于 10000 ~ 99999 之间的数字
     */
    private Integer getNonce() {
        return ThreadLocalRandom.current().nextInt(10000, 100000);
    }

    /**
     * 向指定 URL 发送 POST 数据
     *
     * @param url  URL 地址
     * @param body 表单数据 JSON
     * @return 响应内容
     */
    private void postRequest(HttpUrl url, String body) throws IOException, PrivateSmsSignatureForbidden {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), body);
        request(new Request.Builder().url(url).post(requestBody).build());
    }

    /**
     * 向指定 URL 发送 GET 数据
     *
     * @param url URL 地址
     *            * @return 响应内容
     */
    private void getRequest(HttpUrl url) throws IOException, PrivateSmsSignatureForbidden {
        request(new Request.Builder().url(url).get().build());
    }

    /**
     * 发送 request 请求
     */
    private void request(Request request) throws IOException, PrivateSmsSignatureForbidden {
        Response response;
        response = okHttpClient.newCall(request).execute();
        int responseCode = response.code();
        log.debug("[SmsService] request={}", request.toString());
        response.close();

        // 检查返回结果并进行处理
        if (responseCode == 403) {
            throw new PrivateSmsSignatureForbidden();
        } else if (responseCode != 200) {
            throw new IOException(String.format("Illegal response from third-party sms service server (%d)",
                    responseCode));
        }
    }

}


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SmsContent {
    private String phone;
    private String aliyunId;
    private String code;
    private String content;
    private String signature;
    private Long timestamp;
    private Integer nonce;
}