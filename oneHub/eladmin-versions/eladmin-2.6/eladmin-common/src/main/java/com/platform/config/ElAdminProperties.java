
package com.platform.config;

import lombok.Data;
import com.platform.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author AllDataDC
 * @description
 * @date 2022-10-27
 **/
@Data
@Component
public class ElAdminProperties {

    public static Boolean ipLocal;

    @Value("${ip.local-parsing}")
    public void setIpLocal(Boolean ipLocal) {
        ElAdminProperties.ipLocal = ipLocal;
    }
}
