package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * hive
 *
 * @author wenkaijing
 * @version 2.0
 * @since 2022/01/05
 */
public class HiveQueryTool extends BaseQueryTool implements QueryToolInterface {
    public HiveQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
