package com.hw.lineage.server.domain.query.catalog;

import lombok.Data;

/**
 * @description: CatalogEntry
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CatalogEntry {
    private String pluginCode;

    private Long catalogId;
    
    private String catalogName;

}
