package com.alibaba.sreworks.job.master.jobtrigger.quartz;

import com.alibaba.sreworks.job.master.jobtrigger.AbstractJobTriggerConf;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuartzJobTriggerConf extends AbstractJobTriggerConf {

    private String cronExpression;

    private boolean enabled;

}
