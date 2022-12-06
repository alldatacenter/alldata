package com.platform.dts.admin.tool.query;

import com.platform.dts.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * sql server
 *
 * @author AllDataDC
 * @version 1.0
 * @since 2022/11/2
 */
public class SqlServerQueryTool extends BaseQueryTool implements QueryToolInterface {
    public SqlServerQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
