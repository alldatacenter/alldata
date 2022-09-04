package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DO.Team;
import com.alibaba.sreworks.teammanage.server.DTO.VisibleScope;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamSetVisibleScopeParam {

    private VisibleScope visibleScope;

    public void patchTeam(Team team, String operator) {
        team.setGmtModified(System.currentTimeMillis() / 1000);
        team.setLastModifier(operator);
        team.setVisibleScope(visibleScope.name());
    }

}
