package com.platform.ck.config;

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
 * ClickHouseConfig
 * @author wlhbdp
 * @date 2022/6/2
 */
@Configuration
@MapperScan(basePackages = "com.platform.ck.mapper", sqlSessionTemplateRef = "ckSqlSessionTemplate")
public class CKConfig {
    @Bean(name = "ckDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource fetchDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "ckSqlSessionFactory")
    public SqlSessionFactory fetchSqlSessionFactory(@Qualifier("ckDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        return bean.getObject();
    }

    @Bean(name = "ckSqlSessionTemplate")
    public SqlSessionTemplate fetchSqlSessionTemplate(@Qualifier("ckSqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }


}
