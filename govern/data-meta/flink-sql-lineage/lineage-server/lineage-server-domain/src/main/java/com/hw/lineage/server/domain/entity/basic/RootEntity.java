package com.hw.lineage.server.domain.entity.basic;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: RootEntity
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public abstract class RootEntity {

    private Long createTime;

    private Long modifyTime;

    private Boolean invalid;
}
