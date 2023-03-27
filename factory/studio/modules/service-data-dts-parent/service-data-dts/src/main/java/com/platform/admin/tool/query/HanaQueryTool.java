package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * Hana数据库使用的查询工具
 *
 * @author AllDataDC
 */
public class HanaQueryTool extends BaseQueryTool implements QueryToolInterface {

    public HanaQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
