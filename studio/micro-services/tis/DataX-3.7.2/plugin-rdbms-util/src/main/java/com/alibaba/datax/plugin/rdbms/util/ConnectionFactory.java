package com.alibaba.datax.plugin.rdbms.util;

import com.qlangtech.tis.plugin.ds.IDBReservedKeys;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;

import java.sql.Connection;

/**
 * Date: 15/3/16 下午2:17
 */
public interface ConnectionFactory {

    public Connection getConnecttion();

    public IDataSourceFactoryGetter getConnecttionWithoutRetry();

    public default IDBReservedKeys getDSEntityEscape() {
        return getConnecttionWithoutRetry().getDataSourceFactory();
    }

    public String getConnectionInfo();

}
