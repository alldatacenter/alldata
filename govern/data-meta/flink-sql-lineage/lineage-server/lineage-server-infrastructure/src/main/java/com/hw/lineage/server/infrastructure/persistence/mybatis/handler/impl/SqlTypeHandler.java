package com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl;

import com.hw.lineage.common.enums.SqlType;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.StringEnumTypeHandler;

/**
 * @description: SqlTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class SqlTypeHandler extends StringEnumTypeHandler<SqlType> {
    public SqlTypeHandler() {
        super(SqlType.class);
    }
}
