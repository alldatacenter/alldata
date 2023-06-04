package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.util.EnumUtil;
import com.datasophon.api.enums.Status;
import com.datasophon.api.service.ClusterServiceCommandService;
import com.datasophon.api.security.UserPermission;
import com.datasophon.common.enums.CommandType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceCommandEntity;
import com.datasophon.common.utils.Result;


/**
 * 集群服务操作指令表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@RestController
@RequestMapping("api/cluster/service/command")
public class ClusterServiceCommandController {
    @Autowired
    private ClusterServiceCommandService clusterServiceCommandService;

    /**
     * 查询集群服务指令列表
     */
    @RequestMapping("/getServiceCommandlist")
    public Result list(Integer clusterId, Integer page, Integer pageSize) {
        return clusterServiceCommandService.getServiceCommandlist(clusterId, page, pageSize);
    }


    /**
     * 生成服务安装操作指令
     */
    @UserPermission
    @RequestMapping("/generateCommand")
    public Result generateCommand(Integer clusterId, String commandType, String serviceNames) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        List<String> list = Arrays.asList(serviceNames.split(","));
        return clusterServiceCommandService.generateCommand(clusterId, command, list);
    }

    /**
     * 生成服务实例操作指令
     */
    @RequestMapping("/generateServiceCommand")
    @UserPermission
    public Result generateServiceCommand(Integer clusterId, String commandType, String serviceInstanceIds) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        if(StringUtils.isNotBlank(serviceInstanceIds)){
            List<String> ids = Arrays.asList(serviceInstanceIds.split(","));
            return clusterServiceCommandService.generateServiceCommand(clusterId, command, ids);
        }else {
            return Result.error(Status.NO_SERVICE_EXECUTE.getMsg());
        }


    }

    /**
     * 生成服务角色实例操作指令
     */
    @RequestMapping("/generateServiceRoleCommand")
    @UserPermission
    public Result generateServiceRoleCommand(Integer clusterId, String commandType, Integer serviceInstanceId, String serviceRoleInstancesIds) {
        CommandType command = EnumUtil.fromString(CommandType.class, commandType);
        List<String> ids = Arrays.asList(serviceRoleInstancesIds.split(","));
        return clusterServiceCommandService.generateServiceRoleCommand(clusterId, command, serviceInstanceId, ids);

    }

    /**
     * 启动执行指令
     */
    @RequestMapping("/startExecuteCommand")
    @UserPermission
    public Result startExecuteCommand(Integer clusterId, String commandType, String commandIds) {
        clusterServiceCommandService.startExecuteCommand(clusterId,commandType,commandIds);
        return Result.success();
    }

    @RequestMapping("/cancelCommand")
    public Result cancelCommand(String commandId) {
        clusterServiceCommandService.cancelCommand(commandId);
        return Result.success();
    }



    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterServiceCommandEntity clusterServiceCommand = clusterServiceCommandService.getById(id);

        return Result.success().put("clusterServiceCommand", clusterServiceCommand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceCommandEntity clusterServiceCommand) {
        clusterServiceCommandService.save(clusterServiceCommand);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceCommandEntity clusterServiceCommand) {
        clusterServiceCommandService.updateById(clusterServiceCommand);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        clusterServiceCommandService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }


}
