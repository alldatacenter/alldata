package com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl;

import com.hw.lineage.common.enums.CatalogType;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.StringEnumTypeHandler;

/**
 * @description: CatalogTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class CatalogTypeHandler extends StringEnumTypeHandler<CatalogType> {
    public CatalogTypeHandler() {
        super(CatalogType.class);
    }
}
