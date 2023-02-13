package cn.datax.common.database.dialect;

import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.common.database.exception.DataQueryException;
import org.springframework.jdbc.core.RowMapper;

/**
 * 未知 数据库方言
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class UnknownDialect extends AbstractDbDialect {

    @Override
    public String columns(String dbName, String tableName) {
        throw new DataQueryException("不支持的数据库类型");
    }

    @Override
    public String tables(String dbName) {
        throw new DataQueryException("不支持的数据库类型");
    }

    @Override
    public String buildPaginationSql(String sql, long offset, long count) {
        throw new DataQueryException("不支持的数据库类型");
    }

    @Override
    public String count(String sql) {
        throw new DataQueryException("不支持的数据库类型");
    }

    @Override
    public RowMapper<DbColumn> columnMapper() {
        throw new DataQueryException("不支持的数据库类型");
    }

    @Override
    public RowMapper<DbTable> tableMapper() {
        throw new DataQueryException("不支持的数据库类型");
    }
}
