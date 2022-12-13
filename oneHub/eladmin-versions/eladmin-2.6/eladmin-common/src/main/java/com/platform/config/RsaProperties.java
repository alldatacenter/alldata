
package com.platform.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author AllDataDC
 * @website https://eladmin.vip
 * @description
 * @date 2022-10-27
 **/
@Data
@Component
public class RsaProperties {

    public static String privateKey;

    @Value("${rsa.private_key}")
    public void setPrivateKey(String privateKey) {
        RsaProperties.privateKey = privateKey;
    }
}