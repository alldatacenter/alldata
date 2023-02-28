///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// *
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// *
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.manage.common;
//
//import com.qlangtech.tis.db.parser.domain.DBConfig;
//import org.apache.commons.dbcp.BasicDataSource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.support.BeanDefinitionBuilder;
//import org.springframework.beans.factory.support.DefaultListableBeanFactory;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class SpringDBRegister extends DataSourceRegister.DBRegister {
//
//    private static final Logger logger = LoggerFactory.getLogger(SpringDBRegister.class);
//
//    private final DefaultListableBeanFactory factory;
//
//    private final String dbSuffix;
//
//    public SpringDBRegister(String dbName, DBConfig dbLinkMetaData, DefaultListableBeanFactory factory, String dbSuffix) {
//        super(dbName, dbLinkMetaData);
//        this.factory = factory;
//        this.dbSuffix = dbSuffix;
//    }
//
//    public SpringDBRegister(String dbName, DBConfig dbLinkMetaData, DefaultListableBeanFactory factory) {
//        this(dbName, dbLinkMetaData, factory, "Datasource");
//    }
//
//    @Override
//    protected void createDefinition(final String dbDefinitionId, String driverClassName, String jdbcUrl, String userName, String password) {
//        BeanDefinitionBuilder define = BeanDefinitionBuilder.genericBeanDefinition(TISDataSource.class);
//        define.setLazyInit(true);
//        define.addPropertyValue("driverClassName", driverClassName);
//        define.addPropertyValue("url", jdbcUrl);
//        define.addPropertyValue("username", userName);
//        define.addPropertyValue("password", password);
//        define.addPropertyValue("validationQuery", "select 1");
//        define.setDestroyMethodName("close");
//        logger.info("create dbbean:" + dbDefinitionId + dbSuffix + ",jdbc url:" + jdbcUrl);
//        factory.registerBeanDefinition(dbDefinitionId + dbSuffix, define.getBeanDefinition());
//    }
//}
