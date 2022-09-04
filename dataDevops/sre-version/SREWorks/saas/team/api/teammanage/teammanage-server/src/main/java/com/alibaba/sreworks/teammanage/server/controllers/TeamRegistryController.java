package com.alibaba.sreworks.teammanage.server.controllers;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.repository.TeamRegistryRepository;
import com.alibaba.sreworks.teammanage.server.params.TeamRegistryCreateParam;
import com.alibaba.sreworks.teammanage.server.params.TeamRegistryModifyParam;
import com.alibaba.sreworks.teammanage.server.services.TeamRegistryService;
import com.alibaba.sreworks.teammanage.server.services.TeamUserService;
import com.alibaba.sreworks.teammanage.server.utils.StarUtil;
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
@RequestMapping("/teamRegistry")
@Api(tags = "团队-镜像源")
public class TeamRegistryController extends BaseController {

    @Autowired
    TeamRegistryRepository teamRegistryRepository;

    @Autowired
    TeamUserService teamUserService;

    @Autowired
    TeamRegistryService teamRegistryService;

    @ApiOperation(value = "list")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long teamId) throws Exception {
        if (teamId == null) {
            throw new Exception("teamId is null");
        }
        List<JSONObject> ret = teamRegistryRepository.findAllByTeamId(teamId)
            .stream()
            .map(TeamRegistry::toJsonObject)
            .peek(x -> x.put("authStar", StarUtil.replaceString2Star(x.getString("auth"))))
            .collect(Collectors.toList());
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        return buildSucceedResult(teamRegistryRepository.findFirstById(id));
    }

    @ApiOperation(value = "create")
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(Long teamId, @RequestBody TeamRegistryCreateParam param) throws Exception {
        param.setTeamId(teamId);
        teamUserService.assertUserAdmin(param.getTeamId(), getUserEmployeeId());
        TeamRegistry teamRegistry = teamRegistryRepository.saveAndFlush(param.toTeamRegistry(getUserEmployeeId()));
        teamRegistryService.createSecret(teamRegistry);
        JSONObject result = new JSONObject();
        result.put("teamId", teamId);
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "delete")
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws Exception {
        TeamRegistry teamRegistry = teamRegistryRepository.findFirstById(id);
        teamUserService.assertUserAdmin(teamRegistry.getTeamId(), getUserEmployeeId());
        teamRegistryService.deleteSecret(teamRegistry);
        teamRegistryRepository.deleteById(id);
        JSONObject result = new JSONObject();
        result.put("teamId", teamRegistry.getTeamId());
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "modify")
    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(Long id, @RequestBody TeamRegistryModifyParam param) throws Exception {
        teamUserService.assertUserAdmin(teamRegistryRepository.findFirstById(id).getTeamId(), getUserEmployeeId());
        TeamRegistry teamRegistry = teamRegistryRepository.findFirstById(id);
        param.patchTeamRegistry(teamRegistry, getUserEmployeeId());
        teamRegistryRepository.saveAndFlush(teamRegistry);
        JSONObject result = new JSONObject();
        result.put("teamId", teamRegistry.getTeamId());
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "idSelector")
    @RequestMapping(value = "idSelector", method = RequestMethod.GET)
    public TeslaBaseResult idSelector(Long teamId) {
        List<TeamRegistry> teamRegistryList = teamRegistryRepository.findAllByTeamId(teamId);
        return buildSucceedResult(JsonUtil.map(
            "options", teamRegistryList.stream().map(teamRegistry -> JsonUtil.map(
                "label", teamRegistry.getName(),
                "value", teamRegistry.getId(),
                "url", teamRegistry.getUrl()
            )).collect(Collectors.toList())
        ));
    }
}
