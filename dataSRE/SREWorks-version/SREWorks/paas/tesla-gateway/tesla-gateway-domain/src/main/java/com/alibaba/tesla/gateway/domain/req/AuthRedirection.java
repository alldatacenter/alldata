package com.alibaba.tesla.gateway.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author qiuqiang.qq@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRedirection implements Serializable {
    private static final long serialVersionUID = 6150812165343493461L;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 默认重定向地址
     */
    private String defaultLocation;
}
