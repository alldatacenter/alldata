package com.alibaba.tdata.aisp.server.controller.result;

import java.util.List;

import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: TaskQueryResult
 * @Author: dyj
 * @DATE: 2021-11-18
 * @Description:
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryResult {
    private Long total;
    private List<TaskDO> items;
}
