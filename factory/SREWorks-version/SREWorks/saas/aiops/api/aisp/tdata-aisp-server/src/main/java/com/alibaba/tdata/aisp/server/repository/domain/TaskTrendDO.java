package com.alibaba.tdata.aisp.server.repository.domain;

import java.util.Date;

import lombok.Data;

/**
 * @ClassName: TaskTrendDO
 * @Author: dyj
 * @DATE: 2021-12-06
 * @Description:
 **/
@Data
public class TaskTrendDO {
    private Long count;

    private Date time;
}
