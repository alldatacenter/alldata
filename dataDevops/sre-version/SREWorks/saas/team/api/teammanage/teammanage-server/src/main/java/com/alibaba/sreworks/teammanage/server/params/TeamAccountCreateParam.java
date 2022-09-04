package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.TeamAccount;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamAccountCreateParam {

    private String type;

    private String name;

    private JSONObject detail;

    private String description;

    public TeamAccount toTeamAccount(Long teamId, String operator) {
        return TeamAccount.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .type(type)
            .name(name)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .detail(JSONObject.toJSONString(detail))
            .description(description)
            .build();
    }

}
