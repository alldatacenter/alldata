package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * Oracle数据库使用的查询工具
 *
 * @author AllDataDC
 */
public class OracleQueryTool extends BaseQueryTool implements QueryToolInterface {

    public OracleQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
