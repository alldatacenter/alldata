package com.platform.dts.admin.tool.query;

import com.platform.dts.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * TODO
 *
 * @author AllDataDC
 * @ClassName PostgresqlQueryTool
 * @Version 1.0
 * @since 2022/11/2 11:28
 */
public class PostgresqlQueryTool extends BaseQueryTool implements QueryToolInterface {
    public PostgresqlQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }

}
