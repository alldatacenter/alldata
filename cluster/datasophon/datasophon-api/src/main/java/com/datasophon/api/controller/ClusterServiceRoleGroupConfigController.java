package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.ClusterServiceRoleGroupConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterServiceRoleGroupConfig;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-08-16 16:56:01
 */
@RestController
@RequestMapping("cluster/service/role/group/config")
public class ClusterServiceRoleGroupConfigController {
    @Autowired
    private ClusterServiceRoleGroupConfigService clusterServiceRoleGroupConfigService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(){


        return Result.success();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig = clusterServiceRoleGroupConfigService.getById(id);

        return Result.success().put("clusterServiceRoleGroupConfig", clusterServiceRoleGroupConfig);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig){
        clusterServiceRoleGroupConfigService.save(clusterServiceRoleGroupConfig);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig){

        clusterServiceRoleGroupConfigService.updateById(clusterServiceRoleGroupConfig);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterServiceRoleGroupConfigService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
