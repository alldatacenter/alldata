package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.sreworks.job.master.jobschedule.AbstractJobScheduleConf;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagJobScheduleConf extends AbstractJobScheduleConf {

    private List<DagJobScheduleConfNode> nodes;

    private List<DagJobScheduleConfEdge> edges;

}

