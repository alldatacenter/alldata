package com.alibaba.sreworks.flyadmin.server.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.flyadmin.server.DTO.FlyadminAuthproxyCreateUserParam;
import com.alibaba.sreworks.flyadmin.server.DTO.FlyadminAuthproxyModifyUserParam;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserRoleService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
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
@RequestMapping("/flyadmin/authproxy")
@Api(tags = "authproxy")
public class FlyadminAuthproxyController extends BaseController {

    @Autowired
    FlyadminAuthproxyUserService flyadminAuthproxyUserService;

    @Autowired
    FlyadminAuthproxyUserRoleService flyadminAuthproxyUserRoleService;

    @Autowired
    AppRepository appRepository;

    @ApiOperation(value = "getUser")
    @RequestMapping(value = "getUser", method = RequestMethod.GET)
    public TeslaBaseResult getUser() throws IOException, ApiException {
        JSONObject ret = flyadminAuthproxyUserService.getUser(getUserEmployeeId());
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "users")
    @RequestMapping(value = "users", method = RequestMethod.GET)
    public TeslaBaseResult users() throws IOException, ApiException {
        List<JSONObject> ret = flyadminAuthproxyUserService.userList("");
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "createUser")
    @RequestMapping(value = "createUser", method = RequestMethod.POST)
    public TeslaBaseResult createUser(@RequestBody JSONObject postJson)
        throws IOException, ApiException {

        FlyadminAuthproxyCreateUserParam param = JSONObject.toJavaObject(
            postJson, FlyadminAuthproxyCreateUserParam.class);
        flyadminAuthproxyUserService.createUser(
            param.getNickName(),
            param.getLoginName(),
            param.getPassword(),
            param.getEmail(),
            param.getPhone(),
            param.getAvatar(),
            getUserEmployeeId()
        );
        flyadminAuthproxyUserRoleService.setRole(getBizAppId(), "empid::" + param.getLoginName(), postJson);
        return buildSucceedResult("OK");

    }

    @ApiOperation(value = "modifyUserMeta")
    @RequestMapping(value = "modifyUserMeta", method = RequestMethod.POST)
    public TeslaBaseResult modifyUserMeta(@RequestBody JSONObject postJson)
        throws IOException, ApiException {

        FlyadminAuthproxyModifyUserParam param = JSONObject.toJavaObject(
            postJson, FlyadminAuthproxyModifyUserParam.class);
        String ret = flyadminAuthproxyUserService.modifyUser(
            param.getNickName(),
            param.getLoginName(),
            param.getPassword(),
            param.getEmail(),
            param.getPhone(),
            param.getAvatar(),
            getUserEmployeeId()
        );
        return buildSucceedResult(ret);

    }

    @ApiOperation(value = "modifyUser")
    @RequestMapping(value = "modifyUser", method = RequestMethod.POST)
    public TeslaBaseResult modifyUser(String empid, @RequestBody JSONObject postJson)
        throws IOException, ApiException {

        flyadminAuthproxyUserRoleService.setRole(getBizAppId(), "empid::" + empid, postJson);
        return buildSucceedResult("OK");

    }

    @ApiOperation(value = "deleteUser")
    @RequestMapping(value = "deleteUser", method = RequestMethod.DELETE)
    public TeslaBaseResult deleteUser(Long id) throws IOException, ApiException {

        flyadminAuthproxyUserService.deleteUser(id, getUserEmployeeId());
        return buildSucceedResult("OK");

    }

    @ApiOperation(value = "listRoleSelector")
    @RequestMapping(value = "listRoleSelector", method = RequestMethod.GET)
    public TeslaBaseResult listRoleSelector(String user, String appId) throws IOException, ApiException {
        JSONArray jsonArray = flyadminAuthproxyUserRoleService.listRole(
            flyadminAuthproxyUserRoleService.authproxyAppId(appId, getBizAppId()), "empid::" + user
        );
        return buildSucceedResult(JsonUtil.map(
            "options", jsonArray.toJavaList(JSONObject.class).stream().map(userJson -> JsonUtil.map(
                "label", userJson.getString("roleId").split(":")[1],
                "value", userJson.getString("roleId")
            )).collect(Collectors.toList()),
            "initValue", jsonArray.toJavaList(JSONObject.class).stream()
                .filter(userJson -> userJson.getBooleanValue("exists"))
                .map(userJson -> userJson.getString("roleId"))
                .collect(Collectors.toList())
        ));
    }

}
