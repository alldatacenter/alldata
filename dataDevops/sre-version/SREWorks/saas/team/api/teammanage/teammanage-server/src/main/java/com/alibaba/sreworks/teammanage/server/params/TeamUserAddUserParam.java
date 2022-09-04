package com.alibaba.sreworks.teammanage.server.params;

import com.alibaba.sreworks.domain.DTO.TeamUserRole;

import lombok.Data;

/**
 * @author jinghua.yjh
 */
@Data
public class TeamUserAddUserParam {

    private String user;

    private TeamUserRole role;

}
