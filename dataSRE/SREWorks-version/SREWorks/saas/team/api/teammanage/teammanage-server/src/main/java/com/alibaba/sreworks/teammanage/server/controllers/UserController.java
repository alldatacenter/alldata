package com.alibaba.sreworks.teammanage.server.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.TeamUser;
import com.alibaba.sreworks.domain.repository.TeamUserRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAuthproxyUserService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "人员")
public class UserController extends BaseController {

    @Autowired
    FlyadminAuthproxyUserService flyadminAuthproxyUserService;

    @Autowired
    TeamUserRepository teamUserRepository;

    @ApiOperation(value = "selector")
    @RequestMapping(value = "selector", method = RequestMethod.GET)
    public TeslaBaseResult selector(Long teamId) throws IOException, ApiException {
        List<String> teamUserList = teamUserRepository.findAllByTeamId(teamId).stream()
            .map(TeamUser::getUser).collect(Collectors.toList());
        List<JSONObject> ret = flyadminAuthproxyUserService.userList(getUserEmployeeId()).stream()
            .filter(x -> !teamUserList.contains(x.getString("empId"))).collect(Collectors.toList());
        return buildSucceedResult(JsonUtil.map(
            "options", ret.stream().map(userJson -> JsonUtil.map(
                "label", userJson.getString("nickName"),
                "value", userJson.getString("empId")
            )).collect(Collectors.toList())
        ));
    }

}
