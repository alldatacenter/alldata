/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.protocol.constant;

/**
 * StarRocks options constant
 */
public class StarRocksConstant {

    /**
     * 'connector' = 'starrocks-inlong'
     */
    public static final String CONNECTOR = "connector";

    /**
     * Host of the stream load like: `jdbc:mysql://fe_ip1:query_port,fe_ip2:query_port...`.
     */
    public static final String JDBC_URL = "jdbc-url";

    /**
     * Host of the stream load like: `fe_ip1:http_port;fe_ip2:http_port;fe_ip3:http_port`.
     */
    public static final String LOAD_URL = "load-url";

    /**
     * StarRocks user name.
     */
    public static final String USERNAME = "username";

    /**
     * StarRocks user password.
     */
    public static final String PASSWORD = "password";

    /**
     * StarRocks stream load format, support json and csv.
     */
    public static final String FORMAT = "sink.properties.format";

    /**
     * StarRocks stream load strip outer array for json format.
     */
    public static final String STRIP_OUTER_ARRAY = "sink.properties.strip_outer_array";

    /**
     * Database name of the stream load.
     */
    public static final String DATABASE_NAME = "database-name";

    /**
     * Table name of the stream load.
     */
    public static final String TABLE_NAME = "table-name";

}
