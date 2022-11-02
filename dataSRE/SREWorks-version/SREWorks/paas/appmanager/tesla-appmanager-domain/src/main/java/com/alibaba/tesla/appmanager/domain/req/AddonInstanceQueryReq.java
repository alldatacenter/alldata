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
public class AddonInstanceQueryReq implements Serializable {
    private static final long serialVersionUID = 2701332248815871881L;

    private String namespaceId;

    private String addonId;

    private String addonVersion;

    private String addonInstanceId;
}
