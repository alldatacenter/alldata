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

package org.apache.atlas.notification.preprocessor;

import org.apache.atlas.kafka.AtlasKafkaMessage;
import org.apache.atlas.kafka.KafkaNotification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.utils.AtlasPathExtractorUtil;
import org.apache.atlas.utils.PathExtractorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import org.apache.hadoop.fs.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class AWSS3V2PreprocessorTest {

    private static final Logger LOG = LoggerFactory.getLogger(AWSS3V2PreprocessorTest.class);
    private static final String METADATA_NAMESPACE          = "cm";
    private static final String QNAME_METADATA_NAMESPACE    = '@' + METADATA_NAMESPACE;
    private static final String AWS_S3_MODEL_VERSION_V2     = "V2";

    private static final String SCHEME_SEPARATOR            = "://";
    private static final String S3_SCHEME                   = "s3" + SCHEME_SEPARATOR;
    private static final String S3A_SCHEME                  = "s3a" + SCHEME_SEPARATOR;
    private static final String ABFS_SCHEME                 = "abfs" + SCHEME_SEPARATOR;

    private static final String ATTRIBUTE_NAME              = "name";
    private static final String ATTRIBUTE_OBJECT_PREFIX     = "objectPrefix";
    private static final String ATTRIBUTE_QUALIFIED_NAME    = "qualifiedName";

    private static final List<String>  EMPTY_STR_LIST       = new ArrayList<>();
    private static final List<Pattern> EMPTY_PATTERN_LIST   = new ArrayList<>();

    @Test
    public void testS2V2DirectoryPreprocessorForOtherTypes() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE);
        final String            ABFS_PATH           = ABFS_SCHEME + "data@razrangersan.dfs.core.windows.net/tmp/cdp-demo/sample.csv";
        Path                    path                = new Path(ABFS_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), null);

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertNotEquals(entity.getTypeName(), preprocessor.getTypeName());
    }

    @Test
    public void testS2V2DirectoryPreprocessorForFullPath() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_MODEL_VERSION_V2);
        final String            S3_PATH             = S3A_SCHEME + "aws_bucket1/1234567890/test/data1";
        Path                    path                = new Path(S3_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/test/data1/");

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                                                                        EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                                            false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertEquals(entity.getTypeName(), preprocessor.getTypeName());
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/test/");
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), S3A_SCHEME + "aws_bucket1/1234567890/test/data1/" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "data1");
    }

    @Test
    public void testS2V2DirectoryPreprocessorForRootLevelDirectory() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_MODEL_VERSION_V2);
        final String            S3_PATH             = S3A_SCHEME + "aws_bucket1/root1";
        Path                    path                = new Path(S3_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/root1/");

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertEquals(entity.getTypeName(), preprocessor.getTypeName());
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/");
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), S3A_SCHEME + "aws_bucket1/root1/" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "root1");
    }

    @Test
    public void testS2V2DirectoryPreprocessorWithSameDirNames() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_MODEL_VERSION_V2);
        final String            S3_PATH             = S3_SCHEME + "temp/temp/temp/temp/";
        Path                    path                = new Path(S3_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/temp/temp/temp/");

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertEquals(entity.getTypeName(), preprocessor.getTypeName());
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/temp/temp/");
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), S3_SCHEME + "temp/temp/temp/temp/" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "temp");
    }

    @Test
    public void testS2V2DirectoryPreprocessorForHookWithCorrectObjectPrefix() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_MODEL_VERSION_V2);
        final String            S3_PATH             = S3A_SCHEME + "aws_bucket1/root1";
        Path                    path                = new Path(S3_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        entity.setAttribute(ATTRIBUTE_OBJECT_PREFIX, "/");
        assertNotEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/root1/");

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertEquals(entity.getTypeName(), preprocessor.getTypeName());
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/");
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), S3A_SCHEME + "aws_bucket1/root1/" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "root1");
    }

    @Test
    public void testS2V2DirectoryPreprocessorForHookWithCorrectObjectPrefixHavingSameDirNames() {
        PathExtractorContext    extractorContext    = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_MODEL_VERSION_V2);
        final String            S3_PATH             = S3A_SCHEME + "temp/temp/temp/temp/";
        Path                    path                = new Path(S3_PATH);
        AtlasEntityWithExtInfo  entityWithExtInfo   = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity             entity              = entityWithExtInfo.getEntity();

        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/temp/temp/temp/");
        entity.setAttribute(ATTRIBUTE_OBJECT_PREFIX, "/temp/temp/");
        assertNotEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/temp/temp/temp/");

        HookNotification                    hookNotification    = new HookNotification.EntityCreateRequestV2("test", new AtlasEntity.AtlasEntitiesWithExtInfo(entity));
        AtlasKafkaMessage<HookNotification> kafkaMsg            = new AtlasKafkaMessage(hookNotification, -1, KafkaNotification.ATLAS_HOOK_TOPIC, -1);
        PreprocessorContext                 context             = new PreprocessorContext(kafkaMsg, null, EMPTY_PATTERN_LIST, EMPTY_PATTERN_LIST, null,
                EMPTY_STR_LIST, EMPTY_STR_LIST, EMPTY_STR_LIST, false,
                false, true, false, null);

        EntityPreprocessor                  preprocessor        = new AWSS3V2Preprocessor.AWSS3V2DirectoryPreprocessor();

        preprocessor.preprocess(entity, context);

        assertEquals(entity.getTypeName(), preprocessor.getTypeName());
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/temp/temp/");
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), S3A_SCHEME + "temp/temp/temp/temp/" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "temp");
    }
}
