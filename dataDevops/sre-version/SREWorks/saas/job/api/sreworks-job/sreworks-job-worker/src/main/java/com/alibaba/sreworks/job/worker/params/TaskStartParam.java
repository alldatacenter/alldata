package com.alibaba.sreworks.job.worker.params;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStartParam {

    private Long id;

    private Long taskId;

    private Long execTimeout;

    private String execType;

    private String execContent;

    private JSONObject varConf;

}
