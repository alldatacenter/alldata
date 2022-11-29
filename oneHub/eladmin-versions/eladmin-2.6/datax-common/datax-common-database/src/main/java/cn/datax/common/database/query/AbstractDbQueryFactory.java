package cn.datax.common.database.query;

import cn.datax.common.database.DbDialect;
import cn.datax.common.database.DbQuery;
import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.common.database.core.PageResult;
import cn.datax.common.database.dialect.OracleDialect;
import cn.datax.common.database.exception.DataQueryException;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Setter
public abstract class AbstractDbQueryFactory implements DbQuery {

	protected DataSource dataSource;

	protected JdbcTemplate jdbcTemplate;

	protected DbDialect dbDialect;

	@Override
	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			throw new DataQueryException("获取数据库连接出错");
		}
	}

	@Override
	public boolean valid() {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			return conn.isValid(0);
		} catch (SQLException e) {
			throw new DataQueryException("检测连通性出错");
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new DataQueryException("关闭数据库连接出错");
				}
			}
		}

	}

	@Override
	public void close() {
		if (dataSource instanceof HikariDataSource) {
			((HikariDataSource) dataSource).close();
		} else {
			throw new DataQueryException("不合法数据源类型");
		}
	}

	@Override
	public List<DbColumn> getTableColumns(String dbName, String tableName) {
		String sql = dbDialect.columns(dbName, tableName);
		if (dbDialect instanceof OracleDialect) {
			List<DbColumn> longColumns = jdbcTemplate.query(sql, dbDialect.columnLongMapper());
			List<DbColumn> queryColumns = jdbcTemplate.query(sql, dbDialect.columnMapper());
			for (int i = 0; i < longColumns.size(); i++) {
				DbColumn longColumn = longColumns.get(i);
				DbColumn otherColumn = queryColumns.get(i);
				otherColumn.setDataDefault(longColumn.getDataDefault());
			}
			return queryColumns;
		}
		return jdbcTemplate.query(sql, dbDialect.columnMapper());
	}

	@Override
	public List<DbTable> getTables(String dbName) {
		String sql = dbDialect.tables(dbName);
		return jdbcTemplate.query(sql, dbDialect.tableMapper());
	}

	@Override
	public int count(String sql) {
		return jdbcTemplate.queryForObject(dbDialect.count(sql), Integer.class);
	}

	@Override
	public int count(String sql, Object[] args) {
		return jdbcTemplate.queryForObject(dbDialect.count(sql), args, Integer.class);
	}

	@Override
	public int count(String sql, Map<String, Object> params) {
		NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		return namedJdbcTemplate.queryForObject(dbDialect.count(sql), params, Integer.class);
	}

	@Override
	public List<Map<String, Object>> queryList(String sql) {
		return jdbcTemplate.queryForList(sql);
	}

	@Override
	public List<Map<String, Object>> queryList(String sql, Object[] args) {
		return jdbcTemplate.queryForList(sql, args);
	}

	@Override
	public PageResult<Map<String, Object>> queryByPage(String sql, long offset, long size) {
		int total = count(sql);
		String pageSql = dbDialect.buildPaginationSql(sql, offset, size);
		List<Map<String, Object>> records = jdbcTemplate.queryForList(pageSql);
		return new PageResult<>(total, records);
	}

	@Override
	public PageResult<Map<String, Object>> queryByPage(String sql, Object[] args, long offset, long size) {
		int total = count(sql, args);
		String pageSql = dbDialect.buildPaginationSql(sql, offset, size);
		List<Map<String, Object>> records = jdbcTemplate.queryForList(pageSql, args);
		return new PageResult<>(total, records);
	}

	@Override
	public PageResult<Map<String, Object>> queryByPage(String sql, Map<String, Object> params, long offset, long size) {
		int total = count(sql, params);
		String pageSql = dbDialect.buildPaginationSql(sql, offset, size);
		NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<Map<String, Object>> records = namedJdbcTemplate.queryForList(pageSql, params);
		return new PageResult<>(total, records);
	}
}
