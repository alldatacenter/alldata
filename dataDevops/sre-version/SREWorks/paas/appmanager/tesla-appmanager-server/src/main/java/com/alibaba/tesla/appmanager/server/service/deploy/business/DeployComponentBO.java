package com.alibaba.tesla.appmanager.server.service.deploy.business;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 部署工单 Component - 业务对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployComponentBO {

    /**
     * Component 子工单元信息
     */
    private DeployComponentDO subOrder;

    /**
     * 扩展信息
     */
    private Map<String, String> attrMap;
}
