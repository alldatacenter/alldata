package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.sreworks.domain.DO.AppCicd;

import lombok.Data;

@Data
public class AppCicdCreateParam {

    private String name;

    private Long teamId;

    private String authToken;

    private String timerTrigger;

    private String scmTrigger;

    private String script;

    private String description;

    public AppCicd toAppCicd(String operator) {
        return AppCicd.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .creator(operator)
            .lastModifier(operator)
            .teamId(teamId)
            .name(name)
            .authToken(authToken)
            .timerTrigger(timerTrigger)
            .scmTrigger(scmTrigger)
            .description(description)
            .script(script)
            .build();
    }

}
