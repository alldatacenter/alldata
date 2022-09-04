package com.alibaba.tesla.appmanager.domain.req.kubectl;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Kubectl Apply 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubectlApplyReq implements Serializable {

    /**
     * 目标集群
     */
    private String clusterId;

    /**
     * 目标 Namespace
     */
    private String namespaceId;

    /**
     * 请求内容
     */
    private JSONObject content;
}
