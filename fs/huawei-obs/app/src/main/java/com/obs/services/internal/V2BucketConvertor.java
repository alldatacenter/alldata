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
import com.obs.services.model.AbstractNotification;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCorsRule;
import com.obs.services.model.BucketDirectColdAccess;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.EventTypeEnum;
import com.obs.services.model.FunctionGraphConfiguration;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GranteeInterface;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.Permission;
import com.obs.services.model.SSEAlgorithmEnum;
import com.obs.services.model.TopicConfiguration;
import com.obs.services.model.fs.FSStatusEnum;

public abstract class V2BucketConvertor implements IConvertor {
    
    @Override
    public String transBucketQuota(BucketQuota quota) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("Quota").elem("StorageQuota")
                    .text(String.valueOf(quota.getBucketQuota())).up();
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for storageQuota", e);
        }
    }
    
    @Override
    public String transBucketLoction(String location) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("CreateBucketConfiguration").elem("LocationConstraint")
                    .text(ServiceUtils.toValid(location));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
    public String transBucketEcryption(BucketEncryption encryption) throws ServiceException {
        String algorithm = encryption.getSseAlgorithm().getCode();
        String kmsKeyId = "";
        if (algorithm.equals(SSEAlgorithmEnum.KMS.getCode())) {
            algorithm = "aws:" + algorithm;
            kmsKeyId = encryption.getKmsKeyId();
        }
        return transBucketEcryptionXML(algorithm, kmsKeyId);
    }

    protected String transBucketEcryptionXML(String algorithm, String kmsKeyId) throws FactoryConfigurationError {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("ServerSideEncryptionConfiguration").e("Rule")
                    .e("ApplyServerSideEncryptionByDefault");
            builder.e("SSEAlgorithm").t(algorithm);
            if (ServiceUtils.isValid(kmsKeyId)) {
                builder.e("KMSMasterKeyID").t(kmsKeyId);
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for bucketEncryption", e);
        }
    }
    
    @Override
    public String transBucketLoggingConfiguration(BucketLoggingConfiguration c) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("BucketLoggingStatus");
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
        } catch (ParserConfigurationException e) {
            throw new ServiceException("Failed to build XML document for BucketLoggingConfiguration", e);
        } catch (TransformerException e) {
            throw new ServiceException("Failed to build XML document for BucketLoggingConfiguration", e);
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
                    subBuilder = buildCanonicalGrantee(grantee);
                } else if (grantee instanceof GroupGrantee) {
                    subBuilder = buildGroupGrantee(grantee);
                }

                if (subBuilder != null) {
                    grantsBuilder.elem("Grant").importXMLBuilder(subBuilder).elem("Permission")
                            .text(ServiceUtils.toValid(permission.getPermissionString()));
                }
            }
        }
    }

    protected OBSXMLBuilder buildGroupGrantee(GranteeInterface grantee)
            throws ParserConfigurationException, FactoryConfigurationError {
        OBSXMLBuilder subBuilder;
        subBuilder = OBSXMLBuilder.create("Grantee")
                .attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .attr("xsi:type", "Group").element("URI")
                .text(this.transGroupGrantee(((GroupGrantee) grantee).getGroupGranteeType()));
        return subBuilder;
    }

    protected OBSXMLBuilder buildCanonicalGrantee(GranteeInterface grantee)
            throws ParserConfigurationException, FactoryConfigurationError {
        OBSXMLBuilder subBuilder = null;
        subBuilder = OBSXMLBuilder.create("Grantee")
                .attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .attr("xsi:type", "CanonicalUser").element("ID")
                .text(ServiceUtils.toValid(grantee.getIdentifier()));
        String displayName = ((CanonicalGrantee) grantee).getDisplayName();
        if (ServiceUtils.isValid2(displayName)) {
            subBuilder.up().element("DisplayName").text(displayName);
        }
        return subBuilder;
    }
    
    @Override
    public String transBucketCors(BucketCors cors) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("CORSConfiguration");
            for (BucketCorsRule rule : cors.getRules()) {
                builder = builder.e("CORSRule");
                if (rule.getId() != null) {
                    builder.e("ID").t(rule.getId());
                }
                if (rule.getAllowedMethod() != null) {
                    for (String method : rule.getAllowedMethod()) {
                        builder.e("AllowedMethod").t(ServiceUtils.toValid(method));
                    }
                }

                if (rule.getAllowedOrigin() != null) {
                    for (String origin : rule.getAllowedOrigin()) {
                        builder.e("AllowedOrigin").t(ServiceUtils.toValid(origin));
                    }
                }

                if (rule.getAllowedHeader() != null) {
                    for (String header : rule.getAllowedHeader()) {
                        builder.e("AllowedHeader").t(ServiceUtils.toValid(header));
                    }
                }
                builder.e("MaxAgeSeconds").t(String.valueOf(rule.getMaxAgeSecond()));
                if (rule.getExposeHeader() != null) {
                    for (String exposeHeader : rule.getExposeHeader()) {
                        builder.e("ExposeHeader").t(ServiceUtils.toValid(exposeHeader));
                    }
                }
                builder = builder.up();
            }
            return builder.asString();
        } catch (ParserConfigurationException e) {
            throw new ServiceException("Failed to build XML document for cors", e);
        } catch (TransformerException e) {
            throw new ServiceException("Failed to build XML document for cors", e);
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for cors", e);
        }
    }
    
    @Override
    public String transBucketTagInfo(BucketTagInfo bucketTagInfo) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("Tagging").e("TagSet");
            for (BucketTagInfo.TagSet.Tag tag : bucketTagInfo.getTagSet().getTags()) {
                if (tag != null) {
                    builder.e("Tag").e("Key").t(ServiceUtils.toValid(tag.getKey())).up().e("Value")
                            .t(ServiceUtils.toValid(tag.getValue()));
                }
            }
            return builder.up().asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for Tagging", e);
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
                packNotificationConfig(builder, config, "TopicConfiguration", "Topic", "S3Key");
            }

            for (FunctionGraphConfiguration config : bucketNotificationConfiguration.getFunctionGraphConfigurations()) {
                packNotificationConfig(builder, config, "FunctionGraphConfiguration", "FunctionGraph", "S3Key");
            }

            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for Notification", e);
        }
    }
    
    protected void packNotificationConfig(OBSXMLBuilder builder, AbstractNotification config, String configType,
            String urnType, String adapter) {
        builder = builder.e(configType);
        if (config.getId() != null) {
            builder.e("Id").t(config.getId());
        }
        if (config.getFilter() != null && !config.getFilter().getFilterRules().isEmpty()) {
            builder = builder.e("Filter").e(adapter);
            for (AbstractNotification.Filter.FilterRule rule : config.getFilter().getFilterRules()) {
                if (rule != null) {
                    builder.e("FilterRule").e("Name").t(ServiceUtils.toValid(rule.getName())).up().e("Value")
                            .t(ServiceUtils.toValid(rule.getValue()));
                }
            }
            builder = builder.up().up();
        }
        String urn = null;
        if (config instanceof TopicConfiguration) {
            urn = ((TopicConfiguration) config).getTopic();
        }
        if (config instanceof FunctionGraphConfiguration) {
            urn = ((FunctionGraphConfiguration) config).getFunctionGraph();
        }
        if (urn != null) {
            builder.e(urnType).t(urn);
        }

        if (config.getEventTypes() != null) {
            for (EventTypeEnum event : config.getEventTypes()) {
                if (event != null) {
                    builder.e("Event").t(this.transEventType(event));
                }
            }
        }
    }
    
    @Override
    public String transBucketFileInterface(FSStatusEnum status) throws ServiceException {
        try {
            return OBSXMLBuilder.create("FileInterfaceConfiguration").e("Status").t(status.getCode()).up().asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for FileInterface", e);
        }
    }
    
    @Override
    public String transBucketDirectColdAccess(BucketDirectColdAccess access) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("DirectColdAccessConfiguration");

            builder = builder.e("Status").t(access.getStatus().getCode());
            builder = builder.up();

            return builder.up().asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for Tagging", e);
        }
    }
}
