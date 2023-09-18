package com.datasophon.api.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceCommandHostCommandEntity;
import com.datasophon.api.service.ClusterServiceCommandHostCommandService;
import com.datasophon.common.utils.Result;


/**
 * 集群服务操作指令主机指令表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-12 11:28:06
 */
@RestController
@RequestMapping("api/cluster/service/command/host/command")
public class ClusterServiceCommandHostCommandController {
    @Autowired
    private ClusterServiceCommandHostCommandService clusterServiceCommandHostCommandService;


    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(String hostname, String commandHostId, Integer page, Integer pageSize) {

        return clusterServiceCommandHostCommandService.getHostCommandList(hostname, commandHostId, page, pageSize);
    }

    @RequestMapping("/getHostCommandLog")
    public Result getHostCommandLog(Integer clusterId, String hostCommandId) throws Exception {
        return clusterServiceCommandHostCommandService.getHostCommandLog(clusterId, hostCommandId);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterServiceCommandHostCommandEntity clusterServiceCommandHostCommand = clusterServiceCommandHostCommandService.getById(id);

        return Result.success().put("clusterServiceCommandHostCommand", clusterServiceCommandHostCommand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceCommandHostCommandEntity clusterServiceCommandHostCommand) {
        clusterServiceCommandHostCommandService.save(clusterServiceCommandHostCommand);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceCommandHostCommandEntity clusterServiceCommandHostCommand) {
        clusterServiceCommandHostCommandService.updateById(clusterServiceCommandHostCommand);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        clusterServiceCommandHostCommandService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
