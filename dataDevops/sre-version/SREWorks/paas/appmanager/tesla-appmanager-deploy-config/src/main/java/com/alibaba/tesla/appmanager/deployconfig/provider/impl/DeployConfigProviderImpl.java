package com.alibaba.tesla.appmanager.deployconfig.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.DeployConfigProvider;
import com.alibaba.tesla.appmanager.deployconfig.assembly.DeployConfigDtoConvert;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.service.DeployConfigService;
import com.alibaba.tesla.appmanager.domain.dto.DeployConfigDTO;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigApplyTemplateReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigDeleteReq;
import com.alibaba.tesla.appmanager.domain.req.deployconfig.DeployConfigGenerateReq;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigApplyTemplateRes;
import com.alibaba.tesla.appmanager.domain.res.deployconfig.DeployConfigGenerateRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 部署配置 Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DeployConfigProviderImpl implements DeployConfigProvider {

    private final DeployConfigService deployConfigService;
    private final DeployConfigDtoConvert deployConfigDtoConvert;

    public DeployConfigProviderImpl(
            DeployConfigService deployConfigService, DeployConfigDtoConvert deployConfigDtoConvert) {
        this.deployConfigService = deployConfigService;
        this.deployConfigDtoConvert = deployConfigDtoConvert;
    }

    /**
     * 应用部署模板 (拆分 launch yaml 并分别应用保存)
     *
     * @param req 应用请求
     * @return 应用结果
     */
    @Override
    public DeployConfigApplyTemplateRes<DeployConfigDTO> applyTemplate(DeployConfigApplyTemplateReq req) {
        DeployConfigApplyTemplateRes<DeployConfigDO> res = deployConfigService.applyTemplate(req);
        return DeployConfigApplyTemplateRes.<DeployConfigDTO>builder()
                .items(res.getItems().stream()
                        .map(deployConfigDtoConvert::to)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 生成指定应用在指定部署参数下的 Application Configuration Yaml
     *
     * @param req 部署参数
     * @return 生成 Yaml 结果
     */
    @Override
    public DeployConfigGenerateRes generate(DeployConfigGenerateReq req) {
        return deployConfigService.generate(req);
    }

    /**
     * 删除指定 apiVersion + appId + typeId + envId 对应的 DeployConfig 记录
     *
     * @param req 删除请求
     */
    @Override
    public void delete(DeployConfigDeleteReq req) {
        deployConfigService.delete(req);
    }
}
