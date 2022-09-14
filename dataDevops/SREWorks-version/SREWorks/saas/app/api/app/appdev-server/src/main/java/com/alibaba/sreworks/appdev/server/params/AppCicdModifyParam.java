package com.alibaba.sreworks.appdev.server.params;

import com.alibaba.sreworks.domain.DO.AppCicd;

import lombok.Data;

@Data
public class AppCicdModifyParam {

    private String name;

    private String authToken;

    private String timeTrigger;

    private String scmTrigger;

    private String script;

    private String description;

    public void patchAppCicd(AppCicd appCicd, String operator) {
        appCicd.setGmtModified(System.currentTimeMillis() / 1000);
        appCicd.setLastModifier(operator);
        appCicd.setName(name);
        appCicd.setAuthToken(authToken);
        appCicd.setTimerTrigger(timeTrigger);
        appCicd.setScmTrigger(scmTrigger);
        appCicd.setScript(script);
        appCicd.setDescription(description);
    }

}
