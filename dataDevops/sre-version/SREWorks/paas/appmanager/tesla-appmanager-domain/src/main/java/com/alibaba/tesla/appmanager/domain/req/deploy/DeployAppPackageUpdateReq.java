package com.alibaba.tesla.appmanager.domain.req.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 更新指定 AppPackage 的部署单中的数据
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppPackageUpdateReq implements Serializable {

    private static final long serialVersionUID = -4194268250142616424L;

    private Long deployAppId;

    private Long appPackageId;

    private String deployAppConfiguration;
}
