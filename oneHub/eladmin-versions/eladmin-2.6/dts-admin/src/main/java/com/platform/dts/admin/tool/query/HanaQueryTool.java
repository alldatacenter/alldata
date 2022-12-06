package com.platform.dts.admin.tool.query;

import com.platform.dts.admin.entity.JobDatasource;

import java.sql.SQLException;

/**
 * Hana数据库使用的查询工具
 *
 * @author AllDataDC
 * @ClassName HanaQueryTool
 * @Version 1.0
 * @date 2022/11/14 14:36
 */
public class HanaQueryTool extends BaseQueryTool implements QueryToolInterface {

    public HanaQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
