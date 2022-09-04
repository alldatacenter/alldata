package com.alibaba.sreworks.appdev.server.params;

import lombok.Data;

@Data
public class AppPipelineModifyParam {

    private String name;

    private String timeTrigger;

    private String authToken;

    private String description;

}
