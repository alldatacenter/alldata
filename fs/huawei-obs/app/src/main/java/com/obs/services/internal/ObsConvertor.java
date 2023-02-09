/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.xml.OBSXMLBuilder;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.EventTypeEnum;
import com.obs.services.model.FunctionGraphConfiguration;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GranteeInterface;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.GroupGranteeEnum;
import com.obs.services.model.Owner;
import com.obs.services.model.Permission;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreTierEnum;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.TopicConfiguration;

public class ObsConvertor extends V2Convertor {

    private static ObsConvertor instance = new ObsConvertor();

    public static IConvertor getInstance() {
        return instance;
    }

    @Override
    public String transBucketLoction(String location) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("CreateBucketConfiguration").elem("Location")
                    .text(ServiceUtils.toValid(location));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String transRestoreObjectRequest(RestoreObjectRequest req) throws ServiceException {

        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("RestoreRequest")
                    .elem("Days").t(String.valueOf(req.getDays())).up();
            if (req.getRestoreTier() != null && req.getRestoreTier() != RestoreTierEnum.BULK) {
                builder.e("RestoreJob").e("Tier").t(req.getRestoreTier().getCode());
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for restoreobject", e);
        }

    }

    @Override
    public String transBucketEcryption(BucketEncryption encryption) throws ServiceException {
        String algorithm = encryption.getSseAlgorithm().getCode();
        String kmsKeyId = "";
        if (algorithm.equals(SSEAlgorithmEnum.KMS.getCode())) {
            kmsKeyId = encryption.getKmsKeyId();
        }
        return transBucketEcryptionXML(algorithm, kmsKeyId);
    }

