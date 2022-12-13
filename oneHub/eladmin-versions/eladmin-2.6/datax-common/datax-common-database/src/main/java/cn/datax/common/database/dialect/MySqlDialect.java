package cn.datax.common.database.dialect;

import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

/**
 * MySql 数据库方言
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class MySqlDialect extends AbstractDbDialect {

    @Override
    public RowMapper<DbColumn> columnMapper() {
        return (ResultSet rs, int rowNum) -> {
            DbColumn entity = new DbColumn();
            entity.setColName(rs.getString("COLNAME"));
            entity.setDataType(rs.getString("DATATYPE"));
            entity.setDataLength(rs.getString("DATALENGTH"));
            entity.setDataPrecision(rs.getString("DATAPRECISION"));
            entity.setDataScale(rs.getString("DATASCALE"));
            entity.setColKey("PRI".equals(rs.getString("COLKEY")));
            entity.setNullable("YES".equals(rs.getString("NULLABLE")));
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
