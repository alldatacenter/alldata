package com.alibaba.sreworks.job.master.jobschedule.serial;

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
public class SerialJobScheduleConf extends AbstractJobScheduleConf {

    private List<Long> taskIdList;

}

