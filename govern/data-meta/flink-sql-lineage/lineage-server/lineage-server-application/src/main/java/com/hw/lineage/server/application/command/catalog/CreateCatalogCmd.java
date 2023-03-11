package com.hw.lineage.server.application.command.catalog;

import com.alibaba.fastjson2.JSONObject;
import com.hw.lineage.common.enums.CatalogType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description: CreateCatalogCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreateCatalogCmd {

    @NotNull
    private Long pluginId;

    @NotBlank
    private String catalogName;

    @NotNull
    private CatalogType catalogType;

    @NotBlank
    private String defaultDatabase;

    private String descr;

    private JSONObject catalogProperties;

    private Boolean defaultCatalog;
}
