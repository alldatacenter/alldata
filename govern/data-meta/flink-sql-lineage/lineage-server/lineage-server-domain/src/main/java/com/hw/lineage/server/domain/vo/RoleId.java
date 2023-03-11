package com.hw.lineage.server.domain.vo;

import com.hw.lineage.server.domain.repository.basic.Identifier;
import lombok.Data;

/**
 * @description: RoleId
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class RoleId implements Identifier {

    private final Long value;

    public RoleId(Long value) {
        this.value = value;
    }
}

