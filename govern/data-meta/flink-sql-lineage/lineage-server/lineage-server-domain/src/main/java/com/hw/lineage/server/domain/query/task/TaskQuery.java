package com.hw.lineage.server.domain.query.task;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import lombok.Data;

/**
 * @description: TaskQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class TaskQuery extends PageOrderCriteria {

    private String taskName;
}
