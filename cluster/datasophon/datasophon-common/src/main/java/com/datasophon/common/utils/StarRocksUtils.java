package com.datasophon.common.utils;

import com.datasophon.common.model.ProcInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StarRocksUtils {
    private static final Logger logger = LoggerFactory.getLogger(StarRocksUtils.class);

    public static void allFollower(String feMaster, String hostname) throws SQLException, ClassNotFoundException {
        String sql = "ALTER SYSTEM add FOLLOWER \""+hostname+":9010\";";
        logger.info("sql is {}",sql);
        executeSql(feMaster, hostname,sql);
    }

    public static void allBackend(String feMaster, String hostname) throws SQLException, ClassNotFoundException {
        String sql = "ALTER SYSTEM add BACKEND  \""+hostname+":9050\";";
        logger.info("sql is {}",sql);
        executeSql(feMaster, hostname,sql);
    }

    private static void executeSql(String feMaster, String hostname,String sql) throws ClassNotFoundException, SQLException {
        Connection connection = getConnection(feMaster);
        Statement statement = connection.createStatement();
        logger.info("generate be {} to cluster",hostname);
        if(Objects.nonNull(connection) && Objects.nonNull(statement)){
            statement.executeUpdate(sql);
        }
        close(connection, statement);
    }

    private static Connection getConnection(String feMaster) throws ClassNotFoundException, SQLException {
        String username = "root";
        String password = "";
        String url = "jdbc:mysql://" + feMaster + ":9030";
        //加载驱动
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(url, username, password);
    }

    private static void close(Connection connection, Statement statement) throws SQLException {
        if (Objects.nonNull(connection) && Objects.nonNull(statement)) {
            statement.close();
            connection.close();
        }
    }

    public static List<ProcInfo> showFrontends(String feMaster) throws SQLException, ClassNotFoundException {
        String sql = "SHOW PROC '/frontends';";
        logger.info("sql is {}",sql);
        return executeQuerySql(feMaster, sql);
    }

    public static List<ProcInfo> listDeadFrontends(String feMaster) throws SQLException, ClassNotFoundException {
        String sql = "SHOW PROC '/frontends';";
        logger.info("sql is {}",sql);
        return getDeadProcInfos(feMaster, sql);
    }


    public static List<ProcInfo> listDeadBackends(String feMaster) throws SQLException, ClassNotFoundException {
        String sql = "SHOW PROC '/frontends';";
//        logger.info("sql is {}",sql);
        return getDeadProcInfos(feMaster, sql);
    }

    public static List<ProcInfo> showBackends(String feMaster) throws SQLException, ClassNotFoundException {
        String sql = "SHOW PROC '/backends';";
//        logger.info("sql is {}",sql);
        return executeQuerySql(feMaster, sql);
    }

    private static List<ProcInfo> executeQuerySql(String feMaster, String sql) throws SQLException, ClassNotFoundException {
        Connection connection = getConnection(feMaster);
        Statement statement = connection.createStatement();
        ArrayList<ProcInfo> list = new ArrayList<>();
        if(Objects.nonNull(connection) && Objects.nonNull(statement)){
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()){
                ProcInfo procInfo = new ProcInfo();
                procInfo.setHostName(resultSet.getString("HostName"));
                procInfo.setAlive(resultSet.getBoolean("Alive"));
                procInfo.setErrMsg(resultSet.getString("ErrMsg"));
                list.add(procInfo);
            }
        }
        close(connection, statement);
        return list;
    }

    private static List<ProcInfo> getDeadProcInfos(String feMaster, String sql) throws SQLException, ClassNotFoundException {
        List<ProcInfo> list = executeQuerySql(feMaster, sql);
        ArrayList<ProcInfo> deadList = new ArrayList<>();
        for (ProcInfo procInfo : list) {
            if (!procInfo.getAlive()) {
                deadList.add(procInfo);
            }
        }
        return deadList;
    }
}
