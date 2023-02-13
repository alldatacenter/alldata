package com.alibaba.sreworks.job.master.params;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ListenTaskInstanceStatusParam {

    private Long taskInstanceId;

    private String status;

}
