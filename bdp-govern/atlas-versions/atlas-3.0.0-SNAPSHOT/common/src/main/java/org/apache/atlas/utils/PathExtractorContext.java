/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.utils;

import org.apache.atlas.model.instance.AtlasEntity;

import java.util.HashMap;
import java.util.Map;

public class PathExtractorContext {
    private final String                   metadataNamespace;
    private final Map<String, AtlasEntity> knownEntities;
    private final boolean                  isConvertPathToLowerCase;
    private final String                   awsS3AtlasModelVersion;

    public PathExtractorContext(String metadataNamespace) {
        this(metadataNamespace, new HashMap<>(), false, null) ;
    }

    public PathExtractorContext(String metadataNamespace, String awsS3AtlasModelVersion) {
        this(metadataNamespace, new HashMap<>(), false, awsS3AtlasModelVersion) ;
    }

    public PathExtractorContext(String metadataNamespace, boolean isConvertPathToLowerCase, String awsS3AtlasModelVersion) {
        this(metadataNamespace, new HashMap<>(), isConvertPathToLowerCase, awsS3AtlasModelVersion) ;
    }

    public PathExtractorContext(String metadataNamespace, Map<String, AtlasEntity> knownEntities, boolean isConvertPathToLowerCase, String awsS3AtlasModelVersion) {
        this.metadataNamespace        = metadataNamespace;
        this.knownEntities            = knownEntities;
        this.isConvertPathToLowerCase = isConvertPathToLowerCase;
        this.awsS3AtlasModelVersion   = awsS3AtlasModelVersion;
    }

    public String getMetadataNamespace() {
        return metadataNamespace;
    }

    public Map<String, AtlasEntity> getKnownEntities() {
        return knownEntities;
    }

    public void putEntity(String qualifiedName, AtlasEntity entity) {
        knownEntities.put(qualifiedName, entity);
    }

    public AtlasEntity getEntity(String qualifiedName) {
        return knownEntities.get(qualifiedName);
    }

    public boolean isConvertPathToLowerCase() {
        return isConvertPathToLowerCase;
    }

    public String getAwsS3AtlasModelVersion() {
        return awsS3AtlasModelVersion;
    }
}