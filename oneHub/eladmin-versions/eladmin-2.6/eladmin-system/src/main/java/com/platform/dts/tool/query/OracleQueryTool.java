package com.platform.dts.tool.query;

import com.platform.dts.entity.JobDatasource;

import java.sql.SQLException;

/**
 * Oracle数据库使用的查询工具
 *
 * @author AllDataDC
 * @ClassName MySQLQueryTool
 * @Version 1.0
 * @since 2022/11/18 9:31
 */
public class OracleQueryTool extends BaseQueryTool implements QueryToolInterface {

    public OracleQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
