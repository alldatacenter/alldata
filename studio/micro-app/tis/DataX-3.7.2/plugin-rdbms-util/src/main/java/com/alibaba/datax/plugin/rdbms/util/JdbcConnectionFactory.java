package com.alibaba.datax.plugin.rdbms.util;

import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;

import java.sql.Connection;

/**
 * Date: 15/3/16 下午3:12
 */
public class JdbcConnectionFactory implements ConnectionFactory {

    private IDataSourceFactoryGetter dataBaseType;

    private String jdbcUrl;

    private String userName;

    private String password;

    public JdbcConnectionFactory(IDataSourceFactoryGetter dataBaseType, String jdbcUrl, String userName, String password) {
        this.dataBaseType = dataBaseType;
        this.jdbcUrl = jdbcUrl;
        this.userName = userName;
        this.password = password;
    }

    @Override
    public Connection getConnecttion() {
        return DBUtil.getConnection(dataBaseType, jdbcUrl, userName, password);
        //return dataBaseType;
    }

    @Override
    public IDataSourceFactoryGetter getConnecttionWithoutRetry() {
        //return DBUtil.getConnectionWithoutRetry(dataBaseType, jdbcUrl, userName, password);
        return dataBaseType;
    }

    @Override
    public String getConnectionInfo() {
        return "jdbcUrl:" + jdbcUrl;
    }
}
