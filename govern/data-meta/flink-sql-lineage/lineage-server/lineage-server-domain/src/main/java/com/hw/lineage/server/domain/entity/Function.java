package com.hw.lineage.server.domain.entity;

import com.hw.lineage.server.domain.entity.basic.BasicEntity;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.CatalogId;
import com.hw.lineage.server.domain.vo.FunctionId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: Function
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Function extends BasicEntity implements Entity {

    private FunctionId functionId;

    private CatalogId catalogId;

    private String functionName;

    private String database;

    private String invocation;

    private String functionPath;

    private String className;

    private String descr;
}
