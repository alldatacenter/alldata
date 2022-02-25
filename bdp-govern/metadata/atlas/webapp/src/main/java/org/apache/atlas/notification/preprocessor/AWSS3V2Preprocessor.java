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

package org.apache.atlas.notification.preprocessor;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.hadoop.fs.Path;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AWSS3V2Preprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(AWSS3V2Preprocessor.class);

    private static final String AWS_S3_V2_DIR_TYPE       = "aws_s3_v2_directory";
    private static final String ATTRIBUTE_OBJECT_PREFIX  = "objectPrefix";
    private static final String SCHEME_SEPARATOR             = "://";

    static class AWSS3V2DirectoryPreprocessor extends EntityPreprocessor {
        protected AWSS3V2DirectoryPreprocessor() {
            super(AWS_S3_V2_DIR_TYPE);
        }

        @Override
        public void preprocess(AtlasEntity entity, PreprocessorContext context) {
            if (context.getS3V2DirectoryPruneObjectPrefix()) {
                String qualifiedName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);
                String objectPrefix = (String) entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX);

                if (isObjectPrefixPruneNeeded(qualifiedName, objectPrefix)) {
                    if (objectPrefix.lastIndexOf(Path.SEPARATOR) == -1) {
                        objectPrefix = Path.SEPARATOR;
                    } else {
                        if (doesEndsWithPathSeparator(objectPrefix)) {
                            objectPrefix = removeLastPathSeparator(objectPrefix);
                        }

                        objectPrefix = objectPrefix.substring(0, objectPrefix.lastIndexOf(Path.SEPARATOR) + 1);
                    }

                    LOG.info("Aws S3 V2 Preprocessor: Pruning {} from {} to {}", ATTRIBUTE_OBJECT_PREFIX + QNAME_SEP_CLUSTER_NAME + AWS_S3_V2_DIR_TYPE,
                                entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), objectPrefix);

                    entity.setAttribute(ATTRIBUTE_OBJECT_PREFIX, objectPrefix);
                }
            }
        }

        private boolean isObjectPrefixPruneNeeded(String qualifiedName, String objectPrefix) {
            return (StringUtils.isNotBlank(qualifiedName)
                    && StringUtils.isNotBlank(objectPrefix)
                    && qualifiedName.contains(getSchemeAndBucket(qualifiedName) + objectPrefix + QNAME_SEP_CLUSTER_NAME));
        }

        private String getSchemeAndBucket(String qualifiedName) {
            String ret = "";

            if (StringUtils.isNotEmpty(qualifiedName) && qualifiedName.contains(SCHEME_SEPARATOR)) {
                int schemeSeparatorEndPosition = qualifiedName.indexOf(SCHEME_SEPARATOR) + SCHEME_SEPARATOR.length();
                int bucketEndPosition = qualifiedName.indexOf(Path.SEPARATOR, schemeSeparatorEndPosition);
                ret = qualifiedName.substring(0, bucketEndPosition);
            }

            return ret;
        }

        private boolean doesEndsWithPathSeparator(String path) {
            return path.endsWith(Path.SEPARATOR);
        }

        private String removeLastPathSeparator(String path) {
            return StringUtils.chop(path);
        }
    }

}
