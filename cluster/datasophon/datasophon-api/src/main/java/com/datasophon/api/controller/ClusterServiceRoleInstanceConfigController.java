package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.Map;

import com.datasophon.api.service.ClusterServiceRoleInstanceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.ClusterServiceRoleInstanceConfigEntity;
import com.datasophon.common.utils.Result;



/**
 * 集群服务角色实例配置表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/clusterserviceroleinstanceconfig")
public class ClusterServiceRoleInstanceConfigController {
    @Autowired
    private ClusterServiceRoleInstanceConfigService clusterServiceRoleInstanceConfigService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(@RequestParam Map<String, Object> params){


        return Result.success();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig = clusterServiceRoleInstanceConfigService.getById(id);

        return Result.success().put("clusterServiceRoleInstanceConfig", clusterServiceRoleInstanceConfig);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig){
        clusterServiceRoleInstanceConfigService.save(clusterServiceRoleInstanceConfig);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceRoleInstanceConfigEntity clusterServiceRoleInstanceConfig){
        clusterServiceRoleInstanceConfigService.updateById(clusterServiceRoleInstanceConfig);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterServiceRoleInstanceConfigService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
