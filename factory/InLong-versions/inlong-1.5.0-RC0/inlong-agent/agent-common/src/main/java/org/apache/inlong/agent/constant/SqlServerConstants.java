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

package org.apache.inlong.agent.constant;

public class SqlServerConstants {

    /**
     * Takes a snapshot of structure and data of captured tables;
     * useful if topics should be populated with a complete representation of the data from the captured tables.
     */
    public static final String INITIAL = "initial";

    /**
     * Takes a snapshot of structure and data like initial
     * but instead does not transition into streaming changes once the snapshot has completed.
     */
    public static final String INITIAL_ONLY = "initial_only";

    /**
     * Takes a snapshot of the structure of captured tables only;
     * useful if only changes happening from now onwards should be propagated to topics.
     */
    public static final String SCHEMA_ONLY = "schema_only";
}
