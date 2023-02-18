package com.datasophon.api.controller;

import java.util.Arrays;

import com.datasophon.api.service.AlertGroupService;
import com.datasophon.api.service.FrameServiceRoleService;
import com.datasophon.dao.entity.AlertGroupEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.FrameServiceRoleEntity;
import com.datasophon.common.utils.Result;


/**
 * 框架服务角色表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-18 14:38:53
 */
@RestController
@RequestMapping("api/frame/service/role")
public class FrameServiceRoleController {
    @Autowired
    private FrameServiceRoleService frameServiceRoleService;

    @Autowired
    private AlertGroupService alertGroupService;

    /**
     * 查询服务对应的角色列表
     */
    @RequestMapping("/getServiceRoleList")
    public Result getServiceRoleOfMaster(Integer clusterId,String serviceIds,Integer serviceRoleType){
        return frameServiceRoleService.getServiceRoleList(clusterId,serviceIds,serviceRoleType);
    }

    @RequestMapping("/getNonMasterRoleList")
    public Result getNonMasterRoleList(Integer clusterId,String serviceIds){
        return frameServiceRoleService.getNonMasterRoleList(clusterId,serviceIds);
    }

    @RequestMapping("/getServiceRoleByServiceName")
    public Result getServiceRoleByServiceName(Integer clusterId,Integer alertGroupId){
        AlertGroupEntity alertGroupEntity = alertGroupService.getById(alertGroupId);
        return frameServiceRoleService.getServiceRoleByServiceName(clusterId,alertGroupEntity.getAlertGroupCategory());
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        FrameServiceRoleEntity frameServiceRole = frameServiceRoleService.getById(id);

        return Result.success().put("frameServiceRole", frameServiceRole);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody FrameServiceRoleEntity frameServiceRole){
        frameServiceRoleService.save(frameServiceRole);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody FrameServiceRoleEntity frameServiceRole){
        frameServiceRoleService.updateById(frameServiceRole);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        frameServiceRoleService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
