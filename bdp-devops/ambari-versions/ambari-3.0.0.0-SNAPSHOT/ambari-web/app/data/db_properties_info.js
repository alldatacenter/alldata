/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

module.exports = {
  /**
   * object that shows property names for different kind of config properties
   * - radio button property (db_selector) - main config on which can depend other core properties
   * - properties used to generate connection url (host_name, db_name)
   * - properties that should be updated on radio buttons change (connection_url, driver, db_type, sql_jar_connector)
   * @see <code>setConnectionUrl<code> for details for connection_url
   * - properties that should be hidden in special case (user_name, password)
   * @see <code>handleSpecialUserPassProperties<code> method
   * @type {object}
   */
  dpPropertiesByServiceMap: {
    'HIVE': {
      db_selector: 'hive_database',
      db_name: 'ambari.hive.db.schema.name',

      connection_url: 'javax.jdo.option.ConnectionURL',
      db_type: 'hive_database_type',
      driver: 'javax.jdo.option.ConnectionDriverName',

      user_name: 'javax.jdo.option.ConnectionUserName',
      password: 'javax.jdo.option.ConnectionPassword'
    },
    'OOZIE': {
      db_selector: 'oozie_database',
      db_name: 'oozie.db.schema.name',

      connection_url: 'oozie.service.JPAService.jdbc.url',
      driver: 'oozie.service.JPAService.jdbc.driver',

      user_name: 'oozie.service.JPAService.jdbc.username',
      password: 'oozie.service.JPAService.jdbc.password'
    }
  },

  /**
   * object that shows default(recommended) values for different db type;
   * properties that should have default value ['connection_url', 'driver', 'sql_jar_connector', 'db_type']
   * some properties can be skipped but be sure they are not used.
   * @type {object}
   */
  dpPropertiesMap: {
    'MYSQL': {
      'connection_url': 'jdbc:mysql://{0}/{1}',
      /** in case property has different default value for specific service it can be overriden in such way **/
      'HIVE': {
        'connection_url': 'jdbc:mysql://{0}/{1}?createDatabaseIfNotExist=true'
      },
      'driver': 'com.mysql.jdbc.Driver',
      'sql_jar_connector': '/usr/share/java/mysql-connector-java.jar',
      'db_type': 'mysql',
      'db_name': 'MySQL',
      'driver_download_url': 'https://dev.mysql.com/downloads/connector/j/',
      'driver_name': 'MySQL Connector/J JDBC Driver'
    },
    'POSTGRES': {
      'connection_url': 'jdbc:postgresql://{0}:5432/{1}',
      'driver': 'org.postgresql.Driver',
      'sql_jar_connector': '/usr/share/java/postgresql.jar',
      'db_type': 'postgres',
      'db_name': 'PostgreSQL',
      'driver_download_url': 'https://jdbc.postgresql.org/',
      'driver_name': 'PostgreSQL JDBC Driver'
    },
    'ORACLE': {
      'connection_url': 'jdbc:oracle:thin:@//{0}:1521/{1}',
      'driver': 'oracle.jdbc.driver.OracleDriver',
      'sql_jar_connector': '/usr/share/java/ojdbc6.jar',
      'db_type': 'oracle',
      'db_name': 'Oracle',
      'driver_download_url': 'http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html',
      'driver_name': 'Oracle JDBC Driver'
    },
    'MSSQL': {
      'connection_url': 'jdbc:sqlserver://{0};databaseName={1}',
      'driver': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
      'sql_jar_connector': '/usr/share/java/sqljdbc4.jar',
      'db_type': 'mssql'
    },
    'MSSQL2': {
      'connection_url': 'jdbc:sqlserver://{0};databaseName={1};integratedSecurity=true',
      'driver': 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
      'sql_jar_connector': '/usr/share/java/sqljdbc4.jar',
      'db_type': 'mssql'
    },
    /** TODO: Remove SQLA from the list of databases once Ranger DB_FLAVOR=SQLA is replaced with SQL Anywhere */
    'SQLA': {
      'connection_url': 'jdbc:sqlanywhere:host={0};database={1}',
      'driver': 'sap.jdbc4.sqlanywhere.IDriver',
      'sql_jar_connector': '/path_to_driver/sqla-client-jdbc.tar.gz',
      'db_type': 'sqlanywhere'
    },
    'ANYWHERE': {
      'connection_url': 'jdbc:sqlanywhere:host={0};database={1}',
      'driver': 'sap.jdbc4.sqlanywhere.IDriver',
      'sql_jar_connector': '/path_to_driver/sqla-client-jdbc.tar.gz',
      'db_type': 'sqlanywhere'
    },
    'DERBY': {
      'connection_url': 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true',
      'driver': 'org.apache.derby.jdbc.EmbeddedDriver',
      'db_type': 'derby'
    }
  }
};
