package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 查询指定 AppPackage 的部署单运行状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppGetReq implements Serializable {

    private static final long serialVersionUID = 3256022989002584168L;

    /**
     * 部署单 ID
     */
    private Long deployAppId;
}
