package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterServiceRoleInstanceWebuisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceWebuis;

/**
 * 集群服务角色对应web ui表 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-06-30 09:35:40
 */
@RestController
@RequestMapping("cluster/webuis")
public class ClusterServiceRoleInstanceWebuisController {
    @Autowired
    private ClusterServiceRoleInstanceWebuisService clusterServiceRoleInstanceWebuisService;

    /**
     * 列表
     */
    @RequestMapping("/getWebUis")
    public Result getWebUis(Integer serviceInstanceId){

        return clusterServiceRoleInstanceWebuisService.getWebUis(serviceInstanceId);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceRoleInstanceWebuis clusterServiceRoleInstanceWebuis = clusterServiceRoleInstanceWebuisService.getById(id);

        return Result.success().put("clusterServiceRoleInstanceWebuis", clusterServiceRoleInstanceWebuis);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceRoleInstanceWebuis clusterServiceRoleInstanceWebuis){
        clusterServiceRoleInstanceWebuisService.save(clusterServiceRoleInstanceWebuis);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceRoleInstanceWebuis clusterServiceRoleInstanceWebuis){

        clusterServiceRoleInstanceWebuisService.updateById(clusterServiceRoleInstanceWebuis);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterServiceRoleInstanceWebuisService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
