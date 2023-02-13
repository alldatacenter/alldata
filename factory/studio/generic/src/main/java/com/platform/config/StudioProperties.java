
package com.platform.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author AllDataDC
 * @description
 * @date 2023-01-27
 **/
@Data
@Component
public class StudioProperties {

    public static Boolean ipLocal;

    @Value("${ip.local-parsing}")
    public void setIpLocal(Boolean ipLocal) {
        StudioProperties.ipLocal = ipLocal;
    }
}
