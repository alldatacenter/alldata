package cn.datax.common.database.dialect;

import cn.datax.common.database.DbDialect;

/**
 * 方言抽象类
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public abstract class AbstractDbDialect implements DbDialect {

    @Override
    public String columns(String dbName, String tableName) {
        return "select column_name AS COLNAME, ordinal_position AS COLPOSITION, column_default AS DATADEFAULT, is_nullable AS NULLABLE, data_type AS DATATYPE, " +
                "character_maximum_length AS DATALENGTH, numeric_precision AS DATAPRECISION, numeric_scale AS DATASCALE, column_key AS COLKEY, column_comment AS COLCOMMENT " +
                "from information_schema.columns where table_schema = '" + dbName + "' and table_name = '" + tableName + "' order by ordinal_position ";
    }

    @Override
    public String tables(String dbName) {
        return "SELECT table_name AS TABLENAME, table_comment AS TABLECOMMENT FROM information_schema.tables where table_schema = '" + dbName + "' ";
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        // 获取 分页实际条数
        StringBuilder sqlBuilder = new StringBuilder(originalSql);
        sqlBuilder.append(" LIMIT ").append(offset).append(" , ").append(count);
        return sqlBuilder.toString();
    }

    @Override
    public String count(String sql) {
        return "SELECT COUNT(*) FROM ( " + sql + " ) TEMP";
    }
}
