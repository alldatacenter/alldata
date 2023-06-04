package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.utils.Result;


/**
 * 集群服务角色实例表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@RestController
@RequestMapping("cluster/service/role/instance")
public class ClusterServiceRoleInstanceController {
    @Autowired
    private ClusterServiceRoleInstanceService clusterServiceRoleInstanceService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer serviceInstanceId,String hostname,Integer serviceRoleState, String serviceRoleName,Integer roleGroupId,Integer page, Integer pageSize) {
        return clusterServiceRoleInstanceService.listAll(serviceInstanceId,hostname,serviceRoleState,serviceRoleName,roleGroupId,page,pageSize);
    }


    /**
     * 信息
     */
    @RequestMapping("/getLog")
    public Result getLog(Integer serviceRoleInstanceId) throws Exception {
        return clusterServiceRoleInstanceService.getLog(serviceRoleInstanceId);
    }

    /**
     * 退役
     */
    @RequestMapping("/decommissionNode")
    public Result decommissionNode(String serviceRoleInstanceIds,String serviceName) throws Exception {
        return clusterServiceRoleInstanceService.decommissionNode(serviceRoleInstanceIds,serviceName);
    }

    /**
     * 重启过时服务
     */
    @RequestMapping("/restartObsoleteService")
    public Result restartObsoleteService(Integer roleGroupId) throws Exception {
        return clusterServiceRoleInstanceService.restartObsoleteService(roleGroupId);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceRoleInstanceEntity clusterServiceRoleInstance) {
        clusterServiceRoleInstanceService.save(clusterServiceRoleInstance);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceRoleInstanceEntity clusterServiceRoleInstance) {
        clusterServiceRoleInstanceService.updateById(clusterServiceRoleInstance);
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(String serviceRoleInstancesIds) {
        List<String> idList = Arrays.asList(serviceRoleInstancesIds.split(","));
        return clusterServiceRoleInstanceService.deleteServiceRole(idList);
    }

}
