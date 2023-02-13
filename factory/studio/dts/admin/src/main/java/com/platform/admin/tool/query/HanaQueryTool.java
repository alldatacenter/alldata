package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * Hana数据库使用的查询工具
 *
 * @author zxl
 * @ClassName HanaQueryTool
 * @Version 1.0
 * @since 2022/10/14 14:36
 */
public class HanaQueryTool extends BaseQueryTool implements QueryToolInterface {

    public HanaQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
