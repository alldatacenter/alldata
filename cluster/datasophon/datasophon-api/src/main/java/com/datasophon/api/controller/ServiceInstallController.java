package com.datasophon.api.controller;

import com.alibaba.fastjson.JSONArray;
import com.datasophon.api.service.ServiceInstallService;
import com.datasophon.api.security.UserPermission;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleHostMapping;
import com.datasophon.common.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("service/install")
public class ServiceInstallController {
    @Autowired
    ServiceInstallService serviceInstallService;

    /**
     * 根据服务名称查询服务配置选项
     */
    @RequestMapping("/getServiceConfigOption")
    public Result getServiceConfigOption(Integer clusterId, String serviceName) {
        return serviceInstallService.getServiceConfigOption(clusterId, serviceName);
    }

    /**
     * 保存服务配置
     */
    @RequestMapping("/saveServiceConfig")
    @UserPermission
    public Result saveServiceConfig(Integer clusterId, String serviceName,String serviceConfig,Integer roleGroupId) {
        JSONArray jsonArray = JSONArray.parseArray(serviceConfig);
        List<ServiceConfig> list = jsonArray.toJavaList(ServiceConfig.class);
        return serviceInstallService.saveServiceConfig(clusterId,serviceName,list,roleGroupId);

    }

    /**
     * 保存服务角色与主机对应关系
     */
    @RequestMapping("/saveServiceRoleHostMapping/{clusterId}")
    public Result saveServiceRoleHostMapping( @RequestBody List<ServiceRoleHostMapping> list,@PathVariable("clusterId") Integer clusterId) {
        return serviceInstallService.saveServiceRoleHostMapping(clusterId, list);
    }

    /**
     * 查询服务角色与主机对应关系
     */
    @RequestMapping("/getServiceRoleHostMapping")
    @UserPermission
    public Result getServiceRoleHostMapping( Integer clusterId) {
        return serviceInstallService.getServiceRoleHostMapping(clusterId);
    }

    /**
     * 保存主机与服务角色对应关系
     */
    @RequestMapping("/saveHostServiceRoleMapping/{clusterId}")
    public Result saveHostServiceRoleMapping(@PathVariable("clusterId")Integer clusterId, @RequestBody List<HostServiceRoleMapping> list) {

        return serviceInstallService.saveHostServiceRoleMapping(clusterId, list);
    }
    /**
     * 服务部署总览
     */
    @RequestMapping("/getServiceRoleDeployOverview")
    public Result getServiceRoleDeployOverview(Integer clusterId) {
        return serviceInstallService.getServiceRoleDeployOverview(clusterId);
    }

    /**
     * 开始安装服务
     */
    @RequestMapping("/startInstallService/{clusterId}")
    public Result startInstallService(@PathVariable("clusterId")Integer clusterId,@RequestBody List<String> commandIds) {

        return serviceInstallService.startInstallService(clusterId,commandIds);
    }

    /**
     * 下载安装包
     */
    @GetMapping("/downloadPackage")
    public void downloadPackage(String packageName, String cpuArchitecture,HttpServletResponse response) throws IOException {

        serviceInstallService.downloadPackage(packageName,response);
    }


    /**
     * 服务部署总览
     */
    @RequestMapping("/checkServiceDependency")
    public Result checkServiceDependency(Integer clusterId,String serviceIds) {
        return serviceInstallService.checkServiceDependency(clusterId,serviceIds);
    }

}
