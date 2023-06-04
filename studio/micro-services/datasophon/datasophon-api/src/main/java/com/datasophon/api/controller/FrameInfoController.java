package com.datasophon.api.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.api.service.FrameInfoService;
import com.datasophon.common.utils.Result;



/**
 * 集群框架表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/frame")
public class FrameInfoController {
    @Autowired
    private FrameInfoService frameInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(){
        return frameInfoService.getAllClusterFrame();
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id){
        FrameInfoEntity frameInfo = frameInfoService.getById(id);

        return Result.success().put("frameInfo", frameInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody FrameInfoEntity frameInfo){
        frameInfoService.save(frameInfo);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody FrameInfoEntity frameInfo){
        frameInfoService.updateById(frameInfo);
        
        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids){
        frameInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
