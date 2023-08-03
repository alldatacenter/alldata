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
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.stream.IntStream;

public class AtlasPathExtractorUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPathExtractorUtil.class);

    // Common
    public static final char   QNAME_SEP_METADATA_NAMESPACE = '@';
    public static final char   QNAME_SEP_ENTITY_NAME        = '.';
    public static final String SCHEME_SEPARATOR             = "://";
    public static final String ATTRIBUTE_QUALIFIED_NAME     = "qualifiedName";
    public static final String ATTRIBUTE_NAME               = "name";
    public static final String ATTRIBUTE_BUCKET             = "bucket";

    // HDFS
    public static final String HDFS_TYPE_PATH           = "hdfs_path";
    public static final String ATTRIBUTE_PATH           = "path";
    public static final String ATTRIBUTE_CLUSTER_NAME   = "clusterName";
    public static final String ATTRIBUTE_NAMESERVICE_ID = "nameServiceId";

    // AWS S3
    public static final String AWS_S3_ATLAS_MODEL_VERSION_V2              = "v2";
    public static final String AWS_S3_BUCKET                              = "aws_s3_bucket";
    public static final String AWS_S3_PSEUDO_DIR                          = "aws_s3_pseudo_dir";
    public static final String AWS_S3_V2_BUCKET                           = "aws_s3_v2_bucket";
    public static final String AWS_S3_V2_PSEUDO_DIR                       = "aws_s3_v2_directory";
    public static final String S3_SCHEME                                  = "s3" + SCHEME_SEPARATOR;
    public static final String S3A_SCHEME                                 = "s3a" + SCHEME_SEPARATOR;
    public static final String ATTRIBUTE_CONTAINER                        = "container";
    public static final String ATTRIBUTE_OBJECT_PREFIX                    = "objectPrefix";
    public static final String RELATIONSHIP_AWS_S3_BUCKET_S3_PSEUDO_DIRS  = "aws_s3_bucket_aws_s3_pseudo_dirs";
    public static final String RELATIONSHIP_AWS_S3_V2_CONTAINER_CONTAINED = "aws_s3_v2_container_contained";

    // ADLS Gen2
    public static final String ADLS_GEN2_ACCOUNT                          = "adls_gen2_account";
    public static final String ADLS_GEN2_CONTAINER                        = "adls_gen2_container";
    public static final String ADLS_GEN2_DIRECTORY                        = "adls_gen2_directory";
    public static final String ADLS_GEN2_ACCOUNT_HOST_SUFFIX              = ".dfs.core.windows.net";
    public static final String ABFS_SCHEME                                = "abfs" + SCHEME_SEPARATOR;
    public static final String ABFSS_SCHEME                               = "abfss" + SCHEME_SEPARATOR;
    public static final String ATTRIBUTE_ACCOUNT                          = "account";
    public static final String ATTRIBUTE_PARENT                           = "parent";
    public static final String RELATIONSHIP_ADLS_GEN2_ACCOUNT_CONTAINERS  = "adls_gen2_account_containers";
    public static final String RELATIONSHIP_ADLS_GEN2_PARENT_CHILDREN     = "adls_gen2_parent_children";

    // Ozone
    public static final String OZONE_VOLUME                         = "ozone_volume";
    public static final String OZONE_BUCKET                         = "ozone_bucket";
    public static final String OZONE_KEY                            = "ozone_key";
    public static final String OZONE_SCHEME                         = "ofs" + SCHEME_SEPARATOR;
    public static final String OZONE_3_SCHEME                       = "o3fs" + SCHEME_SEPARATOR;
    public static final String ATTRIBUTE_VOLUME                     = "volume";
    public static final String RELATIONSHIP_OZONE_VOLUME_BUCKET     = "ozone_volume_buckets";
    public static final String RELATIONSHIP_OZONE_PARENT_CHILDREN   = "ozone_parent_children";
    public static final String OZONE_SCHEME_NAME                    = "ofs";

    //Google Cloud Storage
    public static final String GCS_SCHEME                       = "gs" + SCHEME_SEPARATOR;
    public static final String GCS_BUCKET                       = "gcp_storage_bucket";
    public static final String GCS_VIRTUAL_DIR                  = "gcp_storage_virtual_directory";
    public static final String ATTRIBUTE_GCS_PARENT             = "parent";
    public static final String RELATIONSHIP_GCS_PARENT_CHILDREN = "gcp_storage_parent_children";

    public static AtlasEntityWithExtInfo getPathEntity(Path path, PathExtractorContext context) {
        AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntityWithExtInfo();
        AtlasEntity ret;
        String      strPath = path.toString();

        if (context.isConvertPathToLowerCase()) {
            strPath = strPath.toLowerCase();
        }

        if (isS3Path(strPath)) {
            ret = isAwsS3AtlasModelVersionV2(context) ? addS3PathEntityV2(path, entityWithExtInfo, context) :
                                                        addS3PathEntityV1(path, entityWithExtInfo, context);
        } else if (isAbfsPath(strPath)) {
            ret = addAbfsPathEntity(path, entityWithExtInfo, context);
        } else if (isOzonePath(strPath)) {
            ret = addOzonePathEntity(path, entityWithExtInfo, context);
        } else if (isGCSPath(strPath)) {
            ret = addGCSPathEntity(path, entityWithExtInfo, context);
        } else {
            ret = addHDFSPathEntity(path, context);
        }

        entityWithExtInfo.setEntity(ret);

        return entityWithExtInfo;
    }

    private static boolean isAwsS3AtlasModelVersionV2(PathExtractorContext context) {
        return StringUtils.isNotEmpty(context.getAwsS3AtlasModelVersion()) &&
               StringUtils.equalsIgnoreCase(context.getAwsS3AtlasModelVersion(), AWS_S3_ATLAS_MODEL_VERSION_V2);
    }

    private static boolean isS3Path(String strPath) {
        return strPath != null && (strPath.startsWith(S3_SCHEME) || strPath.startsWith(S3A_SCHEME));
    }

    private static boolean isAbfsPath(String strPath) {
        return strPath != null && (strPath.startsWith(ABFS_SCHEME) || strPath.startsWith(ABFSS_SCHEME));
    }

    private static boolean isOzonePath(String strPath) {
        return strPath != null && (strPath.startsWith(OZONE_SCHEME) || strPath.startsWith(OZONE_3_SCHEME));
    }

    private static boolean isGCSPath(String strPath) {
        return strPath != null && strPath.startsWith(GCS_SCHEME);
    }

    private static AtlasEntity addS3PathEntityV1(Path path, AtlasEntityExtInfo extInfo, PathExtractorContext context) {
        String strPath = path.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addS3PathEntityV1(strPath={})", strPath);
        }

        String      metadataNamespace   = context.getMetadataNamespace();
        String      bucketName          = path.toUri().getAuthority();
        String      bucketQualifiedName = (path.toUri().getScheme() + SCHEME_SEPARATOR + path.toUri().getAuthority() + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + metadataNamespace;
        String      pathQualifiedName   = (strPath + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + metadataNamespace;
        AtlasEntity bucketEntity        = context.getEntity(bucketQualifiedName);
        AtlasEntity ret                 = context.getEntity(pathQualifiedName);

        if (ret == null) {
            if (bucketEntity == null) {
                bucketEntity = new AtlasEntity(AWS_S3_BUCKET);

                bucketEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, bucketQualifiedName);
                bucketEntity.setAttribute(ATTRIBUTE_NAME, bucketName);

                context.putEntity(bucketQualifiedName, bucketEntity);
            }

            extInfo.addReferredEntity(bucketEntity);

            ret = new AtlasEntity(AWS_S3_PSEUDO_DIR);

            ret.setRelationshipAttribute(ATTRIBUTE_BUCKET, AtlasTypeUtil.getAtlasRelatedObjectId(bucketEntity, RELATIONSHIP_AWS_S3_BUCKET_S3_PSEUDO_DIRS));
            ret.setAttribute(ATTRIBUTE_OBJECT_PREFIX, Path.getPathWithoutSchemeAndAuthority(path).toString().toLowerCase());
            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, pathQualifiedName);
            ret.setAttribute(ATTRIBUTE_NAME, Path.getPathWithoutSchemeAndAuthority(path).toString().toLowerCase());

            context.putEntity(pathQualifiedName, ret);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addS3PathEntityV1(strPath={})", strPath);
        }

        return ret;
    }

    private static AtlasEntity addS3PathEntityV2(Path path, AtlasEntityExtInfo extInfo, PathExtractorContext context) {
        String strPath = path.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addS3PathEntityV2(strPath={})", strPath);
        }

        String      metadataNamespace = context.getMetadataNamespace();
        String      pathQualifiedName = strPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
        AtlasEntity ret               = context.getEntity(pathQualifiedName);

        if (ret == null) {
            String      bucketName          = path.toUri().getAuthority();
            String      schemeAndBucketName = (path.toUri().getScheme() + SCHEME_SEPARATOR + bucketName).toLowerCase();
            String      bucketQualifiedName = schemeAndBucketName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity bucketEntity        = context.getEntity(bucketQualifiedName);

            if (bucketEntity == null) {
                bucketEntity = new AtlasEntity(AWS_S3_V2_BUCKET);

                bucketEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, bucketQualifiedName);
                bucketEntity.setAttribute(ATTRIBUTE_NAME, bucketName);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", bucketEntity.getTypeName(), bucketEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(bucketQualifiedName, bucketEntity);
            }

            extInfo.addReferredEntity(bucketEntity);

            AtlasRelatedObjectId parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(bucketEntity, RELATIONSHIP_AWS_S3_V2_CONTAINER_CONTAINED);
            String               parentPath  = Path.SEPARATOR;
            String               dirPath     = path.toUri().getPath();

            if (StringUtils.isEmpty(dirPath)) {
                dirPath = Path.SEPARATOR;
            }

            for (String subDirName : dirPath.split(Path.SEPARATOR)) {
                if (StringUtils.isEmpty(subDirName)) {
                    continue;
                }

                String subDirPath          = parentPath + subDirName + Path.SEPARATOR;
                String subDirQualifiedName = schemeAndBucketName + subDirPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;

                ret = context.getEntity(subDirQualifiedName);

                if (ret == null) {
                    ret = new AtlasEntity(AWS_S3_V2_PSEUDO_DIR);

                    ret.setRelationshipAttribute(ATTRIBUTE_CONTAINER, parentObjId);
                    ret.setAttribute(ATTRIBUTE_OBJECT_PREFIX, subDirPath);
                    ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, subDirQualifiedName);
                    ret.setAttribute(ATTRIBUTE_NAME, subDirName);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("adding entity: typeName={}, qualifiedName={}", ret.getTypeName(), ret.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                    }

                    context.putEntity(subDirQualifiedName, ret);
                }

                parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(ret, RELATIONSHIP_AWS_S3_V2_CONTAINER_CONTAINED);
                parentPath  = subDirPath;
            }

            if (ret == null) {
                ret = bucketEntity;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addS3PathEntityV2(strPath={})", strPath);
        }

        return ret;
    }

    private static AtlasEntity addAbfsPathEntity(Path path, AtlasEntityExtInfo extInfo, PathExtractorContext context) {
        String strPath = path.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addAbfsPathEntity(strPath={})", strPath);
        }

        String      metadataNamespace = context.getMetadataNamespace();
        String      pathQualifiedName = strPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
        AtlasEntity ret               = context.getEntity(pathQualifiedName);

        if (ret == null) {
            String      abfsScheme               = path.toUri().getScheme();
            String      storageAcctName          = getAbfsStorageAccountName(path.toUri());
            String      schemeAndStorageAcctName = (abfsScheme + SCHEME_SEPARATOR + storageAcctName).toLowerCase();
            String      storageAcctQualifiedName = schemeAndStorageAcctName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity storageAcctEntity        = context.getEntity(storageAcctQualifiedName);

            // create adls-gen2 storage-account entity
            if (storageAcctEntity == null) {
                storageAcctEntity = new AtlasEntity(ADLS_GEN2_ACCOUNT);

                storageAcctEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, storageAcctQualifiedName);
                storageAcctEntity.setAttribute(ATTRIBUTE_NAME, storageAcctName);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", storageAcctEntity.getTypeName(), storageAcctEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(storageAcctQualifiedName, storageAcctEntity);
            }

            extInfo.addReferredEntity(storageAcctEntity);

            AtlasRelatedObjectId storageAcctObjId = AtlasTypeUtil.getAtlasRelatedObjectId(storageAcctEntity, RELATIONSHIP_ADLS_GEN2_ACCOUNT_CONTAINERS);

            // create adls-gen2 container entity linking to storage account
            String      containerName          = path.toUri().getUserInfo();
            String      schemeAndContainerName = (abfsScheme + SCHEME_SEPARATOR + containerName + QNAME_SEP_METADATA_NAMESPACE + storageAcctName).toLowerCase();
            String      containerQualifiedName = schemeAndContainerName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity containerEntity        = context.getEntity(containerQualifiedName);

            if (containerEntity == null) {
                containerEntity = new AtlasEntity(ADLS_GEN2_CONTAINER);

                containerEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, containerQualifiedName);
                containerEntity.setAttribute(ATTRIBUTE_NAME, containerName);
                containerEntity.setRelationshipAttribute(ATTRIBUTE_ACCOUNT, storageAcctObjId);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", containerEntity.getTypeName(), containerEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(containerQualifiedName, containerEntity);
            }

            extInfo.addReferredEntity(containerEntity);

            // create adls-gen2 directory entity linking to container
            AtlasRelatedObjectId parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(containerEntity, RELATIONSHIP_ADLS_GEN2_PARENT_CHILDREN);
            String               parentPath  = Path.SEPARATOR;
            String               dirPath     = path.toUri().getPath();

            if (StringUtils.isEmpty(dirPath)) {
                dirPath = Path.SEPARATOR;
            }

            for (String subDirName : dirPath.split(Path.SEPARATOR)) {
                if (StringUtils.isEmpty(subDirName)) {
                    continue;
                }

                String subDirPath          = parentPath + subDirName;
                String subDirQualifiedName = schemeAndContainerName + subDirPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;

                ret = context.getEntity(subDirQualifiedName);

                if (ret == null) {
                    ret = new AtlasEntity(ADLS_GEN2_DIRECTORY);

                    ret.setRelationshipAttribute(ATTRIBUTE_PARENT, parentObjId);
                    ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, subDirQualifiedName);
                    ret.setAttribute(ATTRIBUTE_NAME, subDirName);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("adding entity: typeName={}, qualifiedName={}", ret.getTypeName(), ret.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                    }

                    context.putEntity(subDirQualifiedName, ret);
                }

                parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(ret, RELATIONSHIP_ADLS_GEN2_PARENT_CHILDREN);
                parentPath  = subDirPath + Path.SEPARATOR;
            }

            if (ret == null) {
                ret = storageAcctEntity;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addAbfsPathEntity(strPath={})", strPath);
        }

        return ret;
    }

    private static AtlasEntity addOzonePathEntity(Path path, AtlasEntityExtInfo extInfo, PathExtractorContext context) {
        String strPath = path.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addOzonePathEntity(strPath={})", strPath);
        }

        String      metadataNamespace = context.getMetadataNamespace();
        String      ozoneScheme       = path.toUri().getScheme();
        String      pathQualifiedName = strPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
        AtlasEntity ret               = context.getEntity(pathQualifiedName);

        if (ret == null) {
            //create ozone volume entity
            String      volumeName          = getOzoneVolumeName(path);
            String      volumeQualifiedName = ozoneScheme + SCHEME_SEPARATOR + volumeName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity volumeEntity        = context.getEntity(volumeQualifiedName);

            if (volumeEntity == null) {
                volumeEntity = new AtlasEntity(OZONE_VOLUME);

                volumeEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, volumeQualifiedName);
                volumeEntity.setAttribute(ATTRIBUTE_NAME, volumeName);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", volumeEntity.getTypeName(), volumeEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(volumeQualifiedName, volumeEntity);
            }

            extInfo.addReferredEntity(volumeEntity);

            //create ozone bucket entity
            String      bucketName          = getOzoneBucketName(path);
            String      bucketQualifiedName = ozoneScheme + SCHEME_SEPARATOR + volumeName + QNAME_SEP_ENTITY_NAME + bucketName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity bucketEntity        = context.getEntity(bucketQualifiedName);

            if (bucketEntity == null) {
                bucketEntity = new AtlasEntity(OZONE_BUCKET);

                bucketEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, bucketQualifiedName);
                bucketEntity.setAttribute(ATTRIBUTE_NAME, bucketName);
                bucketEntity.setRelationshipAttribute( ATTRIBUTE_VOLUME, AtlasTypeUtil.getAtlasRelatedObjectId(volumeEntity, RELATIONSHIP_OZONE_VOLUME_BUCKET));

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", bucketEntity.getTypeName(), bucketEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(bucketQualifiedName, bucketEntity);
            }

            extInfo.addReferredEntity(bucketEntity);

            AtlasRelatedObjectId parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(bucketEntity, RELATIONSHIP_OZONE_PARENT_CHILDREN);
            String               parentPath  = Path.SEPARATOR;
            String               dirPath     = path.toUri().getPath();

            if (StringUtils.isEmpty(dirPath)) {
                dirPath = Path.SEPARATOR;
            }

            String keyQNamePrefix = ozoneScheme + SCHEME_SEPARATOR + path.toUri().getAuthority();


            String[] subDirNames = dirPath.split(Path.SEPARATOR);
            String[] subDirNameArr = subDirNames;
            if (ozoneScheme.equals(OZONE_SCHEME_NAME)) {
                subDirNames = IntStream.range(3, subDirNameArr.length)
                        .mapToObj(i -> subDirNameArr[i])
                        .toArray(String[]::new);
            }

            boolean volumeBucketAdded = false;
            for (String subDirName : subDirNames) {
                if (StringUtils.isEmpty(subDirName)) {
                    continue;
                }

                String subDirPath;
                if (ozoneScheme.equals(OZONE_SCHEME_NAME) && !volumeBucketAdded) {
                    subDirPath = "%s%s" + Path.SEPARATOR + "%s" + Path.SEPARATOR + "%s";
                    subDirPath = String.format(subDirPath, parentPath, subDirNameArr[1], subDirNameArr[2], subDirName);
                    volumeBucketAdded = true;
                } else {
                    subDirPath = parentPath + subDirName;
                }
                String subDirQualifiedName = keyQNamePrefix + subDirPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;

                ret = context.getEntity(subDirQualifiedName);

                if (ret == null) {
                    ret = new AtlasEntity(OZONE_KEY);

                    ret.setRelationshipAttribute(ATTRIBUTE_PARENT, parentObjId);
                    ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, subDirQualifiedName);
                    ret.setAttribute(ATTRIBUTE_NAME, subDirName);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("adding entity: typeName={}, qualifiedName={}", ret.getTypeName(), ret.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                    }

                    context.putEntity(subDirQualifiedName, ret);
                }

                parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(ret, RELATIONSHIP_OZONE_PARENT_CHILDREN);
                parentPath  = subDirPath + Path.SEPARATOR;;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addOzonePathEntity(strPath={})", strPath);
        }

        return ret;
    }

    private static AtlasEntity addGCSPathEntity(Path path, AtlasEntityExtInfo extInfo, PathExtractorContext context) {
        String strPath = path.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addGCSPathEntity(strPath={})", strPath);
        }

        String      metadataNamespace = context.getMetadataNamespace();
        String      pathQualifiedName = strPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
        AtlasEntity ret               = context.getEntity(pathQualifiedName);

        if (ret == null) {
            String      bucketName          = path.toUri().getAuthority();
            String      schemeAndBucketName = (path.toUri().getScheme() + SCHEME_SEPARATOR + bucketName).toLowerCase();
            String      bucketQualifiedName = schemeAndBucketName + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
            AtlasEntity bucketEntity        = context.getEntity(bucketQualifiedName);

            if (bucketEntity == null) {
                bucketEntity = new AtlasEntity(GCS_BUCKET);

                bucketEntity.setAttribute(ATTRIBUTE_QUALIFIED_NAME, bucketQualifiedName);
                bucketEntity.setAttribute(ATTRIBUTE_NAME, bucketName);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("adding entity: typeName={}, qualifiedName={}", bucketEntity.getTypeName(), bucketEntity.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                }

                context.putEntity(bucketQualifiedName, bucketEntity);
            }

            extInfo.addReferredEntity(bucketEntity);

            AtlasRelatedObjectId parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(bucketEntity, RELATIONSHIP_GCS_PARENT_CHILDREN);
            String               parentPath  = Path.SEPARATOR;
            String               dirPath     = path.toUri().getPath();

            if (StringUtils.isEmpty(dirPath)) {
                dirPath = Path.SEPARATOR;
            }

            for (String subDirName : dirPath.split(Path.SEPARATOR)) {
                if (StringUtils.isEmpty(subDirName)) {
                    continue;
                }

                String subDirPath          = parentPath + subDirName + Path.SEPARATOR;
                String subDirQualifiedName = schemeAndBucketName + subDirPath + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;

                ret = context.getEntity(subDirQualifiedName);

                if (ret == null) {
                    ret = new AtlasEntity(GCS_VIRTUAL_DIR);

                    ret.setRelationshipAttribute(ATTRIBUTE_GCS_PARENT, parentObjId);
                    ret.setAttribute(ATTRIBUTE_OBJECT_PREFIX, parentPath);
                    ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, subDirQualifiedName);
                    ret.setAttribute(ATTRIBUTE_NAME, subDirName);

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("adding entity: typeName={}, qualifiedName={}", ret.getTypeName(), ret.getAttribute(ATTRIBUTE_QUALIFIED_NAME));
                    }

                    context.putEntity(subDirQualifiedName, ret);
                }

                parentObjId = AtlasTypeUtil.getAtlasRelatedObjectId(ret, RELATIONSHIP_GCS_PARENT_CHILDREN);
                parentPath  = subDirPath;
            }

            if (ret == null) {
                ret = bucketEntity;
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addGCSPathEntity(strPath={})", strPath);
        }

        return ret;
    }

    private static AtlasEntity addHDFSPathEntity(Path path, PathExtractorContext context) {
        String strPath = path.toString();

        if (context.isConvertPathToLowerCase()) {
            strPath = strPath.toLowerCase();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addHDFSPathEntity(strPath={})", strPath);
        }

        String      nameServiceID     = HdfsNameServiceResolver.getNameServiceIDForPath(strPath);
        String      attrPath          = StringUtils.isEmpty(nameServiceID) ? strPath : HdfsNameServiceResolver.getPathWithNameServiceID(strPath);
        String      pathQualifiedName = getQualifiedName(attrPath, context.getMetadataNamespace());
        AtlasEntity ret               = context.getEntity(pathQualifiedName);

        if (ret == null) {
            ret = new AtlasEntity(HDFS_TYPE_PATH);

            if (StringUtils.isNotEmpty(nameServiceID)) {
                ret.setAttribute(ATTRIBUTE_NAMESERVICE_ID, nameServiceID);
            }

            String name = Path.getPathWithoutSchemeAndAuthority(path).toString();

            if (context.isConvertPathToLowerCase()) {
                name = name.toLowerCase();
            }

            ret.setAttribute(ATTRIBUTE_PATH, attrPath);
            ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, pathQualifiedName);
            ret.setAttribute(ATTRIBUTE_NAME, name);
            ret.setAttribute(ATTRIBUTE_CLUSTER_NAME, context.getMetadataNamespace());

            context.putEntity(pathQualifiedName, ret);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addHDFSPathEntity(strPath={})", strPath);
        }

        return ret;
    }

    private static String getAbfsStorageAccountName(URI uri) {
        String ret  = null;
        String host = uri.getHost();

        // host: "<account_name>.dfs.core.windows.net"
        if (StringUtils.isNotEmpty(host) && host.contains(ADLS_GEN2_ACCOUNT_HOST_SUFFIX)) {
            ret = host.substring(0, host.indexOf(ADLS_GEN2_ACCOUNT_HOST_SUFFIX));
        }

        return ret;
    }

    private static String getOzoneVolumeName(Path path) {
        String strPath = path.toString();
        String volumeName = StringUtils.EMPTY;
        if (strPath.startsWith(OZONE_3_SCHEME)) {
            String pathAuthority = path.toUri().getAuthority();
            volumeName = pathAuthority.split("\\.")[1];
        } else if (strPath.startsWith(OZONE_SCHEME)) {
            strPath = strPath.replaceAll(OZONE_SCHEME, StringUtils.EMPTY);
            if (strPath.split(Path.SEPARATOR).length >= 2) {
                volumeName = strPath.split(Path.SEPARATOR)[1];
            }
        }
        return volumeName;
    }

    private static String getOzoneBucketName(Path path) {
        String strPath = path.toString();
        String bucketName = StringUtils.EMPTY;
        if (strPath.startsWith(OZONE_3_SCHEME)) {
            String pathAuthority = path.toUri().getAuthority();
            bucketName = pathAuthority.split("\\.")[0];
        } else if (strPath.startsWith(OZONE_SCHEME)) {
            strPath = strPath.replaceAll(OZONE_SCHEME, StringUtils.EMPTY);
            if (strPath.split(Path.SEPARATOR).length >= 3) {
                bucketName = strPath.split(Path.SEPARATOR)[2];
            }
        }
        return bucketName;
    }

    private static String getQualifiedName(String path, String metadataNamespace) {
        if (path.startsWith(HdfsNameServiceResolver.HDFS_SCHEME)) {
            return path + QNAME_SEP_METADATA_NAMESPACE + metadataNamespace;
        }

        return path.toLowerCase();
    }
}
