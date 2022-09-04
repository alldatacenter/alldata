package com.alibaba.tesla.authproxy.core;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyException;
import com.alibaba.tesla.authproxy.util.HttpUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云 RSA Keys 容器
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
@Slf4j
public class AliyunRsaKeyContainer {

    private static final String KEYS_URL = "https://oauth.aliyun.com/v1/keys";

    private List<RSAKey> keys;

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private AuthProperties authProperties;

    public AliyunRsaKeyContainer() {
        this.keys = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        if (!Constants.ENVIRONMENT_OXS.equals(authProperties.getEnvironment())) {
            log.info("Not OXS environment, skip aliyun rsa key container initialization");
            return;
        }

        // 从阿里云接口处获取对应的 RSA Keys
        Request request = new Request.Builder().url(KEYS_URL).build();
        try {
            String response = HttpUtil.makeHttpCall(httpClient, request);
            JsonObject responseJson = new JsonParser().parse(response).getAsJsonObject();
            for (JsonElement key : responseJson.get("keys").getAsJsonArray()) {
                keys.add(RSAKey.parse(key.toString()));
            }
        } catch (Exception e) {
            log.error("Cannot init aliyun rsa keys, exception={}", ExceptionUtils.getStackTrace(e));
        }
    }

    /**
     * 获取当前系统中的 rsa keys
     *
     * @return
     */
    public List<RSAKey> getKeys() {
        if (keys.size() == 0) {
            throw new AuthProxyException("Aliyun rsa key not ready, please wait for a momment...");
        }
        return keys;
    }
}
