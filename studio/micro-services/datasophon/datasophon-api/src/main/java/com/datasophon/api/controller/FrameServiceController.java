package com.datasophon.api.controller;

import java.util.Arrays;
import java.util.List;

import com.datasophon.api.service.FrameServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datasophon.dao.entity.FrameServiceEntity;
import com.datasophon.common.utils.Result;


/**
 * 集群框架版本服务表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:36:08
 */
@RestController
@RequestMapping("api/frame/service")
public class FrameServiceController {

    @Autowired
    private FrameServiceService frameVersionServiceService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public Result list(Integer clusterId) {
        return frameVersionServiceService.getAllFrameService(clusterId);
    }

    /**
     * 根据servce id列表查询服务
     */
    @RequestMapping("/getServiceListByServiceIds")
    public Result getServiceListByServiceIds(List<Integer> serviceIds) {
        return frameVersionServiceService.getServiceListByServiceIds(serviceIds);
    }

    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public Result info(@PathVariable("id") Integer id) {
        FrameServiceEntity frameVersionService = frameVersionServiceService.getById(id);

        return Result.success().put("frameVersionService", frameVersionService);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public Result save(@RequestBody FrameServiceEntity frameVersionService) {
        frameVersionServiceService.save(frameVersionService);

        return Result.success();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public Result update(@RequestBody FrameServiceEntity frameVersionService) {
        frameVersionServiceService.updateById(frameVersionService);

        return Result.success();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public Result delete(@RequestBody Integer[] ids) {
        frameVersionServiceService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
