package com.alibaba.tesla.appmanager.domain.req.kubectl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 列出当前所有的 namespaces
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubectlListNamespaceReq implements Serializable {

    /**
     * 集群 ID，可为空，默认为 master
     */
    private String clusterId;
}
