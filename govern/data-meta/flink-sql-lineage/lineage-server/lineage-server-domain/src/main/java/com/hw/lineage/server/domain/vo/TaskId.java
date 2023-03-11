package com.hw.lineage.server.domain.vo;

import com.hw.lineage.server.domain.repository.basic.Identifier;
import lombok.Data;

/**
 * @description: TaskId
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class TaskId implements Identifier {

    private final Long value;

    public TaskId(Long value) {
        this.value = value;
    }
}
