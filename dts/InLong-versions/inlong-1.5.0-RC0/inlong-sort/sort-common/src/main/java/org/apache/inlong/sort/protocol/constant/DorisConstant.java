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
 * Doris options constant
 */
public class DorisConstant {

    /**
     * 'connector' = 'doris'
     */
    public static final String CONNECTOR = "connector";

    /**
     * Doris FE http address, support multiple addresses, separated by commas
     */
    public static final String FE_NODES = "fenodes";

    /**
     * Doris table identifier, eg, db1.tbl1
     */
    public static final String TABLE_IDENTIFIER = "table.identifier";

    /**
     * Doris username
     */
    public static final String USERNAME = "username";

    /**
     * Doris password
     */
    public static final String PASSWORD = "password";

    /**
     * The multiple enable of sink
     */
    public static final String SINK_MULTIPLE_ENABLE = "sink.multiple.enable";

    /**
     * The multiple format of sink
     */
    public static final String SINK_MULTIPLE_FORMAT = "sink.multiple.format";

    /**
     * The multiple database-pattern of sink
     */
    public static final String SINK_MULTIPLE_DATABASE_PATTERN = "sink.multiple.database-pattern";
    /**
     * The multiple table-pattern of sink
     */
    public static final String SINK_MULTIPLE_TABLE_PATTERN = "sink.multiple.table-pattern";
}
