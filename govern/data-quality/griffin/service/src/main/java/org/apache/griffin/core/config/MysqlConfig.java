package org.apache.griffin.core.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MysqlConfig
 * @author AllDataDC
 * @date 2022/5/19
 */
@Configuration
@MapperScan(basePackages = "org.apache.griffin.core.mapper", sqlSessionTemplateRef = "mysqlSqlSessionTemplate")
public class MysqlConfig {
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource fetchDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "mysqlSqlSessionFactory")
    public SqlSessionFactory fetchSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        return bean.getObject();
    }

    @Bean(name = "mysqlSqlSessionTemplate")
    public SqlSessionTemplate fetchSqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }


}
