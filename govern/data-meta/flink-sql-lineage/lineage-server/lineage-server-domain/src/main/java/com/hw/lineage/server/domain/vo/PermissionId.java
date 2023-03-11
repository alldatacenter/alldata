package com.hw.lineage.server.domain.vo;

import com.hw.lineage.server.domain.repository.basic.Identifier;
import lombok.Data;

/**
 * @description: PermissionId
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PermissionId implements Identifier {

    private final Long value;

    public PermissionId(Long value) {
        this.value = value;
    }
}