/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.discovery;


import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;
import org.apache.atlas.v1.model.lineage.SchemaResponse.SchemaDetails;

public interface AtlasLineageService {
    /**
     * @param entityGuid unique ID of the entity
     * @param direction direction of lineage - INPUT, OUTPUT or BOTH
     * @param depth number of hops in lineage
     * @return AtlasLineageInfo
     */
    AtlasLineageInfo getAtlasLineageInfo(String entityGuid, LineageDirection direction, int depth) throws AtlasBaseException;

    /**
     * Return the schema for the given datasetName.
     *
     * @param datasetName datasetName
     * @return Schema as JSON
     */
    SchemaDetails getSchemaForHiveTableByName(String datasetName) throws AtlasBaseException;

    /**
     * Return the schema for the given entity id.
     *
     * @param guid tableName
     * @return Schema as JSON
     */
    SchemaDetails getSchemaForHiveTableByGuid(String guid) throws AtlasBaseException;
}
