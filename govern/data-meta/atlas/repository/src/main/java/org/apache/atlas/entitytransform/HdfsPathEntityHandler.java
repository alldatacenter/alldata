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
package org.apache.atlas.entitytransform;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

import static org.apache.atlas.entitytransform.TransformationConstants.CLUSTER_DELIMITER;
import static org.apache.atlas.entitytransform.TransformationConstants.CLUSTER_NAME_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.HDFS_CLUSTER_NAME_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.HDFS_PATH;
import static org.apache.atlas.entitytransform.TransformationConstants.HDFS_PATH_PATH_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.HDFS_PATH_NAME_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.NAME_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.PATH_ATTRIBUTE;
import static org.apache.atlas.entitytransform.TransformationConstants.QUALIFIED_NAME_ATTRIBUTE;


public class HdfsPathEntityHandler extends BaseEntityHandler {
    static final List<String> CUSTOM_TRANSFORM_ATTRIBUTES = Arrays.asList(HDFS_PATH_NAME_ATTRIBUTE, HDFS_PATH_PATH_ATTRIBUTE, HDFS_CLUSTER_NAME_ATTRIBUTE);

    public HdfsPathEntityHandler(List<AtlasEntityTransformer> transformers) {
        super(transformers);
    }

    @Override
    public AtlasTransformableEntity getTransformableEntity(AtlasEntity entity) {
        return isHdfsPathEntity(entity) ? new HdfsPathEntity(entity) : null;
    }

    private boolean isHdfsPathEntity(AtlasEntity entity) {
        return StringUtils.equals(entity.getTypeName(), HDFS_PATH);
    }


    public static class HdfsPathEntity extends AtlasTransformableEntity {
        private String  clusterName;
        private String  path;
        private String  name;
        private String  pathPrefix;
        private boolean isPathUpdated            = false;
        private boolean isCustomAttributeUpdated = false;


        public HdfsPathEntity(AtlasEntity entity) {
            super(entity);

            this.path = (String) entity.getAttribute(PATH_ATTRIBUTE);
            this.name = (String) entity.getAttribute(NAME_ATTRIBUTE);

            String qualifiedName = (String) entity.getAttribute(QUALIFIED_NAME_ATTRIBUTE);

            if (qualifiedName != null) {
                int clusterSeparatorIdx = qualifiedName.lastIndexOf(CLUSTER_DELIMITER);

                if (clusterSeparatorIdx != -1) {
                    this.clusterName = qualifiedName.substring(clusterSeparatorIdx + 1);
                } else {
                    this.clusterName = "";
                }

                if (StringUtils.isNotEmpty(path) && StringUtils.isNotEmpty(name)) {
                    int idx = path.indexOf(name);

                    if (idx != -1) {
                        this.pathPrefix = path.substring(0, idx);
                    } else {
                        this.pathPrefix = "";
                    }
                }
            } else {
                this.clusterName = "";
                this.pathPrefix  = "";
            }
        }

        @Override
        public Object getAttribute(EntityAttribute attribute) {
            switch (attribute.getAttributeKey()) {
                case HDFS_CLUSTER_NAME_ATTRIBUTE:
                    return clusterName;

                case HDFS_PATH_NAME_ATTRIBUTE:
                    return name;

                case HDFS_PATH_PATH_ATTRIBUTE:
                    return path;
            }

            return super.getAttribute(attribute);
        }

        @Override
        public void setAttribute(EntityAttribute attribute, String attributeValue) {
            switch (attribute.getAttributeKey()) {
                case HDFS_CLUSTER_NAME_ATTRIBUTE:
                    clusterName = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HDFS_PATH_NAME_ATTRIBUTE:
                    name = attributeValue;

                    isCustomAttributeUpdated = true;
                break;

                case HDFS_PATH_PATH_ATTRIBUTE:
                    path = attributeValue;

                    isPathUpdated              = true;
                    isCustomAttributeUpdated = true;
                break;

                default:
                    super.setAttribute(attribute, attributeValue);
                break;
            }
        }

        @Override
        public void transformComplete() {
            if (isCustomAttributeUpdated) {
                entity.setAttribute(CLUSTER_NAME_ATTRIBUTE, clusterName);
                entity.setAttribute(NAME_ATTRIBUTE, name);
                entity.setAttribute(PATH_ATTRIBUTE, toPath());
                entity.setAttribute(QUALIFIED_NAME_ATTRIBUTE, toQualifiedName());
            }
        }


        private String toQualifiedName() {
            return StringUtils.isEmpty(clusterName) ? toPath() : String.format("%s@%s", toPath(), clusterName);
        }

        private String toPath() {
            final String ret;

            if (isPathUpdated) {
                ret = path;
            } else {
                if (StringUtils.isNotEmpty(pathPrefix)) {
                    ret = pathPrefix + name;
                } else {
                    ret = name;
                }
            }

            return ret;
        }
    }
}