    @Override
    public String transStoragePolicy(BucketStoragePolicyConfiguration status) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("StorageClass")
                    .text(this.transStorageClass(status.getBucketStorageClass()));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for StorageClass", e);
        }
    }

    @Override
    public String transBucketLoggingConfiguration(BucketLoggingConfiguration c) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("BucketLoggingStatus");
            if (c.getAgency() != null) {
                builder.e("Agency").t(ServiceUtils.toValid(c.getAgency()));
            }
            if (c.isLoggingEnabled()) {
                OBSXMLBuilder enabledBuilder = builder.elem("LoggingEnabled");
                if (c.getTargetBucketName() != null) {
                    enabledBuilder.elem("TargetBucket").text(ServiceUtils.toValid(c.getTargetBucketName()));
                }
                if (c.getLogfilePrefix() != null) {
                    enabledBuilder.elem("TargetPrefix").text(ServiceUtils.toValid(c.getLogfilePrefix()));
                }
                GrantAndPermission[] grants = c.getTargetGrants();
                if (grants.length > 0) {
                    OBSXMLBuilder grantsBuilder = enabledBuilder.elem("TargetGrants");
                    transGrantsBuilder(grants, grantsBuilder);
                }
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for BucketLoggingConfiguration", e);
        }
    }

    private void transGrantsBuilder(GrantAndPermission[] grants, OBSXMLBuilder grantsBuilder)
            throws ParserConfigurationException, FactoryConfigurationError {
        for (GrantAndPermission gap : grants) {
            GranteeInterface grantee = gap.getGrantee();
            Permission permission = gap.getPermission();
            if (permission != null) {
                OBSXMLBuilder subBuilder = null;
                if (grantee instanceof CanonicalGrantee) {
                    subBuilder = OBSXMLBuilder.create("Grantee").element("ID")
                            .text(ServiceUtils.toValid(grantee.getIdentifier()));
                } else if (grantee instanceof GroupGrantee) {
                    subBuilder = OBSXMLBuilder.create("Grantee").element("Canned")
                            .text(this.transGroupGrantee(((GroupGrantee) grantee).getGroupGranteeType()));
                }

                if (subBuilder != null) {
                    grantsBuilder.elem("Grant").importXMLBuilder(subBuilder).elem("Permission")
                            .text(ServiceUtils.toValid(permission.getPermissionString()));
                }
            }
        }
    }

    @Override
    public String transAccessControlList(AccessControlList acl, boolean isBucket) throws ServiceException {
        Owner owner = acl.getOwner();
        GrantAndPermission[] grants = acl.getGrantAndPermissions();
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("AccessControlPolicy");
            if (owner != null) {
                builder.elem("Owner").elem("ID").text(ServiceUtils.toValid(owner.getId()));
            }
            if (!isBucket) {
                builder.elem("Delivered").text(String.valueOf(acl.isDelivered()));
            }

            if (grants.length > 0) {
                OBSXMLBuilder accessControlList = builder.elem("AccessControlList");
                for (GrantAndPermission gap : grants) {
                    GranteeInterface grantee = gap.getGrantee();
                    Permission permission = gap.getPermission();

                    OBSXMLBuilder subBuilder = null;
                    if (grantee instanceof CanonicalGrantee) {
                        subBuilder = OBSXMLBuilder.create("Grantee").element("ID")
                                .text(ServiceUtils.toValid(grantee.getIdentifier()));
                    } else if (grantee instanceof GroupGrantee) {
                        if (((GroupGrantee) grantee).getGroupGranteeType() != GroupGranteeEnum.ALL_USERS) {
                            continue;
                        }
                        subBuilder = OBSXMLBuilder.create("Grantee").element("Canned")
                                .text(this.transGroupGrantee(((GroupGrantee) grantee).getGroupGranteeType()));
                    } else if (grantee != null) {
                        subBuilder = OBSXMLBuilder.create("Grantee").element("ID")
                                .text(ServiceUtils.toValid(grantee.getIdentifier()));
                    }
                    if (subBuilder != null) {
                        OBSXMLBuilder grantBuilder = accessControlList.elem("Grant").importXMLBuilder(subBuilder);
                        if (permission != null) {
                            grantBuilder.elem("Permission")
                                    .text(ServiceUtils.toValid(permission.getPermissionString()));
                        }
                        if (isBucket) {
                            grantBuilder.e("Delivered").t(String.valueOf(gap.isDelivered()));
                        }
                    }
                }
            }

            return builder.asString();
        } catch (ParserConfigurationException | FactoryConfigurationError | TransformerException e) {
            throw new ServiceException("Failed to build XML document for ACL", e);
        }
    }

    @Override
    public String transBucketNotificationConfiguration(BucketNotificationConfiguration bucketNotificationConfiguration)
            throws ServiceException {

        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("NotificationConfiguration");
            if (bucketNotificationConfiguration == null) {
                return builder.asString();
            }

            for (TopicConfiguration config : bucketNotificationConfiguration.getTopicConfigurations()) {
                packNotificationConfig(builder, config, "TopicConfiguration", "Topic", "Object");
            }

            for (FunctionGraphConfiguration config : bucketNotificationConfiguration.getFunctionGraphConfigurations()) {
                packNotificationConfig(builder, config, "FunctionGraphConfiguration", "FunctionGraph", "Object");
            }

            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for Notification", e);
        }
    }

    @Override
    public String transReplicationConfiguration(ReplicationConfiguration replicationConfiguration)
            throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("ReplicationConfiguration").e("Agency")
                    .t(ServiceUtils.toValid(replicationConfiguration.getAgency())).up();
            for (ReplicationConfiguration.Rule rule : replicationConfiguration.getRules()) {
                builder = builder.e("Rule");
                if (rule.getId() != null) {
                    builder.e("ID").t(rule.getId());
                }
                builder.e("Prefix").t(ServiceUtils.toValid(rule.getPrefix()));
                if (rule.getStatus() != null) {
                    builder.e("Status").t(rule.getStatus().getCode());
                }
                if (rule.getHistoricalObjectReplication() != null) {
                    builder.e("HistoricalObjectReplication").t(rule.getHistoricalObjectReplication().getCode());
                }
                if (rule.getDestination() != null) {
                    String bucketName = ServiceUtils.toValid(rule.getDestination().getBucket());
                    builder = builder.e("Destination").e("Bucket").t(bucketName).up();
                    if (rule.getDestination().getObjectStorageClass() != null) {
                        builder.e("StorageClass")
                                .t(this.transStorageClass(rule.getDestination().getObjectStorageClass()));
                    }
                    builder = builder.up();
                }
                builder = builder.up();
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for Replication", e);
        }
    }

    @Override
    public String transEventType(EventTypeEnum eventType) {
        return transEventTypeStatic(eventType);
    }

    public static String transEventTypeStatic(EventTypeEnum eventType) {
        String eventTypeStr = "";
        if (eventType != null) {
            switch (eventType) {
                case OBJECT_CREATED_ALL:
                    eventTypeStr = "ObjectCreated:*";
                    break;
                case OBJECT_CREATED_PUT:
                    eventTypeStr = "ObjectCreated:Put";
                    break;
                case OBJECT_CREATED_POST:
                    eventTypeStr = "ObjectCreated:Post";
                    break;
                case OBJECT_CREATED_COPY:
                    eventTypeStr = "ObjectCreated:Copy";
                    break;
                case OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD:
                    eventTypeStr = "ObjectCreated:CompleteMultipartUpload";
                    break;
                case OBJECT_REMOVED_ALL:
                    eventTypeStr = "ObjectRemoved:*";
                    break;
                case OBJECT_REMOVED_DELETE:
                    eventTypeStr = "ObjectRemoved:Delete";
                    break;
                case OBJECT_REMOVED_DELETE_MARKER_CREATED:
                    eventTypeStr = "ObjectRemoved:DeleteMarkerCreated";
                    break;
                default:
                    break;
            }
        }
        return eventTypeStr;
    }

    @Override
    public String transStorageClass(StorageClassEnum storageClass) {
        String storageClassStr = "";
        if (storageClass != null) {
            switch (storageClass) {
                case STANDARD:
                    storageClassStr = "STANDARD";
                    break;
                case WARM:
                    storageClassStr = "WARM";
                    break;
                case COLD:
                    storageClassStr = "COLD";
                    break;
                case DEEP_ARCHIVE:
                    storageClassStr = "DEEP_ARCHIVE";
                    break;
                default:
                    break;
            }
        }
        return storageClassStr;
    }

    @Override
    public AccessControlList transCannedAcl(String cannedAcl) {
        if (Constants.ACL_PRIVATE.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PRIVATE;
        } else if (Constants.ACL_PUBLIC_READ.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PUBLIC_READ;
        } else if (Constants.ACL_PUBLIC_READ_WRITE.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PUBLIC_READ_WRITE;
        } else if (Constants.ACL_PUBLIC_READ_DELIVERED.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PUBLIC_READ_DELIVERED;
        } else if (Constants.ACL_PUBLIC_READ_WRITE_DELIVERED.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PUBLIC_READ_WRITE_DELIVERED;
        }
        return null;
    }

    @Override
    public String transGroupGrantee(GroupGranteeEnum groupGrantee) {
        String groupGranteeStr = "";
        if (groupGrantee == GroupGranteeEnum.ALL_USERS) {
            groupGranteeStr = "Everyone";
        }
        return groupGranteeStr;
    }
}
