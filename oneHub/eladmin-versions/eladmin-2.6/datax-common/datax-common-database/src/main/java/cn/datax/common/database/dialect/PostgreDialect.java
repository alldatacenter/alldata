package cn.datax.common.database.dialect;

import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

/**
 * Postgre 数据库方言
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class PostgreDialect extends AbstractDbDialect {

    @Override
    public String columns(String dbName, String tableName) {
        return "select col.column_name AS COLNAME, col.ordinal_position AS COLPOSITION, col.column_default AS DATADEFAULT, col.is_nullable AS NULLABLE, col.udt_name AS DATATYPE, " +
                "col.character_maximum_length AS DATALENGTH, col.numeric_precision AS DATAPRECISION, col.numeric_scale AS DATASCALE, des.description AS COLCOMMENT, " +
                "case when t.colname is null then 0 else 1 end as COLKEY " +
                "from information_schema.columns col left join pg_description des on col.table_name::regclass = des.objoid and col.ordinal_position = des.objsubid " +
                "left join ( " +
                "select pg_attribute.attname as colname from pg_constraint inner join pg_class on pg_constraint.conrelid = pg_class.oid " +
                "inner join pg_attribute on pg_attribute.attrelid = pg_class.oid and pg_attribute.attnum = any(pg_constraint.conkey) " +
                "where pg_class.relname = '" + tableName + "' and pg_constraint.contype = 'p' " +
                ") t on t.colname = col.column_name " +
                "where col.table_catalog = '" + dbName + "' and col.table_schema = 'public' and col.table_name = '" + tableName + "' order by col.ordinal_position ";
    }

    @Override
    public String tables(String dbName) {
        return "select relname AS TABLENAME, cast(obj_description(relfilenode, 'pg_class') as varchar) AS TABLECOMMENT from pg_class " +
                "where relname in (select tablename from pg_tables where schemaname = 'public' and tableowner = '" + dbName + "' and position('_2' in tablename) = 0) ";
    }

    @Override
    public String buildPaginationSql(String originalSql, long offset, long count) {
        StringBuilder sqlBuilder = new StringBuilder(originalSql);
        sqlBuilder.append(" LIMIT ").append(count).append(" offset ").append(offset);
        return sqlBuilder.toString();
    }

    @Override
    public RowMapper<DbColumn> columnMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbColumn entity = new DbColumn();
            entity.setColName(rs.getString("COLNAME"));
            entity.setDataType(rs.getString("DATATYPE"));
            entity.setDataLength(rs.getString("DATALENGTH"));
            entity.setDataPrecision(rs.getString("DATAPRECISION"));
            entity.setDataScale(rs.getString("DATASCALE"));
            entity.setColKey("1".equals(rs.getString("COLKEY")) ? true : false);
            entity.setNullable("YES".equals(rs.getString("NULLABLE")) ? true : false);
            entity.setColPosition(rs.getInt("COLPOSITION"));
            entity.setDataDefault(rs.getString("DATADEFAULT"));
            entity.setColComment(rs.getString("COLCOMMENT"));
            return entity;
        };
    }

    @Override
    public RowMapper<DbTable> tableMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbTable entity = new DbTable();
            entity.setTableName(rs.getString("TABLENAME"));
            entity.setTableComment(rs.getString("TABLECOMMENT"));
            return entity;
        };
    }
}
