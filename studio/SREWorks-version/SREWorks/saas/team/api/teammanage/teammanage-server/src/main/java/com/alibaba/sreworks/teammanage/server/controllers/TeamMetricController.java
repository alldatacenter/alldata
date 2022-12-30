package com.alibaba.sreworks.teammanage.server.controllers;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.DO.Team;
import com.alibaba.sreworks.domain.DO.TeamUser;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.domain.repository.TeamRepository;
import com.alibaba.sreworks.domain.repository.TeamUserRepository;
import com.alibaba.tesla.web.controller.BaseController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/")
public class TeamMetricController extends BaseController {

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    TeamUserRepository teamUserRepository;

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @GetMapping("/")
    public JSONObject metric() {
        JSONObject ret = new JSONObject();
        List<Team> teamList = teamRepository.findAll();
        List<TeamUser> teamUserList = teamUserRepository.findAll();
        List<Cluster> clusterList = clusterRepository.findAll();
        List<AppInstance> appInstanceList = appInstanceRepository.findAll();
        for (Team team : teamList) {
            ret.put(
                "user_count_" + team.getId(),
                teamUserList.stream().filter(teamUser -> teamUser.getTeamId().equals(team.getId())).count()
            );
            ret.put(
                "app_instance_count_" + team.getId(),
                appInstanceList.stream().filter(appInstance -> appInstance.getTeamId().equals(team.getId())).count()
            );
            ret.put(
                "cluster_count_" + team.getId(),
                clusterList.stream().filter(cluster -> cluster.getTeamId().equals(team.getId())).count()
            );
        }
        return ret;
    }

}
