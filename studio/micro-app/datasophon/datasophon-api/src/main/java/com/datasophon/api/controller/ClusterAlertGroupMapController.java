package com.datasophon.api.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterAlertGroupMap;
import com.datasophon.api.service.ClusterAlertGroupMapService;

/**
 * 
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-07-13 10:01:06
 */
@RestController
@RequestMapping("cluster/alert/group/map")
public class ClusterAlertGroupMapController {
    @Autowired
    private ClusterAlertGroupMapService clusterAlertGroupMapService;

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
        ClusterAlertGroupMap clusterAlertGroupMap = clusterAlertGroupMapService.getById(id);

        return Result.success().put("clusterAlertGroupMap", clusterAlertGroupMap);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterAlertGroupMap clusterAlertGroupMap){
        clusterAlertGroupMapService.save(clusterAlertGroupMap);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterAlertGroupMap clusterAlertGroupMap){

        clusterAlertGroupMapService.updateById(clusterAlertGroupMap);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterAlertGroupMapService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
