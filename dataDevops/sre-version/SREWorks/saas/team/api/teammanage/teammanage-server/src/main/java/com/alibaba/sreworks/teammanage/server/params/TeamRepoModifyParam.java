package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.TeamRepo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TeamRepoModifyParam {

    private String name;

    private String url;

    private String ciAccount;

    private String ciToken;

    private String description;

    public void patchTeamRepo(TeamRepo teamRepo, String operator) {
        teamRepo.setGmtModified(System.currentTimeMillis() / 1000);
        teamRepo.setLastModifier(operator);
        teamRepo.setName(name);
        teamRepo.setUrl(url);
        teamRepo.setCiAccount(ciAccount);
        teamRepo.setCiToken(ciToken);
        teamRepo.setDescription(description);
    }

}
