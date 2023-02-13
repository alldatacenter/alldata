package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * TODO
 *
 * @author AllDataDC
 * @ClassName PostgresqlQueryTool
 * @Version 1.0
 * @since 2023/01/2 11:28
 */
public class PostgresqlQueryTool extends BaseQueryTool implements QueryToolInterface {
    public PostgresqlQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }

}
