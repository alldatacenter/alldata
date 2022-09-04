package com.alibaba.tesla.appmanager.domain.req;

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
public class AddonInstanceRelQueryReq implements Serializable {
    private static final long serialVersionUID = -8892742506275176066L;

    private String namespaceId;

    private String envId;

    private String componentType;

    private String componentName;

    private String appId;
}
