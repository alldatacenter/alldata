package com.alibaba.sreworks.teammanage.server.params;


import com.alibaba.sreworks.domain.DO.Team;
import com.alibaba.sreworks.teammanage.server.DTO.VisibleScope;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamCreateParam {

    private String name;

    private String description;

    private String avatar;

    private VisibleScope visibleScope;

    public Team toTeam(String operator) {
        return Team.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .name(name)
            .creator(operator)
            .lastModifier(operator)
            .description(description)
            .avatar(avatar)
            .visibleScope(visibleScope.toString())
            .build();
    }

}
