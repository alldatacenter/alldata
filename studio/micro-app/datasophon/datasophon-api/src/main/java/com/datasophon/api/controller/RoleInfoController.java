package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.Map;

import com.datasophon.api.service.RoleInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.RoleInfoEntity;
import com.datasophon.common.utils.Result;



/**
 * 角色信息表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("/api/role/info")
public class RoleInfoController {
    @Autowired
    private RoleInfoService roleInfoService;

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
        RoleInfoEntity roleInfo = roleInfoService.getById(id);

        return Result.success().put("roleInfo", roleInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody RoleInfoEntity roleInfo){
        roleInfoService.save(roleInfo);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody RoleInfoEntity roleInfo){
        roleInfoService.updateById(roleInfo);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        roleInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
