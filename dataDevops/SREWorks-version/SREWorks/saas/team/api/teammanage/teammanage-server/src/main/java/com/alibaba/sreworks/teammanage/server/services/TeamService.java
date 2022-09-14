package com.alibaba.sreworks.teammanage.server.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.TeamAccount;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;
import com.alibaba.sreworks.domain.repository.TeamAccountRepository;
import com.alibaba.sreworks.domain.repository.TeamRegistryRepository;
import com.alibaba.sreworks.domain.repository.TeamRepoRepository;
import com.alibaba.sreworks.teammanage.server.controllers.TeamRepoController;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Slf4j
@Data
public class TeamService {

    @Autowired
    TeamRepoRepository teamRepoRepository;

    @Autowired
    TeamRegistryRepository teamRegistryRepository;

    @Autowired
    TeamAccountRepository teamAccountRepository;

    public void initTeam(Long teamId, String operator) {
        String teamDefaultRepo = System.getenv("TEAM_DEFAULT_REPO");
        String teamDefaultRegistry = System.getenv("TEAM_DEFAULT_REGISTRY");
        String teamDefaultAccount = System.getenv("TEAM_DEFAULT_ACCOUNT");

        if (!StringUtils.isEmpty(teamDefaultRepo)) {
            String repo = new String(Base64.getDecoder().decode(teamDefaultRepo));
            TeamRepo teamRepo = JSONObject.parseObject(repo, TeamRepo.class);
            teamRepo.setGmtCreate(System.currentTimeMillis() / 1000);
            teamRepo.setGmtModified(System.currentTimeMillis() / 1000);
            teamRepo.setCreator(operator);
            teamRepo.setLastModifier(operator);
            teamRepo.setTeamId(teamId);
            teamRepoRepository.saveAndFlush(teamRepo);
        }

        if (!StringUtils.isEmpty(teamDefaultRegistry)) {
            String registry = new String(Base64.getDecoder().decode(teamDefaultRegistry));
            TeamRegistry teamRegistry = JSONObject.parseObject(registry, TeamRegistry.class);
            teamRegistry.setGmtCreate(System.currentTimeMillis() / 1000);
            teamRegistry.setGmtModified(System.currentTimeMillis() / 1000);
            teamRegistry.setCreator(operator);
            teamRegistry.setLastModifier(operator);
            teamRegistry.setTeamId(teamId);
            teamRegistryRepository.saveAndFlush(teamRegistry);
        }

        if (!StringUtils.isEmpty(teamDefaultAccount)) {
            String account = new String(Base64.getDecoder().decode(teamDefaultAccount));
            TeamAccount teamAccount = JSONObject.parseObject(account, TeamAccount.class);
            teamAccount.setGmtCreate(System.currentTimeMillis() / 1000);
            teamAccount.setGmtModified(System.currentTimeMillis() / 1000);
            teamAccount.setCreator(operator);
            teamAccount.setLastModifier(operator);
            teamAccount.setTeamId(teamId);
            teamAccountRepository.saveAndFlush(teamAccount);
        }

    }

}
