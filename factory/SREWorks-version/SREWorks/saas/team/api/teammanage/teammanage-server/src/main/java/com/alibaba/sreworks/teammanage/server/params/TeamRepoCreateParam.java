package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.TeamRepo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TeamRepoCreateParam {

    private Long teamId;

    private String name;

    private String url;

    private String ciAccount;

    private String ciToken;

    private String description;

    public TeamRepo toTeamRepo(String operator) {
        return TeamRepo.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .name(name)
            .url(url)
            .ciAccount(ciAccount)
            .ciToken(ciToken)
            .description(description)
            .build();
    }

}
