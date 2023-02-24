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
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.apache.hadoop.fs.Path;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class AtlasPathExtractorUtilTest {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPathExtractorUtilTest.class);

    // Common
    private static final String METADATA_NAMESPACE          = "metaspace";
    private static final String QNAME_METADATA_NAMESPACE    = '@' + METADATA_NAMESPACE;
    private static final String SCHEME_SEPARATOR            = "://";
    private static final String ATTRIBUTE_NAME              = "name";
    private static final String ATTRIBUTE_QUALIFIED_NAME    = "qualifiedName";

    // HDFS
    private static final String HDFS_PATH_TYPE           = "hdfs_path";
    private static final String ATTRIBUTE_PATH           = "path";
    private static final String ATTRIBUTE_CLUSTER_NAME   = "clusterName";

    // Ozone
    private static final String OZONE_VOLUME      = "ozone_volume";
    private static final String OZONE_BUCKET      = "ozone_bucket";
    private static final String OZONE_KEY         = "ozone_key";
    private static final String OZONE_SCHEME      = "ofs" + SCHEME_SEPARATOR;
    private static final String OZONE_3_SCHEME    = "o3fs" + SCHEME_SEPARATOR;

    // HDFS
    private static final String HDFS_SCHEME    = "hdfs" + SCHEME_SEPARATOR;
    private static final String HDFS_PATH      = HDFS_SCHEME + "host_name:8020/warehouse/tablespace/external/hive/taBlE_306";

    // ADLS Gen2
    private static final String ADLS_GEN2_ACCOUNT       = "adls_gen2_account";
    private static final String ADLS_GEN2_CONTAINER     = "adls_gen2_container";
    private static final String ADLS_GEN2_DIRECTORY     = "adls_gen2_directory";
    private static final String ABFS_SCHEME             = "abfs" + SCHEME_SEPARATOR;
    private static final String ABFSS_SCHEME            = "abfss" + SCHEME_SEPARATOR;
    private static final String ABFS_PATH               = ABFS_SCHEME + "data@razrangersan.dfs.core.windows.net/tmp/cdp-demo/sample.csv";
    private static final String ABFSS_PATH              = ABFSS_SCHEME + "data@razrangersan.dfs.core.windows.net/tmp/cdp-demo/sample.csv";

    // AWS S3
    private static final String AWS_S3_ATLAS_MODEL_VERSION_V2    = "V2";
    private static final String AWS_S3_BUCKET                    = "aws_s3_bucket";
    private static final String AWS_S3_PSEUDO_DIR                = "aws_s3_pseudo_dir";
    private static final String AWS_S3_V2_BUCKET                 = "aws_s3_v2_bucket";
    private static final String AWS_S3_V2_PSEUDO_DIR             = "aws_s3_v2_directory";
    private static final String S3_SCHEME                        = "s3" + SCHEME_SEPARATOR;
    private static final String S3A_SCHEME                       = "s3a" + SCHEME_SEPARATOR;
    private static final String ATTRIBUTE_OBJECT_PREFIX          = "objectPrefix";
    private static final String S3_PATH                          = S3_SCHEME + "aws_my_bucket1/1234567890/renders/Irradiance_A.csv";
    private static final String S3A_PATH                         = S3A_SCHEME + "aws_my_bucket1/1234567890/renders/Irradiance_A.csv";

    // Google Cloud Storage
    private static final String GCS_VIRTUAL_DIR            = "gcp_storage_virtual_directory";
    private static final String GCS_BUCKET                 = "gcp_storage_bucket";
    private static final String GCS_SCHEME                 = "gs" + SCHEME_SEPARATOR;
    private static final String GCS_PATH                   = GCS_SCHEME + "gcs_test_bucket1/1234567890/data";

    @DataProvider(name = "ozonePathProvider")
    private Object[][] ozonePathProvider(){
        return new Object[][]{
                { new OzoneKeyValidator(OZONE_SCHEME, "ozone1.com/volume1/bucket1/files/file.txt",
                        "files", "ozone1.com/volume1/bucket1/files",
                        "file.txt", "ozone1.com/volume1/bucket1/files/file.txt")},

                { new OzoneKeyValidator(OZONE_SCHEME, "ozone1:1234/volume1/bucket1/file21.txt",
                        "file21.txt", "ozone1:1234/volume1/bucket1/file21.txt")},

                { new OzoneKeyValidator(OZONE_SCHEME, "ozone1/volume1/bucket1/quarter_one/sales",
                        "quarter_one", "ozone1/volume1/bucket1/quarter_one",
                        "sales", "ozone1/volume1/bucket1/quarter_one/sales")},

                { new OzoneKeyValidator(OZONE_SCHEME, "ozone1/volume1/bucket1/quarter_one/sales/",
                        "quarter_one", "ozone1/volume1/bucket1/quarter_one",
                        "sales", "ozone1/volume1/bucket1/quarter_one/sales")},

                { new OzoneKeyValidator(OZONE_3_SCHEME, "bucket1.volume1.ozone1/files/file.txt",
                        "files", "bucket1.volume1.ozone1/files",
                        "file.txt", "bucket1.volume1.ozone1/files/file.txt") },

                { new OzoneKeyValidator(OZONE_3_SCHEME, "bucket1.volume1.ozone1/file21.txt",
                        "file21.txt", "bucket1.volume1.ozone1/file21.txt") },

                { new OzoneKeyValidator(OZONE_3_SCHEME, "bucket1.volume1.ozone1/quarter_one/sales",
                        "quarter_one", "bucket1.volume1.ozone1/quarter_one",
                        "sales", "bucket1.volume1.ozone1/quarter_one/sales") },

                { new OzoneKeyValidator(OZONE_3_SCHEME, "bucket1.volume1.ozone1/quarter_one/sales/",
                        "quarter_one", "bucket1.volume1.ozone1/quarter_one",
                        "sales", "bucket1.volume1.ozone1/quarter_one/sales") },
        };
    }

    @Test(dataProvider = "ozonePathProvider")
    public void testGetPathEntityOzone3Path(OzoneKeyValidator validator) {
        String scheme = validator.scheme;
        String ozonePath = scheme + validator.location;

        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);
        Path path = new Path(ozonePath);

        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        verifyOzoneKeyEntity(entity, validator);

        assertEquals(entityWithExtInfo.getReferredEntities().size(), 2);
        verifyOzoneEntities(entityWithExtInfo.getReferredEntities(), validator);

        assertEquals(extractorContext.getKnownEntities().size(), validator.knownEntitiesCount);
        verifyOzoneEntities(extractorContext.getKnownEntities(), validator);
    }

    @Test
    public void testGetPathEntityHdfsPath() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(HDFS_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), HDFS_PATH_TYPE);
        verifyHDFSEntity(entity, false);

        assertNull(entityWithExtInfo.getReferredEntities());
        assertEquals(extractorContext.getKnownEntities().size(), 1);
        extractorContext.getKnownEntities().values().forEach(x -> verifyHDFSEntity(x, false));
    }

    @Test
    public void testGetPathEntityHdfsPathLowerCase() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE, true, null);

        Path path = new Path(HDFS_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), HDFS_PATH_TYPE);
        verifyHDFSEntity(entity, true);

        assertNull(entityWithExtInfo.getReferredEntities());
        assertEquals(extractorContext.getKnownEntities().size(), 1);
        extractorContext.getKnownEntities().values().forEach(x -> verifyHDFSEntity(x, true));
    }

    @Test
    public void testGetPathEntityABFSPath() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(ABFS_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), ADLS_GEN2_DIRECTORY);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 2);

        verifyABFSAdlsGen2Dir(ABFS_SCHEME, ABFS_PATH, entity);
        verifyABFSKnownEntities(ABFS_SCHEME, ABFS_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityABFSSPath() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(ABFSS_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), ADLS_GEN2_DIRECTORY);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 2);

        verifyABFSAdlsGen2Dir(ABFSS_SCHEME, ABFSS_PATH, entity);
        verifyABFSKnownEntities(ABFSS_SCHEME, ABFSS_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityS3V2Path() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_ATLAS_MODEL_VERSION_V2);

        Path path = new Path(S3_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), AWS_S3_V2_PSEUDO_DIR);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 1);

        verifyS3V2PseudoDir(S3A_SCHEME, S3_PATH, entity);
        verifyS3V2KnownEntities(S3_SCHEME, S3_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityS3AV2Path() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE, AWS_S3_ATLAS_MODEL_VERSION_V2);

        Path path = new Path(S3A_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), AWS_S3_V2_PSEUDO_DIR);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 1);

        verifyS3V2PseudoDir(S3A_SCHEME, S3A_PATH, entity);
        verifyS3V2KnownEntities(S3A_SCHEME, S3A_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityS3Path() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(S3_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), AWS_S3_PSEUDO_DIR);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 1);

        verifyS3PseudoDir(S3_PATH, entity);
        verifyS3KnownEntities(S3_SCHEME, S3_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityS3APath() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(S3A_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), AWS_S3_PSEUDO_DIR);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 1);

        verifyS3PseudoDir(S3A_PATH, entity);
        verifyS3KnownEntities(S3A_SCHEME, S3A_PATH, extractorContext.getKnownEntities());
    }

    @Test
    public void testGetPathEntityGCSPath() {
        PathExtractorContext extractorContext = new PathExtractorContext(METADATA_NAMESPACE);

        Path path = new Path(GCS_PATH);
        AtlasEntityWithExtInfo entityWithExtInfo = AtlasPathExtractorUtil.getPathEntity(path, extractorContext);
        AtlasEntity entity = entityWithExtInfo.getEntity();

        assertNotNull(entity);
        assertEquals(entity.getTypeName(), GCS_VIRTUAL_DIR);
        assertEquals(entityWithExtInfo.getReferredEntities().size(), 1);

        verifyGCSVirtualDir(GCS_SCHEME, GCS_PATH, entity);
        verifyGCSKnownEntities(GCS_SCHEME, GCS_PATH, extractorContext.getKnownEntities());
    }

    private void verifyOzoneEntities(Map<String, AtlasEntity> knownEntities, OzoneKeyValidator validator) {
        for (AtlasEntity knownEntity : knownEntities.values()) {
            switch (knownEntity.getTypeName()){
                case OZONE_KEY:
                    verifyOzoneKeyEntity(knownEntity, validator);
                    break;

                case OZONE_VOLUME:
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), validator.scheme + "volume1" + QNAME_METADATA_NAMESPACE);
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_NAME), "volume1");
                    break;

                case OZONE_BUCKET:
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), validator.scheme + "volume1.bucket1" + QNAME_METADATA_NAMESPACE);
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_NAME), "bucket1");
                    break;
            }
        }
    }

    private void verifyOzoneKeyEntity(AtlasEntity entity, OzoneKeyValidator validator) {
        assertEquals(entity.getTypeName(), OZONE_KEY);
        assertTrue(validator.validateNameQName(entity));
    }

    private void verifyHDFSEntity(AtlasEntity entity, boolean toLowerCase) {
        if (toLowerCase) {
            assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), HDFS_PATH.toLowerCase() + QNAME_METADATA_NAMESPACE);
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "/warehouse/tablespace/external/hive/table_306");
            assertEquals(entity.getAttribute(ATTRIBUTE_PATH), HDFS_PATH.toLowerCase());
            assertEquals(entity.getAttribute(ATTRIBUTE_CLUSTER_NAME), METADATA_NAMESPACE);
        } else {
            assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), HDFS_PATH + QNAME_METADATA_NAMESPACE);
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "/warehouse/tablespace/external/hive/taBlE_306");
            assertEquals(entity.getAttribute(ATTRIBUTE_PATH), HDFS_PATH);
            assertEquals(entity.getAttribute(ATTRIBUTE_CLUSTER_NAME), METADATA_NAMESPACE);
        }
    }

    private void verifyABFSAdlsGen2Dir(String abfsScheme, String path, AtlasEntity entity){
        String pathQName = abfsScheme + "data@razrangersan/tmp/cdp-demo/sample.csv" + QNAME_METADATA_NAMESPACE;
        String entityQName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

        if (pathQName.equalsIgnoreCase(entityQName)){
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "sample.csv");
        } else {
            pathQName = abfsScheme + "data@razrangersan/tmp/cdp-demo" + QNAME_METADATA_NAMESPACE;
            if (pathQName.equalsIgnoreCase(entityQName)){
                assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "cdp-demo");
            } else {
                assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), abfsScheme + "data@razrangersan/tmp" + QNAME_METADATA_NAMESPACE);
                assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "tmp");
            }
        }
    }

    private void verifyABFSKnownEntities(String scheme, String path, Map<String, AtlasEntity> knownEntities) {
        assertEquals(knownEntities.size(), 5);
        int directoryCount = 0;
        for (AtlasEntity knownEntity : knownEntities.values()) {
            switch (knownEntity.getTypeName()){
                case ADLS_GEN2_DIRECTORY:
                    verifyABFSAdlsGen2Dir(scheme, path, knownEntity);
                    directoryCount++;
                    break;

                case ADLS_GEN2_CONTAINER:
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), scheme + "data@razrangersan" + QNAME_METADATA_NAMESPACE);
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_NAME), "data");
                    break;

                case ADLS_GEN2_ACCOUNT:
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), scheme + "razrangersan" + QNAME_METADATA_NAMESPACE);
                    assertEquals(knownEntity.getAttribute(ATTRIBUTE_NAME), "razrangersan");
                    break;
            }
        }
        assertEquals(directoryCount, 3);
    }

    private void verifyS3V2PseudoDir(String s3Scheme, String path, AtlasEntity entity){
        String pathQName = path + "/" + QNAME_METADATA_NAMESPACE;
        String entityQName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

        if (pathQName.equalsIgnoreCase(entityQName)){
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "Irradiance_A.csv");
            assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/renders/Irradiance_A.csv/");
        } else {
            pathQName = s3Scheme + "aws_my_bucket1/1234567890/" + QNAME_METADATA_NAMESPACE;
            if (pathQName.equalsIgnoreCase(entityQName)){
                assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "1234567890");
                assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/");
            } else {
                assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), s3Scheme + "aws_my_bucket1/1234567890/renders/" + QNAME_METADATA_NAMESPACE);
                assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "renders");
                assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/renders/");
            }
        }
    }

    private void verifyGCSVirtualDir(String s3Scheme, String path, AtlasEntity entity) {
        String pathQName = path + "/" + QNAME_METADATA_NAMESPACE;
        String entityQName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

        if (pathQName.equalsIgnoreCase(entityQName)){
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "data");
            assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/");
        } else {
            pathQName = s3Scheme + "gcs_test_bucket1/1234567890/" + QNAME_METADATA_NAMESPACE;
            assertEquals(entityQName, pathQName);
            assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "1234567890");
            assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/");
        }
    }

    private void verifyS3V2KnownEntities(String scheme, String path, Map<String, AtlasEntity> knownEntities) {
        assertEquals(knownEntities.size(), 4);
        int dirCount = 0;
        for (AtlasEntity knownEntity : knownEntities.values()) {
            switch (knownEntity.getTypeName()){
                case AWS_S3_V2_PSEUDO_DIR:
                    verifyS3V2PseudoDir(scheme, path, knownEntity);
                    dirCount++;
                    break;

                case AWS_S3_V2_BUCKET:
                    verifyS3BucketEntity(scheme, knownEntity);
                    break;
            }
        }
        assertEquals(dirCount, 3);
    }

    private void verifyS3PseudoDir(String path, AtlasEntity entity){
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), path.toLowerCase() + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "/1234567890/renders/irradiance_a.csv");
        assertEquals(entity.getAttribute(ATTRIBUTE_OBJECT_PREFIX), "/1234567890/renders/irradiance_a.csv");
    }

    private void verifyS3KnownEntities(String scheme, String path, Map<String, AtlasEntity> knownEntities) {
        assertEquals(knownEntities.size(), 2);
        int dirCount = 0;
        for (AtlasEntity knownEntity : knownEntities.values()) {
            switch (knownEntity.getTypeName()){
                case AWS_S3_PSEUDO_DIR:
                    verifyS3PseudoDir(path, knownEntity);
                    dirCount++;
                    break;

                case AWS_S3_BUCKET:
                    verifyS3BucketEntity(scheme, knownEntity);
                    break;
            }
        }
        assertEquals(dirCount, 1);
    }

    private void verifyS3BucketEntity(String scheme, AtlasEntity entity) {
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), scheme + "aws_my_bucket1" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "aws_my_bucket1");
    }

    private void verifyGCSKnownEntities(String scheme, String path, Map<String, AtlasEntity> knownEntities) {
        assertEquals(knownEntities.size(), 3);
        int dirCount = 0;
        for (AtlasEntity knownEntity : knownEntities.values()) {
            switch (knownEntity.getTypeName()){
                case GCS_VIRTUAL_DIR:
                    verifyGCSVirtualDir(scheme, path, knownEntity);
                    dirCount++;
                    break;

                case GCS_BUCKET:
                    verifyGCSBucketEntity(scheme, knownEntity);
                    break;
            }
        }
        assertEquals(dirCount, 2);
    }

    private void verifyGCSBucketEntity(String scheme, AtlasEntity entity) {
        assertEquals(entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME), scheme + "gcs_test_bucket1" + QNAME_METADATA_NAMESPACE);
        assertEquals(entity.getAttribute(ATTRIBUTE_NAME), "gcs_test_bucket1");
    }

    private class OzoneKeyValidator {
        private final String              scheme;
        private final String              location;
        private final int                 knownEntitiesCount;
        private final Map<String, String> nameQNamePairs;

        public OzoneKeyValidator(String scheme, String location, String... pairs) {
            this.scheme             = scheme;
            this.location           = location;
            this.nameQNamePairs     = getPairMap(scheme, pairs);
            this.knownEntitiesCount = nameQNamePairs.size() + 2;
        }

        public boolean validateNameQName(AtlasEntity entity){
            String name = (String) entity.getAttribute(ATTRIBUTE_NAME);

            if (this.nameQNamePairs.containsKey(name)){
                String qName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);

                if (qName.equals(this.nameQNamePairs.get(name))) {
                    return true;
                }
            }

            return false;
        }

        private Map<String, String> getPairMap(String scheme, String... pairs){
            Map< String, String > ret = new HashMap<>();

            for (int i = 0; i < pairs.length; i += 2) {
                ret.put(pairs[i], scheme + pairs[i+1] + QNAME_METADATA_NAMESPACE);
            }

            return ret;
        }
    }
}
