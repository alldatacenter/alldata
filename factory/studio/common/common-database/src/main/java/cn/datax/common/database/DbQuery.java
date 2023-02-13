package cn.datax.common.database;

import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.common.database.core.PageResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 表数据查询接口
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public interface DbQuery {

    /**
     * 获取数据库连接
     */
    Connection getConnection();

    /**
     * 检测连通性
     */
    boolean valid();

    /**
     * 关闭数据源
     */
    void close();

    /**
     *  获取指定表 具有的所有字段列表
     * @param dbName
     * @param tableName
     * @return
     */
    List<DbColumn> getTableColumns(String dbName, String tableName);

    /**
     * 获取指定数据库下 所有的表信息
     *
     * @param dbName
     * @return
     */
    List<DbTable> getTables(String dbName);

    /**
     * 获取总数
     *
     * @param sql
     * @return
     */
    int count(String sql);

    /**
     * 获取总数带查询参数
     *
     * @param sql
     * @return
     */
    int count(String sql, Object[] args);

    /**
     * 获取总数带查询参数 NamedParameterJdbcTemplate
     *
     * @param sql
     * @return
     */
    int count(String sql, Map<String, Object> params);

    /**
     * 查询结果列表
     *
     * @param sql
     * @return
     */
    List<Map<String, Object>> queryList(String sql);

    /**
     * 查询结果列表带查询参数
     *
     * @param sql
     * @param args
     * @return
     */
    List<Map<String, Object>> queryList(String sql, Object[] args);

    /**
     * 查询结果分页
     *
     * @param sql
     * @param offset
     * @param size
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, long offset, long size);

    /**
     * 查询结果分页带查询参数
     * @param sql
     * @param args
     * @param offset
     * @param size
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, Object[] args, long offset, long size);

    /**
     * 查询结果分页带查询参数 NamedParameterJdbcTemplate
     * @param sql
     * @param params
     * @param offset
     * @param size
     * @return
     */
    PageResult<Map<String, Object>> queryByPage(String sql, Map<String, Object> params, long offset, long size);
}
