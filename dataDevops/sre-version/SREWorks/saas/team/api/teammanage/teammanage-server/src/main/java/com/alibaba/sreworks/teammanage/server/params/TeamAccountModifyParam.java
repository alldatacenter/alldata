package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.domain.DO.TeamAccount;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamAccountModifyParam {

    private String type;

    private String name;

    private Long teamId;

    private JSONObject detail;

    private String description;

    public void patchTeamAccount(TeamAccount teamAccount, String operator) {
        teamAccount.setGmtModified(System.currentTimeMillis() / 1000);
        teamAccount.setLastModifier(operator);
        teamAccount.setType(type);
        teamAccount.setName(name);
        teamAccount.setDetail(JSONObject.toJSONString(detail));
        teamAccount.setDescription(description);
    }

}
