package com.platform.dts.admin.tool.query;

import com.platform.dts.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * mysql数据库使用的查询工具
 *
 * @author AllDataDC
 * @ClassName MySQLQueryTool
 * @Version 1.0
 * @since 2022/11/18 9:31
 */
public class MySQLQueryTool extends BaseQueryTool implements QueryToolInterface {

    public MySQLQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }

}
