package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 重试指定 AppPackage 的部署单
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppRetryReq implements Serializable {

    private static final long serialVersionUID = 3256022989002584168L;

    private Long deployAppId;
}
