package org.dromara.cloudeon.test;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.sql.SqlExecutor;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DbUtilTest {

    @Test
    public void testdoris() throws SQLException {
        DataSource ds = new SimpleDataSource("jdbc:mysql://10.81.16.19:9030", "root", null);
       Connection conn = ds.getConnection();
        // 执行非查询语句，返回影响的行数
        int count = SqlExecutor.execute(conn, "ALTER SYSTEM ADD BACKEND \"10.81.16.196:9050\" " );
    }
}
