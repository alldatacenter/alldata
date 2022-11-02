package com.alibaba.sreworks.appdev.server.controllers;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appdev.server.params.AppCicdCreateParam;
import com.alibaba.sreworks.appdev.server.params.AppCicdModifyParam;
//import com.alibaba.sreworks.common.util.JenkinsUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.DO.AppCicd;
import com.alibaba.sreworks.domain.repository.AppCicdRepository;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/appdev/appCicd")
@Api(tags = "应用cicd")
public class AppCicdController extends BaseController {

    @Autowired
    AppCicdRepository appCicdRepository;

//    private JenkinsUtil jenkinsUtil() {
//        return new JenkinsUtil("http://dev-app-dev-jenkins:8080/", "admin:admin");
//    }

//    @ApiOperation(value = "create")
//    @RequestMapping(value = "create", method = RequestMethod.POST)
//    public TeslaBaseResult create(@RequestBody AppCicdCreateParam param) {
//
//        AppCicd appCicd = appCicdRepository.saveAndFlush(param.toAppCicd(getUserEmployeeId()));
//        jenkinsUtil().createJob(
//            param.getName(), param.getTimerTrigger(), param.getAuthToken(), param.getDescription(), param.getScript()
//        );
//        return buildSucceedResult(appCicd);
//
//    }

    @ApiOperation(value = "modify")
    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(Long id, @RequestBody AppCicdModifyParam param) {
        AppCicd appCicd = appCicdRepository.findFirstById(id);
        param.patchAppCicd(appCicd, getUserEmployeeId());
        appCicdRepository.saveAndFlush(appCicd);
        return buildSucceedResult(appCicd);
    }

//    @ApiOperation(value = "delete")
//    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
//    public TeslaBaseResult delete(Long id) {
//        jenkinsUtil().deleteJob(appCicdRepository.findFirstById(id).getName());
//        appCicdRepository.deleteById(id);
//        return buildSucceedResult("OK");
//    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        JSONObject appCicd = appCicdRepository.findFirstById(id).toJsonObject();
        RegularUtil.gmt2Date(appCicd);
        return buildSucceedResult(appCicd);
    }

    @ApiOperation(value = "list")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list() {
        List<JSONObject> ret = appCicdRepository.findObjectByUser(getUserEmployeeId());
        RegularUtil.gmt2Date(ret);
        RegularUtil.underscoreToCamel(ret);
        return buildSucceedResult(ret);
    }

}
