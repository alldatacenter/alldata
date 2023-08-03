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

package org.apache.atlas.repository.impexp;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.repository.impexp.AuditsWriter.getCurrentClusterName;

@Component
public class HdfsPathEntityCreator {
    protected static final Logger LOG = LoggerFactory.getLogger(HdfsPathEntityCreator.class);

    public static final String HDFS_PATH_TYPE = "hdfs_path";
    public static final String HDFS_PATH_ATTRIBUTE_NAME_NAME = "name";
    public static final String HDFS_PATH_ATTRIBUTE_NAME_CLUSTER_NAME = "clusterName";
    public static final String HDFS_PATH_ATTRIBUTE_NAME_PATH = "path";
    public static final String HDFS_PATH_ATTRIBUTE_QUALIFIED_NAME = "qualifiedName";

    private static final String QUALIFIED_NAME_FORMAT = "%s@%s";
    private final String PATH_SEPARATOR = "/";

    private AtlasTypeRegistry typeRegistry;
    private AtlasEntityStoreV2 entityStore;

    @Inject
    public HdfsPathEntityCreator(AtlasTypeRegistry typeRegistry, AtlasEntityStoreV2 entityStore) {
        this.typeRegistry = typeRegistry;
        this.entityStore = entityStore;
    }

    public AtlasEntity.AtlasEntityWithExtInfo getCreateEntity(AtlasObjectId item) throws AtlasBaseException {
        if(item.getUniqueAttributes() == null || !item.getUniqueAttributes().containsKey(HDFS_PATH_ATTRIBUTE_NAME_PATH)) {
            return null;
        }

        return getCreateEntity((String) item.getUniqueAttributes().get(HDFS_PATH_ATTRIBUTE_NAME_PATH));
    }

    public AtlasEntity.AtlasEntityWithExtInfo getCreateEntity(String path) throws AtlasBaseException {
        return getCreateEntity(path, getCurrentClusterName());
    }

    public AtlasEntity.AtlasEntityWithExtInfo getCreateEntity(String path, String clusterName) throws AtlasBaseException {
        String pathWithTrailingSeparator = getPathWithTrailingSeparator(path);
        AtlasEntityType hdfsPathEntityType = getHdfsPathEntityType();
        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = getHDFSPathEntity(hdfsPathEntityType, pathWithTrailingSeparator, clusterName);
        if(entityWithExtInfo != null) {
            return entityWithExtInfo;
        }

        AtlasEntity entity = createHDFSPathEntity(hdfsPathEntityType, pathWithTrailingSeparator, clusterName);
        AtlasEntityStream entityStream = new AtlasEntityStream(entity);
        EntityMutationResponse entityMutationResponse = entityStore.createOrUpdate(entityStream, false);
        if(entityMutationResponse.getCreatedEntities().size() == 0) {
            return null;
        }

        return getHDFSPathEntity(hdfsPathEntityType, pathWithTrailingSeparator, clusterName);
    }

    private AtlasEntity createHDFSPathEntity(AtlasEntityType hdfsPathEntityType, String path, String clusterName) {
        AtlasEntity entity = hdfsPathEntityType.createDefaultValue();

        entity.setAttribute(HDFS_PATH_ATTRIBUTE_QUALIFIED_NAME, getQualifiedName(path, clusterName));
        entity.setAttribute(HDFS_PATH_ATTRIBUTE_NAME_PATH, path);
        entity.setAttribute(HDFS_PATH_ATTRIBUTE_NAME_NAME, path);
        entity.setAttribute(HDFS_PATH_ATTRIBUTE_NAME_CLUSTER_NAME, clusterName);

        return entity;
    }

    private AtlasEntity.AtlasEntityWithExtInfo getHDFSPathEntity(AtlasEntityType hdfsPathEntityType, String path, String clusterName) {
        try {
            return entityStore.getByUniqueAttributes(hdfsPathEntityType, getUniqueAttributes(path, clusterName));
        } catch (AtlasBaseException e) {
            return null;
        }
    }

    private AtlasEntityType getHdfsPathEntityType() throws AtlasBaseException {
        return (AtlasEntityType) typeRegistry.getType(HDFS_PATH_TYPE);
    }

    private Map<String,Object> getUniqueAttributes(String path, String clusterName) {
        Map<String,Object>  ret = new HashMap<String, Object>();
        ret.put(HDFS_PATH_ATTRIBUTE_QUALIFIED_NAME, getQualifiedName(path, clusterName));
        return ret;
    }

    private String getPathWithTrailingSeparator(String path) {
        if(path.endsWith(PATH_SEPARATOR)) {
            return path;
        }

        return path + PATH_SEPARATOR;
    }

    public static String getQualifiedName(String path, String clusterName) {
        return String.format(QUALIFIED_NAME_FORMAT, path, clusterName);
    }
}
