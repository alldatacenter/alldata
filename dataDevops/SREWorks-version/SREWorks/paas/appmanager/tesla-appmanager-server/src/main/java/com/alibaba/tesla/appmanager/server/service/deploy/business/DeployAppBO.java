package com.alibaba.tesla.appmanager.server.service.deploy.business;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 部署工单 App - 业务对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppBO {

    /**
     * 工单元信息
     */
    private DeployAppDO order;

    /**
     * 扩展信息
     */
    private Map<String, String> attrMap;
}
