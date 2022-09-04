package com.alibaba.sreworks.job.taskhandler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class MysqlContent {

    private Long datasourceId;

    private String sql;

}
