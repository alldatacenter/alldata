package com.datasophon.api.service;

import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleHostMapping;
import com.datasophon.common.utils.Result;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface ServiceInstallService {
    Result getServiceConfigOption(Integer clusterId, String serviceName);

    Result saveServiceRoleHostMapping(Integer clusterId, List<ServiceRoleHostMapping> list);

    Result saveServiceConfig(Integer clusterId, String serviceName, List<ServiceConfig> configJson, Integer roleGroupId);

    Result saveHostServiceRoleMapping(Integer clusterId, List<HostServiceRoleMapping> list);

    Result getServiceRoleDeployOverview(Integer clusterId);

    Result startInstallService(Integer clusterId, List<String> commandIds);

    void downloadPackage(String packageName,HttpServletResponse response) throws IOException;

    Result getServiceRoleHostMapping(Integer clusterId);

    Result checkServiceDependency(Integer clusterId, String serviceIds);
}
