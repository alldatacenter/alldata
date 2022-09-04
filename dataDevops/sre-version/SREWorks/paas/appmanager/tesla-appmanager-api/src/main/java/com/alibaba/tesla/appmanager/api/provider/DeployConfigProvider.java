package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.dto.DeployConfigDTO;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigApplyTemplateReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigGenerateReq;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigApplyTemplateRes;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigGenerateRes;

/**
 * 部署配置 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployConfigProvider {

    /**
     * 应用部署模板 (拆分 launch yaml 并分别应用保存)
     *
     * @param req 应用请求
     * @return 应用结果
     */
    DeployConfigApplyTemplateRes<DeployConfigDTO> applyTemplate(DeployConfigApplyTemplateReq req);

    /**
     * 生成指定应用在指定部署参数下的 Application Configuration Yaml
     *
     * @param req 部署参数
     * @return 生成 Yaml 结果
     */
    DeployConfigGenerateRes generate(DeployConfigGenerateReq req);

    /**
     * 删除指定 apiVersion + appId + typeId + envId 对应的 DeployConfig 记录
     *
     * @param req 删除请求
     */
    void delete(DeployConfigDeleteReq req);
}
