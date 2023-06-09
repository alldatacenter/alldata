/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.mongo;


public interface MongoTestConstants {
  String LOCALHOST = "localhost";
  // TODO: DRILL-3934: add some randomization to this as it fails when running concurrent builds
  int CONFIG_SERVER_1_PORT = 61114;
  int CONFIG_SERVER_2_PORT = 61215;
  int CONFIG_SERVER_3_PORT = 61316;
  int MONGOD_1_PORT = 27020;
  int MONGOD_2_PORT = 27021;
  int MONGOD_3_PORT = 27022;

  int MONGOD_4_PORT = 27023;
  int MONGOD_5_PORT = 27024;
  int MONGOD_6_PORT = 27025;

  int MONGOS_PORT = 27017;

  String EMPLOYEE_DB = "employee";
  String AUTHENTICATION_DB = "admin";
  String DONUTS_DB = "donuts";
  String DATATYPE_DB = "datatype";
  String ISSUE7820_DB = "ISSUE7820"; // capital letters

  String DONUTS_COLLECTION = "donuts";
  String EMPINFO_COLLECTION = "empinfo";
  String EMPTY_COLLECTION = "empty";
  String SCHEMA_CHANGE_COLLECTION = "schema_change";
  String DATATYPE_COLLECTION = "types";
  String ISSUE7820_COLLECTION = "Issue7820";

  String DONUTS_DATA = "donuts.json";
  String EMP_DATA = "emp.json";
  String SCHEMA_CHANGE_DATA = "schema_change_int_to_string.json";

  String STORAGE_ENGINE = "wiredTiger";
  String DATATYPE_DATA = "datatype-oid.json";

  String CONFIG_REPLICA_SET = "config_replicas";
  String REPLICA_SET_1_NAME = "shard_1_replicas";
  String REPLICA_SET_2_NAME = "shard_2_replicas";

  // test queries
  String TEST_QUERY_1 = "SELECT * FROM mongo.employee.`empinfo` limit 5";
  String TEST_QUERY_LIMIT = "SELECT first_name, last_name FROM mongo.%s.`%s` limit 2";
  String TEST_LIMIT_QUERY = "select `employee_id` from mongo.%s.`%s` limit %d";



  // test query template1
  String TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_1 = "SELECT `employee_id` FROM mongo.%s.`%s`";
  String TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_2 = "select `employee_id`, `rating`, coalesce(`full_name`, 'Bob') from mongo.%s.`%s`";
  String TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_3 = "select * from mongo.%s.`%s`";
  String TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_4 = "select coalesce(`position_id`, -1) position_id_or_default from mongo.%s.`%s`";
  String TEST_FILTER_PUSH_DOWN_IS_NULL_QUERY_TEMPLATE_1 = "SELECT `employee_id` FROM mongo.%s.`%s` where position_id is null";
  String TEST_FILTER_PUSH_DOWN_IS_NOT_NULL_QUERY_TEMPLATE_1 = "SELECT `employee_id` FROM mongo.%s.`%s` where position_id is not null";
  String TEST_FILTER_PUSH_DOWN_EQUAL_QUERY_TEMPLATE_1 = "SELECT `full_name` FROM mongo.%s.`%s` where rating = 52.17";
  String TEST_FILTER_PUSH_DOWN_NOT_EQUAL_QUERY_TEMPLATE_1 = "SELECT `employee_id` FROM mongo.%s.`%s` where rating != 52.17";
  String TEST_FILTER_PUSH_DOWN_LESS_THAN_QUERY_TEMPLATE_1 = "SELECT `full_name` FROM mongo.%s.`%s` where rating < 52.17";
  String TEST_FILTER_PUSH_DOWN_GREATER_THAN_QUERY_TEMPLATE_1 = "SELECT `full_name` FROM mongo.%s.`%s` where rating > 52.17";
  String TEST_EMPTY_TABLE_QUERY_TEMPLATE = "select count(*) from mongo.%s.`%s`";

  String TEST_BOOLEAN_FILTER_QUERY_TEMPLATE1 = "select `employee_id` from mongo.%s.`%s` where isFTE = true";
  String TEST_BOOLEAN_FILTER_QUERY_TEMPLATE2 = "select `employee_id` from mongo.%s.`%s` where isFTE = false";
  String TEST_BOOLEAN_FILTER_QUERY_TEMPLATE3 = "select `employee_id` from mongo.%s.`%s` where position_id = 16 and isFTE = true";
  String TEST_BOOLEAN_FILTER_QUERY_TEMPLATE4 = "select `employee_id` from mongo.%s.`%s` where (position_id = 16 and isFTE = true) or last_name = 'Yonce'";

  String TEST_STAR_QUERY_UNSHARDED_DB = "select * from mongo.%s.`%s`";
  // This query is invalid. See DRILL-7420. Topping is a repeated map.
  // Drill should not allow projecting a repeated map to the top level; this should
  // require a flatten or lateral query.
  String TEST_STAR_QUERY_UNSHARDED_DB_PROJECT_FILTER = "select t.name as name, t.topping.type as type from mongo.%s.`%s` t where t.sales >= 150";
  String TEST_STAR_QUERY_UNSHARDED_DB_GROUP_PROJECT_FILTER = "select t.topping.type as type, count(t.topping.type) as typeCount from mongo.%s.`%s` t group by t.topping.type order by typeCount";
}
