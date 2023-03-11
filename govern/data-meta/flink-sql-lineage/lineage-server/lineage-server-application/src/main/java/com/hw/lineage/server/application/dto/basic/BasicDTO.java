package com.hw.lineage.server.application.dto.basic;

import lombok.Data;

/**
 * @description: BasicDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public abstract class BasicDTO extends RootDTO {
    private Long createUserId;

    private Long modifyUserId;

}
