package cn.datax.common.database.dialect;

import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle Oracle11g及以下数据库方言
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public class OracleDialect extends AbstractDbDialect {

	@Override
	public String columns(String dbName, String tableName) {
		return "select columns.column_name AS colName, columns.data_type AS DATATYPE, columns.data_length AS DATALENGTH, columns.data_precision AS DATAPRECISION, " +
				"columns.data_scale AS DATASCALE, columns.nullable AS NULLABLE, columns.column_id AS COLPOSITION, columns.data_default AS DATADEFAULT, comments.comments AS COLCOMMENT," +
				"case when t.column_name is null then 0 else 1 end as COLKEY " +
				"from sys.user_tab_columns columns LEFT JOIN sys.user_col_comments comments ON columns.table_name = comments.table_name AND columns.column_name = comments.column_name " +
				"left join ( " +
				"select col.column_name as column_name, con.table_name as table_name from user_constraints con, user_cons_columns col " +
				"where con.constraint_name = col.constraint_name and con.constraint_type = 'P' " +
				") t on t.table_name = columns.table_name and columns.column_name = t.column_name " +
				"where columns.table_name = UPPER('" + tableName + "') order by columns.column_id ";
	}

	@Override
	public String tables(String dbName) {
		return "select tables.table_name AS TABLENAME, comments.comments AS TABLECOMMENT from sys.user_tables tables " +
				"LEFT JOIN sys.user_tab_comments comments ON tables.table_name = comments.table_name ";
	}

	@Override
	public String buildPaginationSql(String originalSql, long offset, long count) {
		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT * FROM ( SELECT TMP.*, ROWNUM ROW_ID FROM ( ");
		sqlBuilder.append(originalSql).append(" ) TMP WHERE ROWNUM <=").append((offset >= 1) ? (offset + count) : count);
		sqlBuilder.append(") WHERE ROW_ID > ").append(offset);
		return sqlBuilder.toString();
	}

	@Override
	public RowMapper<DbColumn> columnLongMapper() {
		return (ResultSet rs, int rowNum) -> {
			DbColumn entity = new DbColumn();
			entity.setDataDefault(rs.getString("DATADEFAULT"));
			return entity;
		};
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
			entity.setColKey("1".equals(rs.getString("COLKEY")));
			entity.setNullable("Y".equals(rs.getString("NULLABLE")));
			//long类型，单独处理
			//entity.setDataDefault(rs.getString("DATADEFAULT"));
			entity.setColPosition(rs.getInt("COLPOSITION"));
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
