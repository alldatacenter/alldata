package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagJobScheduleConfEdge {

    private String id;

    private String source;

    private String target;

    private String expression;

    private JSONObject options;

}
