package com.alibaba.sreworks.appdev.server.params;

import lombok.Data;

@Data
public class AppPipelineCreateParam {

    private String name;

    private String timeTrigger;

    private String authToken;

    private String description;

    private Long appId;

    private Long teamRegistryId;

    private Long teamRepoId;


}
