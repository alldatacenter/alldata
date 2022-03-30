package com.platform.devops.controller;

import com.platform.devops.entity.Application;
import com.platform.devops.entity.Cluster;
import com.platform.devops.entity.Project;
import com.platform.devops.service.ProjectService;
import com.platform.mall.entity.Result;
import com.platform.mall.utils.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author wlhbdp
 * @desc Devops--项目管理Controller
 */
@RestController
@Api(description = "项目管理")
public class ProjectController {

    @Autowired
    private ProjectService projectService;


    @RequestMapping(value = "/getProjectList", method = RequestMethod.GET)
    @ApiOperation(value = "获取项目列表")
    public Result<Object> getProjectList() {

        List<Project> projects = projectService.getProjectList();
        return new ResultUtil<Object>().setData(projects);
    }

    @RequestMapping(value = "/getAppList", method = RequestMethod.GET)
    @ApiOperation(value = "获取项目列表")
    public Result<Object> getAppList() {

        List<Application> applications = projectService.getAppList();
        return new ResultUtil<Object>().setData(applications);
    }


    @RequestMapping(value = "/getCluster", method = RequestMethod.GET)
    @ApiOperation(value = "获取项目列表")
    public Result<Object> getCluster() {

        Cluster cluster = projectService.getCluster();
        return new ResultUtil<Object>().setData(cluster);
    }

}
