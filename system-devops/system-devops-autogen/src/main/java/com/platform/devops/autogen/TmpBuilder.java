package com.platform.devops.autogen;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by wulinhao on 2020/04/12.
 */
public abstract class TmpBuilder {
    protected InputParams ip;
    protected VelocityEngine ve;

    public TmpBuilder(InputParams ip) {
        this.ip = ip;
        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
    }

    protected VelocityContext getContext() {
        VelocityContext ctx = new VelocityContext();
        ctx.put("name", ip.name);
        return ctx;
    }

    protected String buildVm(String fromPath, VelocityContext ctx) throws IOException {
        Template t = ve.getTemplate(fromPath);
        StringWriter sw = new StringWriter();
        t.merge(ctx, sw);
        return sw.toString();
    }

    protected void writeFile(String toPath, String content) throws IOException {
        FileUtils.write(new File(toPath), content, StandardCharsets.UTF_8);
    }

    public void build() throws Exception {
        //get db if need
        List<TableInfo> tableInfo = null;
        if (ip.dbUrl != null) {
            tableInfo = getDbInfo();
        }
        buildWithDb(tableInfo);
    }

    protected abstract void buildWithDb(List<TableInfo> tableInfo) throws IOException;

    private List<TableInfo> getDbInfo() throws Exception {
        Connection conn = null;
        Statement stmt = null;
        try {
            // 注册 JDBC 驱动
            Class.forName("com.mysql.jdbc.Driver");
            // 打开链接
            System.out.println("连接数据库 " + ip.dbUrl + " " + ip.dbUser + " " + ip.dbPass);
            conn = DriverManager.getConnection(ip.dbUrl, ip.dbUser, ip.dbPass);
            // 执行查询
            System.out.println(" 实例化Statement对象...");
            stmt = conn.createStatement();
            DatabaseMetaData dbmd = conn.getMetaData();

            String[] types = {"TABLE"};
            List<String> tableNames = new ArrayList<>();
            {
                ResultSet rs = dbmd.getTables(null, null, "%", types);
                ResultSetMetaData mt = rs.getMetaData();
                int count = mt.getColumnCount();
                int line = 0;
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    tableNames.add(tableName);
                }
                rs.close();
            }
            List<TableInfo> tableInfos = new ArrayList<>();
            for (String oneTable : tableNames) {
                ResultSet rs = stmt.executeQuery("show create table " + oneTable);
                ResultSetMetaData mt = rs.getMetaData();

                while (rs.next()) {
                    String tableName = rs.getString("Table");
                    String createTablelines = rs.getString("Create Table");
                    TableInfo tableInfo = TableInfo.parse(tableName, createTablelines);
                    tableInfos.add(tableInfo);
                }
                rs.close();
            }

            // 完成后关闭

            stmt.close();
            conn.close();

            return tableInfos;
        } catch (Exception e) {
            throw e;
        } finally {
            // 关闭资源
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException se2) {
            }// 什么都不做
            try {
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    //    try {
//        URI uri = Main.class.getResource("/scala-play").toURI();
//        Path myPath;
//        if (uri.getScheme().equals("jar")) {
//            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
//            myPath = fileSystem.getPath("/scala-play");
//        } else {
//            myPath = Paths.get(uri);
//        }
//        Stream<Path> walk = Files.walk(myPath, 1);
//        for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
//            System.out.println(it.next());
//        }
//    }
//        catch (Exception e){
//        e.printStackTrace();
//    }

    private FileSystem jarFileSystem = null;

    protected List<Path> getResourceFolderFiles(String folder) {
        List<Path> re = new ArrayList<>();
        try {
            URI uri = Main.class.getResource(folder).toURI();
            Path myPath;
            if (uri.getScheme().equals("jar")) {
                if (jarFileSystem == null) {
                    jarFileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
                }
                myPath = jarFileSystem.getPath(folder);
            } else {
                myPath = Paths.get(uri);
            }
            Stream<Path> walk = Files.walk(myPath);
            for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                Path p = it.next();
                if (!Files.isDirectory(p)) {
                    re.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return re;
    }

}
