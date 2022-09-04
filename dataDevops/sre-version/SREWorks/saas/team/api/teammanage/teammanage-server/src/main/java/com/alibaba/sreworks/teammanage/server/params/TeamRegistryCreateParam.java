package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TeamRegistryCreateParam {

    private Long teamId;

    private String name;

    private String url;

    private String auth;

    private String description;

    public TeamRegistry toTeamRegistry(String operator) {
        return TeamRegistry.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .name(name)
            .url(url)
            .auth(auth)
            .description(description)
            .build();
    }

}
