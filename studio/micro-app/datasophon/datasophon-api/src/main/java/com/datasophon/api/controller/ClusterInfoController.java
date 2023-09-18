package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.security.UserPermission;
import com.datasophon.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.common.utils.Result;


/**
 * 集群信息表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/cluster")
public class ClusterInfoController {
    @Autowired
    private ClusterInfoService clusterInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list() {
        return clusterInfoService.getClusterList();
    }
    /**
     * 配置好的集群列表
     */
    @RequestMapping("/runningClusterList")
    public Result runningClusterList() {
        return clusterInfoService.runningClusterList();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(id);

        return Result.success().put(Constants.DATA, clusterInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @UserPermission
    public Result save(@RequestBody ClusterInfoEntity clusterInfo)  {
        return clusterInfoService.saveCluster(clusterInfo);
    }

    @RequestMapping("/updateClusterState")
    public Result updateClusterState(Integer clusterId,Integer clusterState) {

        return clusterInfoService.updateClusterState(clusterId,clusterState);
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @UserPermission
    public Result update(@RequestBody ClusterInfoEntity clusterInfo) {
        return clusterInfoService.updateCluster(clusterInfo);

    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @UserPermission
    public Result delete(@RequestBody Integer[] ids) {
        clusterInfoService.deleteCluster(Arrays.asList(ids));

        return Result.success();
    }

}
