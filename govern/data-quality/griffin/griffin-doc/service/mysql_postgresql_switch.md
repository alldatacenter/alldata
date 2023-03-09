<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Mysql and postgresql switch

## Overview
Apache Griffin uses EclipseLink as the default JPA implementation, which supports two kinds of database, mysql and postgresql. This document mainly describes the steps of how to switch mysql and postgresql as backend database.

- [Mysql database](#1.1)
- [Postgresql database](#1.2)

<h2 id = "1.1"></h2>

## Mysql database 
### Add mysql dependency

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
### Configure properties

- configure application.properties

        spring.datasource.url = jdbc:mysql://localhost:3306/quartz?autoReconnect=true&useSSL=false
        spring.datasource.username = griffin
        spring.datasource.password = 123456
        spring.jpa.generate-ddl=true
        spring.datasource.driver-class-name = com.mysql.jdbc.Driver
        spring.jpa.show-sql = true
   If you use hibernate as your jpa implentation, you need also to add following configuration.
     
        spring.jpa.hibernate.ddl-auto = update
        spring.jpa.hibernate.naming-strategy = org.hibernate.cfg.ImprovedNamingStrategy
- configure quartz.properties

      org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate

<h2 id = "1.2"></h2>

## Postgresql database 

### Add postgresql dependency

    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

### Configure properties
- configure application.properties

        spring.datasource.url = jdbc:postgresql://localhost:5432/quartz?autoReconnect=true&useSSL=false
        spring.datasource.username = griffin
        spring.datasource.password = 123456
        spring.jpa.generate-ddl=true
        spring.datasource.driver-class-name = org.postgresql.Driver
        spring.jpa.show-sql = true
  If you use hibernate as your jpa implentation, you need also to add following configuration.
     
        spring.jpa.hibernate.ddl-auto = update
        spring.jpa.hibernate.naming-strategy = org.hibernate.cfg.ImprovedNamingStrategy
       
- configure quartz.properties

      org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      
