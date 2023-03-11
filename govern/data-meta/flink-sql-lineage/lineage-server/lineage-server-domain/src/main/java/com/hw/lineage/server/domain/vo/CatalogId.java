package com.hw.lineage.server.domain.vo;

import com.hw.lineage.server.domain.repository.basic.Identifier;
import lombok.Data;

/**
 * @description: CatalogId
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CatalogId implements Identifier {

    private final Long value;

    public CatalogId(Long value) {
        this.value = value;
    }
}
