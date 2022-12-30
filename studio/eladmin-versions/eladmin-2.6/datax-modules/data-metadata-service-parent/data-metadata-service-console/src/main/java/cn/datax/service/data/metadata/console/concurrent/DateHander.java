package cn.datax.service.data.metadata.console.concurrent;

import cn.datax.service.data.metadata.api.vo.SqlConsoleVo;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class DateHander extends CallableTemplate<SqlConsoleVo> {

    private CountDownLatch latch;

    private Connection conn;

    private String sql;

    public DateHander(CountDownLatch latch, Connection conn, String sql) {
        this.latch = latch;
        this.conn = conn;
        this.sql = sql;
    }

    @Override
    public SqlConsoleVo process() {
        log.info("执行sql:" + sql);
        long start = System.currentTimeMillis();
        Statement stmt = null;
        ResultSet rs = null;
        // 将查询数据存储到数据中
        List<Map<String, Object>> dataList = new ArrayList<>();
        // 存储列名的数组
        List<String> columnList = new LinkedList<>();
        // 新增、修改、删除受影响行数
        Integer updateCount = null;
        SqlConsoleVo sqlConsoleVo = new SqlConsoleVo();
        sqlConsoleVo.setSuccess(true);
        try {
            // 为了设置fetchSize，必须设置为false
            conn.setAutoCommit(false);
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(200);
            // 是否查询操作
            boolean execute = stmt.execute(sql);
            if (execute) {
                // 限制下最大数量
                stmt.setMaxRows(1000);
                rs = stmt.getResultSet();
                // 获取结果集的元数据信息
                ResultSetMetaData rsmd = rs.getMetaData();
                // 获取列字段的个数
                int colunmCount = rsmd.getColumnCount();
                for (int i = 1; i <= colunmCount; i++) {
                    // 获取所有的字段名称
                    columnList.add(rsmd.getColumnName(i));
                }
                while(rs.next()){
                    Map<String, Object> map = new HashMap<>();
                    for (int i = 1; i <= colunmCount; i++) {
                        // 获取列名
                        String columnName = rsmd.getColumnName(i);
                        Object val = null;
                        switch (rsmd.getColumnType(i)) {
                            case Types.ARRAY:
                                val = rs.getArray(columnName);
                                break;
                            case Types.BIGINT:
                                val = rs.getLong(columnName);
                                break;
                            case Types.BOOLEAN:
                            case Types.BIT:
                                val = rs.getBoolean(columnName);
                                break;
                            case Types.DOUBLE:
                                val = rs.getDouble(columnName);
                                break;
                            case Types.FLOAT:
                            case Types.REAL:
                                val = rs.getFloat(columnName);
                                break;
                            case Types.INTEGER:
                                val = rs.getInt(columnName);
                                break;
                            case Types.NVARCHAR:
                            case Types.NCHAR:
                            case Types.LONGNVARCHAR:
                                val = rs.getNString(columnName);
                                break;
                            case Types.VARCHAR:
                            case Types.CHAR:
                            case Types.LONGVARCHAR:
                                val = rs.getString(columnName);
                                break;
                            case Types.TINYINT:
                            case Types.BINARY:
                            case Types.VARBINARY:
                                val = rs.getByte(columnName);
                                break;
                            case Types.SMALLINT:
                                val = rs.getShort(columnName);
                                break;
                            case Types.DATE:
                                val = rs.getDate(columnName);
                                break;
                            case Types.TIME:
                                val = rs.getTime(columnName);
                                break;
                            case Types.TIMESTAMP:
                                val = rs.getTimestamp(columnName);
                                break;
                            case Types.NUMERIC:
                            case Types.DECIMAL:
                                val = rs.getBigDecimal(columnName);
                                break;
                            case Types.BLOB:
                            case Types.CLOB:
                            case Types.LONGVARBINARY:
                            case Types.DATALINK:
                            case Types.REF:
                            case Types.STRUCT:
                            case Types.DISTINCT:
                            case Types.JAVA_OBJECT:
                                break;
                            default:
                                val = rs.getObject(columnName);
                                break;
                        }
                        map.put(columnName, val);
                    }
                    dataList.add(map);
                }
            } else {
                // 执行新增、修改、删除受影响行数
                updateCount = stmt.getUpdateCount();
            }
            conn.commit();
        } catch (SQLException e) {
            sqlConsoleVo.setSuccess(false);
            if(conn != null){
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                }
            }
        } finally {
            try {
                if(rs != null){
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {}
        }
        latch.countDown();
        long end = System.currentTimeMillis();
        log.info("线程查询数据用时:" + (end - start) + "ms");
        sqlConsoleVo.setSql(sql);
        sqlConsoleVo.setCount(updateCount);
        sqlConsoleVo.setColumnList(columnList);
        sqlConsoleVo.setDataList(dataList);
        sqlConsoleVo.setTime(end - start);
        return sqlConsoleVo;
    }
}
