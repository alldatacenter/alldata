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
public class DagJobScheduleConfNode {

    private String id;

    private Long taskId;

    private JSONObject options;

}
