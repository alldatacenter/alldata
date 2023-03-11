package com.hw.lineage.server.domain.entity.basic;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: BasicEntity
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public abstract class BasicEntity extends RootEntity{

    private Long createUserId;

    private Long modifyUserId;
}
