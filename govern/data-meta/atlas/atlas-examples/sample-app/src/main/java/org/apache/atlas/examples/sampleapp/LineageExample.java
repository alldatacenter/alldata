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
package org.apache.atlas.examples.sampleapp;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.lineage.AtlasLineageInfo;

import java.util.Map;
import java.util.Set;

public class LineageExample {
    private AtlasClientV2 atlasClient;

    LineageExample(AtlasClientV2 atlasClient) {
        this.atlasClient = atlasClient;
    }

    public void lineage(String guid) throws AtlasServiceException {
        AtlasLineageInfo                      lineageInfo   = atlasClient.getLineageInfo(guid, AtlasLineageInfo.LineageDirection.BOTH, 0);
        Set<AtlasLineageInfo.LineageRelation> relations     = lineageInfo.getRelations();
        Map<String, AtlasEntityHeader>        guidEntityMap = lineageInfo.getGuidEntityMap();

        for (AtlasLineageInfo.LineageRelation relation : relations) {
            AtlasEntityHeader fromEntity = guidEntityMap.get(relation.getFromEntityId());
            AtlasEntityHeader toEntity   = guidEntityMap.get(relation.getToEntityId());

            SampleApp.log(fromEntity.getDisplayText() + "(" + fromEntity.getTypeName() + ") -> " +
                          toEntity.getDisplayText() + "(" + toEntity.getTypeName() + ")");
        }
    }
}