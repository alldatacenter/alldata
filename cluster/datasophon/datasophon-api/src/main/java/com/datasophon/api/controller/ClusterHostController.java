package com.datasophon.api.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.common.utils.Result;


/**
 * 集群主机表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-14 20:32:39
 */
@RestController
@RequestMapping("api/cluster/host")
public class ClusterHostController {
    @Autowired
    private ClusterHostService clusterHostService;

    /**
     * 查询集群所有主机
     */
    @RequestMapping("/all")
    public Result all(Integer clusterId) {
        List<ClusterHostEntity> list = clusterHostService.list(new QueryWrapper<ClusterHostEntity>().eq(Constants.CLUSTER_ID, clusterId)
                .eq(Constants.MANAGED, 1)
                .orderByAsc(Constants.HOSTNAME));
        return Result.success(list);
    }

    /**
     * 查询集群所有主机
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId, String hostname, String ip,String cpuArchitecture, Integer hostState, String orderField, String orderType, Integer page, Integer pageSize) {
        return clusterHostService.listByPage(clusterId, hostname, ip,cpuArchitecture, hostState, orderField, orderType, page, pageSize);

    }

    @RequestMapping("/getRoleListByHostname")
    public Result getRoleListByHostname(Integer clusterId, String hostname) {
        return clusterHostService.getRoleListByHostname(clusterId, hostname);

    }

    @RequestMapping("/getRack")
    public Result getRack(Integer clusterId) {
        return clusterHostService.getRack(clusterId);

    }

    @RequestMapping("/assignRack")
    public Result assignRack(Integer clusterId, String rack, String hostIds) {
        return clusterHostService.assignRack(clusterId, rack, hostIds);

    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        ClusterHostEntity clusterHost = clusterHostService.getById(id);

        return Result.success().put(Constants.DATA, clusterHost);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterHostEntity clusterHost) {
        clusterHostService.save(clusterHost);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterHostEntity clusterHost) {
        clusterHostService.updateById(clusterHost);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(Integer hostId) {

        return clusterHostService.deleteHost(hostId);

    }

}
