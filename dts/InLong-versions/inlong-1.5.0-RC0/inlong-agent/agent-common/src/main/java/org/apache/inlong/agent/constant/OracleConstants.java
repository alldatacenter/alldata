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

public class OracleConstants {

    /**
     * The snapshot includes the structure and data of the captured tables.
     * Specify this value to populate topics with a complete representation of the data from the captured tables.
     */
    public static final String INITIAL = "initial";

    /**
     * The snapshot includes the structure and data of the captured tables.
     * The connector performs an initial snapshot and then stops, without processing any subsequent changes.
     */
    public static final String INITIAL_ONLY = "initial_only";

    /**
     * The snapshot includes only the structure of captured tables.
     * Specify this value if you want the connector to capture data only for changes that occur after the snapshot.
     */
    public static final String SCHEMA_ONLY = "schema_only";

    /**
     * This is a recovery setting for a connector that has already been capturing changes.
     * When you restart the connector, this setting enables recovery of a corrupted or lost database history topic.
     * You might set it periodically to "clean up" a database history topic that has been growing unexpectedly.
     * Database history topics require infinite retention. Note this mode is only safe to be used when it is guaranteed
     * that no schema changes happened since the point in time the connector was shut down before and the point in time
     * the snapshot is taken.
     */
    public static final String SCHEMA_ONLY_RECOVERY = "schema_only_recovery";

}
