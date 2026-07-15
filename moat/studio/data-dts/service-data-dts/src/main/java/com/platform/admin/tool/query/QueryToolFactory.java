package com.platform.admin.tool.query;

import com.platform.admin.entity.JobDatasource;
import com.platform.admin.util.JdbcConstants;
import com.platform.admin.util.RdbmsException;

import java.sql.SQLException;

/**
 * 工具类，获取单例实体
 *
 * @author AllDataDC
 */
public class QueryToolFactory {

    public static BaseQueryTool getByDbType(JobDatasource jobDatasource) {
        //获取dbType
        String datasource = jobDatasource.getDatasource();
        if (JdbcConstants.MYSQL.equals(datasource)) {
            return getMySQLQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.ORACLE.equals(datasource)) {
            return getOracleQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.POSTGRESQL.equals(datasource)) {
            return getPostgresqlQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.SQL_SERVER.equals(datasource)) {
            return getSqlserverQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.HIVE.equals(datasource)) {
            return getHiveQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.CLICKHOUSE.equals(datasource)) {
            return getClickHouseQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.HBASE20XSQL.equals(datasource)) {
            return getHbase20XsqlQueryToolQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.HANA.equals(datasource)) {
            return getHanaQueryToolInstance(jobDatasource);
        }
        throw new UnsupportedOperationException("找不到该类型: ".concat(datasource));
    }

    private static BaseQueryTool getMySQLQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new MySQLQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.MYSQL,
                    e,jdbcDatasource.getJdbcUsername(),jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getOracleQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new OracleQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.ORACLE,
                    e,jdbcDatasource.getJdbcUsername(),jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getPostgresqlQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new PostgresqlQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.POSTGRESQL,
                    e,jdbcDatasource.getJdbcUsername(),jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getSqlserverQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new SqlServerQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.SQL_SERVER,
                    e,jdbcDatasource.getJdbcUsername(),jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getHiveQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new HiveQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.HIVE,
                    e,jdbcDatasource.getJdbcUsername(),jdbcDatasource.getDatasourceName());
        }
    }
    private static BaseQueryTool getClickHouseQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new ClickHouseQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.CLICKHOUSE,
                    e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }
    private static BaseQueryTool getHanaQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new HanaQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.HANA,
                    e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }
    private static Hbase20XsqlQueryTool getHbase20XsqlQueryToolQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new Hbase20XsqlQueryTool(jdbcDatasource);
        } catch (SQLException e) {
            throw RdbmsException.asConnException(JdbcConstants.HBASE20XSQL,
                    e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }
}
