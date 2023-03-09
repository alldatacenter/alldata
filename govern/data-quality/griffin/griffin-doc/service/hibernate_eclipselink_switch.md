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

# Hibernate and eclipselink switch
## Overview
In this document,we list two main part.

- [Migration from Hibernate to EclipseLink](#0.0)
- [Migration from EclipseLink to Hibernate](#0.1)

<h2 id = "0.0"></h2>

## Migration from Hibernate to EclipseLink
By default, Spring Data uses Hibernate as the default JPA implementation provider.However, Hibernate is certainly not the only JPA implementation available to us.In this document, we’ll go through steps necessary to set up EclipseLink as the implementation provider for Spring Data JPA.The migration will not need to convert any Hibernate annotations to EclipseLink annotations in application code. 

## Migration main steps
- [exclude hibernate dependencies](#1.1)
- [add EclipseLink dependency](#1.2)
- [configure EclipseLink static weaving](#1.3)
- [spring code configuration](#1.4)
- [configure properties](#1.5)

<h2 id = "1.1"></h2>

### Exclude hibernate dependencies
Since we want to use EclipseLink instead as the JPA provider, we don't need it anymore.Therefore we can remove it from our project by excluding its dependencies:

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.hibernate</groupId>
                <artifactId>*</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

<h2 id = "1.2"></h2>

### Add EclipseLink dependency
To use it in our Spring Boot application, we just need to add the org.eclipse.persistence.jpa dependency in the pom.xml of our project.

    <properties>
        <eclipselink.version>2.6.0</eclipselink.version>
    </properties>
    <dependency>
        <groupId>org.eclipse.persistence</groupId>
        <artifactId>org.eclipse.persistence.jpa</artifactId>
        <version>${eclipselink.version}</version>
    </dependency>
    
<h2 id = "1.3"></h2> 

### Configure EclipseLink static weaving
EclipseLink requires the domain types to be instrumented to implement lazy-loading. This can be achieved either through static weaving at compile time or dynamically at class loading time (load-time weaving). In Apache Griffin,we use static weaving in pom.xml.

    <build>
        <plugins>
            <plugin>
                <groupId>com.ethlo.persistence.tools</groupId>
                <artifactId>eclipselink-maven-plugin</artifactId>
                <version>2.7.0</version>
                <executions>
                    <execution>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>weave</goal>
                        </goals>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>org.eclipse.persistence</groupId>
                        <artifactId>org.eclipse.persistence.jpa</artifactId>
                        <version>${eclipselink.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build> 
    
<h2 id = "1.4"></h2>

### Spring configuration
**JpaBaseConfiguration is an abstract class which defines beans for JPA** in Spring Boot. Spring  provides a configuration implementation for Hibernate out of the box called HibernateJpaAutoConfiguration. However, for EclipseLink, we have to create a custom configuration.To customize it, we have to implement some methods like createJpaVendorAdapter() or getVendorProperties().
First, we need to implement the createJpaVendorAdapter() method which specifies the JPA implementation to use.
Also, we have to define some vendor-specific properties which will be used by EclipseLink.We can add these via the getVendorProperties() method.
**Add following code as a class to org.apache.griffin.core.config package in Apache Griffin project.**
   

        @Configuration
        @ComponentScan("org.apache.griffin.core")
        public class EclipseLinkJpaConfig extends JpaBaseConfiguration {
            protected EclipseLinkJpaConfig(DataSource ds, JpaProperties properties,
                                           ObjectProvider<JtaTransactionManager> jtm,
                                           ObjectProvider<TransactionManagerCustomizers> tmc) {
                super(ds, properties, jtm, tmc);
            }
        
            @Override
            protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
                return new EclipseLinkJpaVendorAdapter();
            }
        
            @Override
            protected Map<String, Object> getVendorProperties() {
                Map<String, Object> map = new HashMap<>();
                map.put(PersistenceUnitProperties.WEAVING, "false");
                map.put(PersistenceUnitProperties.DDL_GENERATION, "create-or-extend-tables");
                return map;
            }
        }

<h2 id = "1.5"></h2>

#### Configure properties
You need to configure properties according to the database you use in Apache Griffin.
Please see [Mysql and postgresql switch](https://github.com/apache/griffin/blob/master/griffin-doc/service/mysql_postgresql_switch.md) to configure.

<h2 id = "0.1"></h2>

## Migration from EclipseLink to Hibernate
Here we'll go through steps necessary to migrate applications from using EclipseLink JPA to using Hibernate JPA.The migration will not need to convert any EclipseLink annotations to Hibernate annotations in application code. 

## Quick use
In Apache Griffin, we provide **hibernate_mysql_pom.xml** file for hibernate and mysql. If you want to quick use hibernate and mysql with jar, firstly you should [configure properties](#2.3) and then use command `mvn clean package -f pom_hibernate.xml` to package jar.

## Migration main steps
- [add hibernate dependency](#2.1)
- [remove EclipseLink](#2.2)
- [configure properties](#2.3)

<h2 id = "2.1"></h2>

### Add hibernate dependency
By default, Spring Data uses Hibernate as the default JPA implementation provider.So we just add **spring-boot-starter-data-jpa** dependency.**If you have already added it, skip this step.**

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

<h2 id = "2.2"></h2>

### Remove EclipseLink dependency
**If you don't want to remove EclipseLink,you can skip this step.**

- remove EclipseLink dependency 

        <dependency>
            <groupId>org.eclipse.persistence</groupId>
            <artifactId>org.eclipse.persistence.jpa</artifactId>
            <version>${eclipselink.version}</version>
        </dependency>

- remove EclipseLink static weaving

        <plugin>
            <groupId>com.ethlo.persistence.tools</groupId>
            <artifactId>eclipselink-maven-plugin</artifactId>
            <version>2.7.0</version>
            <executions>
                <execution>
                    <phase>process-classes</phase>
                    <goals>
                        <goal>weave</goal>
                    </goals>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>org.eclipse.persistence</groupId>
                    <artifactId>org.eclipse.persistence.jpa</artifactId>
                    <version>${eclipselink.version}</version>
                </dependency>
            </dependencies>
        </plugin>

- remove EclipseLinkJpaConfig class

  remove EclipseLinkJpaConfig class in org.apache.griffin.core.config package.  

<h2 id = "2.3"></h2>

#### Configure properties
You need to configure properties according to the database you use in Apache Griffin.
Please see [Mysql and postgresql switch](https://github.com/apache/griffin/blob/master/griffin-doc/service/mysql_postgresql_switch.md) to configure.
