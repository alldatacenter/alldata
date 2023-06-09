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
CREATE KEYSPACE test_keyspace
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE test_keyspace.employee (
    employee_id bigint,
    full_name text,
    first_name text,
    last_name text,
    position_id int,
    position_title text,
    store_id int,
    department_id int,
    birth_date text,
    hire_date text,
    salary float,
    supervisor_id int,
    education_level text,
    marital_status text,
    gender text,
    management_role text,
    ascii_field ascii,
    blob_field blob,
    boolean_field boolean,
    date_field date,
    decimal_field decimal,
    double_field double,
    duration_field duration,
    inet_field inet,
    time_field time,
    timestamp_field timestamp,
    timeuuid_field timeuuid,
    uuid_field uuid,
    varchar_field varchar,
    varint_field varint,
    PRIMARY KEY (full_name, employee_id)
) WITH CLUSTERING ORDER BY (employee_id ASC);

USE test_keyspace;

INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role, ascii_field, blob_field, boolean_field, date_field, decimal_field, double_field, duration_field, inet_field, time_field,  timestamp_field, timeuuid_field, uuid_field, varchar_field, varint_field)
 VALUES (1, 'Sheri Nowmer', 'Sheri', 'Nowmer', 1, 'President',0,1, '1961-08-26', '1994-12-01 00:00:00.0',80000.0 ,0, 'Graduate Degree', 'S', 'F', 'Senior Management', 'abc', bigintAsBlob(3), true, '2011-02-03', 123.456, 321.123, 3d89h4m48s, '8.8.8.8', '04:05:00',  '2011-02-03 04:05:00', 50554d6e-29bb-11e5-b345-feff819cdc9f, 50554d6e-29bb-11e5-b345-feff819cdc9f, 'abc', 123);
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (2, 'Derrick Whelply', 'Derrick', 'Whelply', 2, 'VP Country Manager',0,1, '1915-07-03', '1994-12-01 00:00:00.0',40000.0 ,1, 'Graduate Degree', 'M', 'M', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (4, 'Michael Spence', 'Michael', 'Spence', 2, 'VP Country Manager',0,1, '1969-06-20', '1998-01-01 00:00:00.0',40000.0 ,1, 'Graduate Degree', 'S', 'M', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (5, 'Maya Gutierrez', 'Maya', 'Gutierrez', 2, 'VP Country Manager',0,1, '1951-05-10', '1998-01-01 00:00:00.0',35000.0 ,1, 'Bachelors Degree', 'M', 'F', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (6, 'Roberta Damstra', 'Roberta', 'Damstra', 3, 'VP Information Systems',0,2, '1942-10-08', '1994-12-01 00:00:00.0',25000.0 ,1, 'Bachelors Degree', 'M', 'F', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (7, 'Rebecca Kanagaki', 'Rebecca', 'Kanagaki', 4, 'VP Human Resources',0,3, '1949-03-27', '1994-12-01 00:00:00.0',15000.0 ,1, 'Bachelors Degree', 'M', 'F', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (8, 'Kim Brunner', 'Kim', 'Brunner', 11, 'Store Manager',9,11, '1922-08-10', '1998-01-01 00:00:00.0',10000.0 ,5, 'Bachelors Degree', 'S', 'F', 'Store Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (9, 'Brenda Blumberg', 'Brenda', 'Blumberg', 11, 'Store Manager',21,11, '1979-06-23', '1998-01-01 00:00:00.0',17000.0 ,5, 'Graduate Degree', 'M', 'F', 'Store Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (10, 'Darren Stanz', 'Darren', 'Stanz', 5, 'VP Finance',0,5, '1949-08-26', '1994-12-01 00:00:00.0',50000.0 ,1, 'Partial College', 'M', 'M', 'Senior Management');
INSERT INTO employee(employee_id, full_name, first_name, last_name, position_id, position_title, store_id, department_id, birth_date, hire_date, salary, supervisor_id, education_level, marital_status, gender, management_role)
 VALUES (11, 'Jonathan Murraiin', 'Jonathan', 'Murraiin', 11, 'Store Manager',1,11, '1967-06-20', '1998-01-01 00:00:00.0',15000.0 ,5, 'Graduate Degree', 'S', 'M', 'Store Management');

CREATE TABLE test_keyspace.nation (
    n_nationkey bigint,
    n_name text,
    n_regionkey int,
    PRIMARY KEY (n_nationkey, n_regionkey)
) WITH CLUSTERING ORDER BY (n_regionkey ASC);

INSERT INTO nation(n_nationkey, n_name, n_regionkey)
VALUES (0, 'ALGERIA', 1);

CREATE TABLE arr (
    f_int int PRIMARY KEY,
    string_arr list<text>,
    int_arr list<int>,
    int_set set<int>
);

INSERT INTO arr (f_int, string_arr, int_arr, int_set)
VALUES (0, ['a', 'b', 'c', 'd'], [1, 2, 3, 4, 0], {9, 8, 7, 6, 5});

CREATE TABLE map (
    prim_field int PRIMARY KEY,
    nest_field map<text, text>,
    more_nest_field map<text, frozen<map<text, text>>>,
    map_arr list<frozen<map<text, int>>>
);

INSERT INTO map (prim_field, nest_field, more_nest_field, map_arr)
VALUES (0, {'a':'123', 'b':'abc'}, {'a':{'b':'abc'}}, [{'a':123, 'b':321}, {'c':456, 'd':789}]);

CREATE TABLE tuple (
    prim_field int PRIMARY KEY,
    tuple_field tuple<int, text, double>
);
