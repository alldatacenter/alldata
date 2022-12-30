package com.alibaba.sreworks.teammanage.server.controllers;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.RegularUtil;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;
import com.alibaba.sreworks.domain.repository.TeamRepoRepository;
import com.alibaba.sreworks.teammanage.server.params.TeamRepoCreateParam;
import com.alibaba.sreworks.teammanage.server.params.TeamRepoModifyParam;
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
@RequestMapping("/teamRepo")
@Api(tags = "团队-仓库")
public class TeamRepoController extends BaseController {

    @Autowired
    TeamRepoRepository teamRepoRepository;

    @Autowired
    TeamUserService teamUserService;

    @ApiOperation(value = "list")
    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long teamId) throws Exception {
        if (teamId == null) {
            throw new Exception("teamId is null");
        }
        List<JSONObject> ret = teamRepoRepository.findAllByTeamId(teamId)
            .stream()
            .map(TeamRepo::toJsonObject)
            .peek(x -> x.put("ciTokenStar", StarUtil.replaceString2Star(x.getString("ciToken"))))
            .collect(Collectors.toList());
        RegularUtil.underscoreToCamel(ret);
        RegularUtil.gmt2Date(ret);
        return buildSucceedResult(ret);
    }

    @ApiOperation(value = "get")
    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(Long id) {
        return buildSucceedResult(teamRepoRepository.findFirstById(id));
    }

    @ApiOperation(value = "create")
    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(Long teamId, @RequestBody TeamRepoCreateParam param) throws Exception {
        param.setTeamId(teamId);
        teamUserService.assertUserAdmin(param.getTeamId(), getUserEmployeeId());
        teamRepoRepository.saveAndFlush(param.toTeamRepo(getUserEmployeeId()));
        JSONObject result = new JSONObject();
        result.put("teamId", teamId);
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "delete")
    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws Exception {
        teamUserService.assertUserAdmin(teamRepoRepository.findFirstById(id).getTeamId(), getUserEmployeeId());
        JSONObject result = new JSONObject();
        result.put("teamId", teamRepoRepository.findFirstById(id).getTeamId());
        teamRepoRepository.deleteById(id);
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "modify")
    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(Long id, @RequestBody TeamRepoModifyParam param) throws Exception {
        teamUserService.assertUserAdmin(teamRepoRepository.findFirstById(id).getTeamId(), getUserEmployeeId());
        TeamRepo teamRepo = teamRepoRepository.findFirstById(id);
        param.patchTeamRepo(teamRepo, getUserEmployeeId());
        teamRepoRepository.saveAndFlush(teamRepo);
        JSONObject result = new JSONObject();
        result.put("teamId", teamRepo.getTeamId());
        result.put("result", "OK");
        return buildSucceedResult(result);
    }

    @ApiOperation(value = "idSelector")
    @RequestMapping(value = "idSelector", method = RequestMethod.GET)
    public TeslaBaseResult idSelector(Long teamId) {
        List<TeamRepo> teamRepoList = teamRepoRepository.findAllByTeamId(teamId);
        return buildSucceedResult(JsonUtil.map(
            "options", teamRepoList.stream().map(teamRepo -> JsonUtil.map(
                "label", teamRepo.getName(),
                "value", teamRepo.getId(),
                "url", teamRepo.getUrl()
            )).collect(Collectors.toList())
        ));
    }
}
