package com.datasophon.api.controller;

import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterUserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;


@RestController
@RequestMapping("api/clusterusergroup")
public class ClusterUserGroupController {
    @Autowired
    private ClusterUserGroupService clusterUserGroupService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(){


        return Result.success();
    }




    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody ClusterUserGroup clusterUserGroup){
        clusterUserGroupService.save(clusterUserGroup);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody ClusterUserGroup clusterUserGroup){

        clusterUserGroupService.updateById(clusterUserGroup);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        clusterUserGroupService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
