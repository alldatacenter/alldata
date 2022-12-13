package com.platform.dts.tool.query;

import com.platform.dts.entity.JobDatasource;

import java.sql.SQLException;

/**
 * hive
 *
 * @author wenkaijing
 * @version 2.0
 * @date 2022/11/05
 */
public class HiveQueryTool extends BaseQueryTool implements QueryToolInterface {
    public HiveQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
