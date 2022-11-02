package com.alibaba.tesla.appmanager.domain.req.deploy;

import java.util.List;

import com.alibaba.tesla.appmanager.domain.dto.EnvMetaDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询指定 AppPackage 的部署单运行状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppBuildAppConfigReq {
    private Long appPackageId;

    private String stageId;

    private List<EnvMetaDTO> envMetaDTOList;

    private List<ComponentBinder> componentList;
}
