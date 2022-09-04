package com.alibaba.sreworks.teammanage.server.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.Team;
import com.alibaba.sreworks.domain.DO.TeamUser;
import com.alibaba.sreworks.domain.DTO.TeamUserRole;
import com.alibaba.sreworks.domain.repository.TeamRepository;
import com.alibaba.sreworks.domain.repository.TeamUserRepository;
import com.alibaba.sreworks.teammanage.server.DTO.Role;
import com.alibaba.sreworks.teammanage.server.DTO.VisibleScope;
import com.alibaba.sreworks.teammanage.server.params.TeamCreateParam;
import com.alibaba.sreworks.teammanage.server.params.TeamModifyParam;
import com.alibaba.sreworks.teammanage.server.params.TeamSetVisibleScopeParam;
import com.alibaba.sreworks.teammanage.server.services.TeamService;
import com.alibaba.sreworks.teammanage.server.services.TeamUserService;
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
@RequestMapping("/team")
@Api(tags = "团队")
public class TeamController extends BaseController {

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamUserRepository teamUserRepository;

    @Autowired
    TeamUserService teamUserService;

    @Autowired
    TeamService teamService;

    @ApiOperation(value = "公开团队")
    @RequestMapping(value = "listPublic", method = RequestMethod.GET)
    public TeslaBaseResult listPublic(String name) {
        name = StringUtil.isEmpty(name) ? "" : name;
        List<JSONObject> ret = teamRepository.findObjectByVisibleScopeAndNameLikeOrderByIdDesc(
            VisibleScope.PUBLIC.name(), "%" + name + "%");
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "创建")
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(@RequestBody TeamCreateParam param) {
        Team team = param.toTeam(getUserEmployeeId());
        teamRepository.saveAndFlush(team);
        TeamUser teamUser = new TeamUser(team.getId(), getUserEmployeeId());
        teamUser.setRole(TeamUserRole.ADMIN.name());
        teamUserRepository.saveAndFlush(teamUser);
        teamService.initTeam(team.getId(), getUserEmployeeId());
        JSONObject result = new JSONObject();
        result.put("teamId", team.getId());
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "删除")
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws Exception {
        teamUserService.assertUserAdmin(id, getUserEmployeeId());
        teamRepository.deleteById(id);
        JSONObject result = new JSONObject();
        result.put("teamId", id);
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "修改")
    @RequestMapping(value = "modify", method = RequestMethod.PUT)
    public TeslaBaseResult modify(Long id, @RequestBody TeamModifyParam param) throws Exception {
        teamUserService.assertUserAdmin(id, getUserEmployeeId());
        Team team = teamRepository.findFirstById(id);
        param.patchTeam(team, getUserEmployeeId());
        teamRepository.saveAndFlush(team);
        JSONObject result = new JSONObject();
        result.put("teamId", id);
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "更新可见范围")
    @RequestMapping(value = "setVisibleScope", method = RequestMethod.PUT)
    public TeslaBaseResult setVisibleScope(Long id, @RequestBody TeamSetVisibleScopeParam param) throws Exception {
        teamUserService.assertUserAdmin(id, getUserEmployeeId());
        Team team = teamRepository.findFirstById(id);
        param.patchTeam(team, getUserEmployeeId());
        teamRepository.saveAndFlush(team);
        return buildSucceedResult("OK");
    }

    @ApiOperation(value = "详情")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        return buildSucceedResult(
            teamRepository.findFirstById(id)
        );
    }

    @ApiOperation(value = "visibleScopeSelector")
    @RequestMapping(value = "visibleScopeSelector", method = RequestMethod.GET)
    public TeslaBaseResult visibleScopeSelector() {
        return buildSucceedResult(JsonUtil.map(
            "options", Arrays.stream(VisibleScope.values()).map(visibleScope -> JsonUtil.map(
                "label", visibleScope.getCn(),
                "value", visibleScope.name()
            )).collect(Collectors.toList())
        ));
    }

    @ApiOperation(value = "roleSelector")
    @RequestMapping(value = "roleSelector", method = RequestMethod.GET)
    public TeslaBaseResult RoleSelector() {
        return buildSucceedResult(JsonUtil.map(
            "options", Arrays.stream(Role.values()).map(role -> JsonUtil.map(
                "label", role.getCn(),
                "value", role.name()
            )).collect(Collectors.toList())
        ));
    }
}
