package com.platform.admin.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库查询服务
 *
 * @author AllDataDC
 * @ClassName JdbcDatasourceQueryService * @date 2022/7/31 20:50
 */
public interface DatasourceQueryService {

    /**
     * 获取db列表
     * @param id
     * @return
     */
    List<String> getDBs(Long id) throws IOException;

    /**
     * 根据数据源表id查询出可用的表
     *
     * @param id
     * @return
     */
    List<String> getTables(Long id,String tableSchema) throws IOException;

    /**
     * 获取CollectionNames
     * @param dbName
     * @return
     */
    List<String> getCollectionNames(long id,String dbName) throws IOException;

    /**
     * 根据数据源id，表名查询出该表所有字段
     *
     * @param id
     * @return
     */
    List<String> getColumns(Long id, String tableName) throws IOException;

    /**
     * 根据 sql 语句获取字段
     *
     * @param datasourceId
     * @param querySql
     * @return
     */
    List<String> getColumnsByQuerySql(Long datasourceId, String querySql) throws SQLException;

    /**
     * 获取PG table schema
     * @param id
     * @return
     */
    List<String> getTableSchema(Long id);
}
