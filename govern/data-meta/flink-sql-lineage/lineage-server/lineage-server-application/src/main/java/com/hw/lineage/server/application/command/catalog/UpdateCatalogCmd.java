package com.hw.lineage.server.application.command.catalog;

import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: UpdateCatalogCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdateCatalogCmd {

    @ApiModelProperty(hidden = true)
    private Long catalogId;

    private String catalogName;

    private String defaultDatabase;

    private String descr;

    private JSONObject catalogProperties;

}
