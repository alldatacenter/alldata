package org.dromara.cloudeon.test;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.handler.StringHandler;
import cn.hutool.db.sql.SqlExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SqlTest {

    @Test
    public void test() {
        String url = "jdbc:mysql://10.81.17.8:33066/hive_db_ce?createDatabaseIfNotExist=true&amp;useUnicode=true&amp;characterEncoding=utf-8&amp;useSSL=false";
        String substring = url.substring(0, url.indexOf("?"));
        DataSource ds = new SimpleDataSource(substring, "root", "bigdata");
            try (Connection conn = ds.getConnection();) {
                String sql = " select SCHEMA_VERSION from VERSION";
                log.info("执行sql：{}", sql);
                String query = SqlExecutor.query(conn, sql, new StringHandler());
                System.out.println(query);

            } catch (SQLException e) {
                if (e.getMessage().contains("doesn't exist")){
                    System.out.println("hhh");
                }
                e.printStackTrace();
            }
    }
}
