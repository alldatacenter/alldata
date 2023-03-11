package com.hw.lineage.server.application.dto;

import com.alibaba.fastjson2.JSONObject;
import com.hw.lineage.common.enums.CatalogType;
import com.hw.lineage.server.application.dto.basic.BasicDTO;
import lombok.Data;

/**
 * @description: CatalogDTO
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CatalogDTO extends BasicDTO {

    private Long catalogId;

    private Long pluginId;

    private String catalogName;

    private CatalogType catalogType;

    private String defaultDatabase;

    private String descr;

    private JSONObject catalogProperties;

    private Boolean defaultCatalog;
}
