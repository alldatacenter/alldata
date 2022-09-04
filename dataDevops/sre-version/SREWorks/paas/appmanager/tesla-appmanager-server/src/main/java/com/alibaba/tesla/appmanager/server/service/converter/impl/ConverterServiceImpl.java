package com.alibaba.tesla.appmanager.server.service.converter.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.req.converter.AddParametersToLaunchReq;
import com.alibaba.tesla.appmanager.domain.res.converter.AddParametersToLaunchRes;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import com.alibaba.tesla.appmanager.server.service.converter.ConverterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 转换器服务实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class ConverterServiceImpl implements ConverterService {

    /**
     * 向 launch YAML 中附加全局参数
     *
     * @param request 请求内容
     * @return 返回数据
     */
    @Override
    public AddParametersToLaunchRes addParametersToLaunch(AddParametersToLaunchReq request) {
        DeployAppSchema schema = SchemaUtil.toSchema(DeployAppSchema.class, request.getLaunchYaml());
        if (schema.getSpec() == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid launch yaml, no spec found");
        }
        if (CollectionUtils.isEmpty(schema.getSpec().getParameterValues())) {
            schema.getSpec().setParameterValues(new ArrayList<>());
        }
        request.getParameterValues().forEach(item ->
                schema.getSpec().getParameterValues().add(DeployAppSchema.ParameterValue.builder()
                        .name(item.getName())
                        .value(item.getValue())
                        .build()));
        DeployAppSchema.MetaDataAnnotations annotations = new DeployAppSchema.MetaDataAnnotations();
        annotations.setNamespaceId(request.getNamespaceId());
        annotations.setClusterId(request.getClusterId());
        annotations.setStageId(request.getStageId());
        annotations.setAppId(request.getAppId());
        annotations.setAppInstanceName(request.getAppInstanceName());
        DeployAppSchema.MetaData metadata = new DeployAppSchema.MetaData();
        metadata.setName("deploy-app-package");
        metadata.setAnnotations(annotations);
        schema.setMetadata(metadata);
        return AddParametersToLaunchRes.builder()
                .launchYaml(SchemaUtil.toYamlMapStr(schema))
                .build();
    }
}
