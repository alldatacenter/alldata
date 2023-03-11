package com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl;

import com.hw.lineage.common.enums.SqlStatus;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.IntEnumTypeHandler;

/**
 * @description: SqlStatusTypeHandler
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class SqlStatusTypeHandler extends IntEnumTypeHandler<SqlStatus> {
    public SqlStatusTypeHandler() {
        super(SqlStatus.class);
    }
}
