package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.Team;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamModifyParam {

    private String name;

    private String description;

    private String avatar;

    public void patchTeam(Team team, String operator) {
        team.setGmtModified(System.currentTimeMillis() / 1000);
        team.setLastModifier(operator);
        team.setName(name);
        team.setDescription(description);
        if (avatar != null) {
            team.setAvatar(avatar);
        }
    }

}
