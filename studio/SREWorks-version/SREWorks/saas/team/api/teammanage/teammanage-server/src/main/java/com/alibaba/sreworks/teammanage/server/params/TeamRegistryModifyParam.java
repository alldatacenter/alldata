package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TeamRegistryModifyParam {

    private String name;

    private String url;

    private String auth;

    private String description;

    public void patchTeamRegistry(TeamRegistry teamRegistry, String operator) {
        teamRegistry.setGmtModified(System.currentTimeMillis() / 1000);
        teamRegistry.setLastModifier(operator);
        teamRegistry.setName(name);
        teamRegistry.setUrl(url);
        teamRegistry.setAuth(auth);
        teamRegistry.setDescription(description);
    }

}
