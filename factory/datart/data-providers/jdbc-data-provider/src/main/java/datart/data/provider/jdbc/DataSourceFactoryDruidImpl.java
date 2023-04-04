/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.data.provider.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import datart.data.provider.JdbcDataProvider;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
public class DataSourceFactoryDruidImpl implements DataSourceFactory<DruidDataSource> {

    @Override
    public DruidDataSource createDataSource(JdbcProperties jdbcProperties) throws Exception {
        Properties properties = configDataSource(jdbcProperties);
        DruidDataSource druidDataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(properties);
        druidDataSource.setBreakAfterAcquireFailure(true);
        druidDataSource.setConnectionErrorRetryAttempts(0);
        log.info("druid data source created ({})", druidDataSource.getName());
        return druidDataSource;
    }

    @Override
    public void destroy(DataSource dataSource) {
        ((DruidDataSource) dataSource).close();
    }

    private Properties configDataSource(JdbcProperties properties) {
        Properties pro = new Properties();
        //connect params
        pro.setProperty(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, properties.getDriverClass());
        pro.setProperty(DruidDataSourceFactory.PROP_URL, properties.getUrl());
        if (properties.getUser() != null) {
            pro.setProperty(DruidDataSourceFactory.PROP_USERNAME, properties.getUser());
        }
        if (properties.getPassword() != null) {
            pro.setProperty(DruidDataSourceFactory.PROP_PASSWORD, properties.getPassword());
        }
        pro.setProperty(DruidDataSourceFactory.PROP_MAXWAIT, JdbcDataProvider.DEFAULT_MAX_WAIT.toString());

        System.setProperty("druid.mysql.usePingMethod", "false");

        //opt config
        pro.putAll(properties.getProperties());
        return pro;
    }
}
