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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.xml.OBSXMLBuilder;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.CanonicalGrantee;
import com.obs.services.model.EventTypeEnum;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GranteeInterface;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.GroupGranteeEnum;
import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.LifecycleConfiguration.NoncurrentVersionTransition;
import com.obs.services.model.LifecycleConfiguration.Rule;
import com.obs.services.model.LifecycleConfiguration.Transition;
import com.obs.services.model.Owner;
import com.obs.services.model.PartEtag;
import com.obs.services.model.Permission;
import com.obs.services.model.Redirect;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RouteRule;
import com.obs.services.model.RouteRuleCondition;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.WebsiteConfiguration;

public class V2Convertor extends V2BucketConvertor {

    private static IConvertor instance = new V2Convertor();

    public static IConvertor getInstance() {
        return instance;
    }

    public static String getEncodedString(String value, String encodingType) {
        if (encodingType != null && encodingType.toLowerCase().equals("url")) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException exception) {
                throw new ServiceException(exception);
            }
        }
        return value;
    }

    @Override
    public String transCompleteMultipartUpload(List<PartEtag> parts) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("CompleteMultipartUpload");
            Collections.sort(parts, new Comparator<PartEtag>() {
                @Override
                public int compare(PartEtag o1, PartEtag o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    if (o1 == null) {
                        return -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    return o1.getPartNumber().compareTo(o2.getPartNumber());
                }

            });
            for (PartEtag part : parts) {
                builder.e("Part").e("PartNumber").t(part.getPartNumber() == null ? "" : part.getPartNumber().toString())
                        .up().e("ETag").t(ServiceUtils.toValid(part.getEtag()));
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    @Override
    public String transVersioningConfiguration(String bucketName, String status) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("VersioningConfiguration").elem("Status")
                    .text(ServiceUtils.toValid(status));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for versioning", e);
        }
    }

    @Override
    public String transRequestPaymentConfiguration(String bucketName, String payer) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("RequestPaymentConfiguration").elem("Payer")
                    .text(ServiceUtils.toValid(payer));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for requestPayment", e);
        }
    }

    @Override
    public String transLifecycleConfiguration(LifecycleConfiguration config) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("LifecycleConfiguration");
            for (Rule rule : config.getRules()) {
                OBSXMLBuilder b = builder.elem("Rule");
                if (ServiceUtils.isValid2(rule.getId())) {
                    b.elem("ID").t(rule.getId());
                }
                if (rule.getPrefix() != null) {
                    b.elem("Prefix").t(ServiceUtils.toValid(rule.getPrefix()));
                }
                b.elem("Status").t(rule.getEnabled() ? "Enabled" : "Disabled");

                if (rule.getTransitions() != null) {
                    transTransitions(rule, b);
                }

                if (rule.getExpiration() != null) {
                    transExpiration(rule, b);
                }

                if (rule.getNoncurrentVersionTransitions() != null) {
                    transNoncurrentVersionTransition(rule, b);
                }

                if (rule.getNoncurrentVersionExpiration() != null
                        && rule.getNoncurrentVersionExpiration().getDays() != null) {
                    OBSXMLBuilder noncurrentVersionBuilder = b.elem("NoncurrentVersionExpiration");
                    noncurrentVersionBuilder.elem("NoncurrentDays").t(
                            rule.getNoncurrentVersionExpiration().getDays().toString());
                }
            }
            return builder.asString();
        } catch (ParserConfigurationException e) {
            throw new ServiceException("Failed to build XML document for lifecycle", e);
        } catch (TransformerException e) {
            throw new ServiceException("Failed to build XML document for lifecycle", e);
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for lifecycle", e);
        }
    }

    private void transNoncurrentVersionTransition(Rule rule, OBSXMLBuilder b) {
        for (NoncurrentVersionTransition noncurrentVersionTransition : rule
                .getNoncurrentVersionTransitions()) {
            if (noncurrentVersionTransition.getObjectStorageClass() != null
                    && noncurrentVersionTransition.getDays() != null) {
                OBSXMLBuilder noncurrentVersionBuilder = b.elem("NoncurrentVersionTransition");
                noncurrentVersionBuilder.elem("NoncurrentDays").t(
                        noncurrentVersionTransition.getDays().toString());
                noncurrentVersionBuilder.elem("StorageClass")
                        .t(this.transStorageClass(noncurrentVersionTransition.getObjectStorageClass()));
            }
        }
    }

    private void transExpiration(Rule rule, OBSXMLBuilder b) {
        OBSXMLBuilder expirationBuilder = b.elem("Expiration");
        if (rule.getExpiration().getDate() != null) {
            expirationBuilder.elem("Date").t(
                    ServiceUtils.formatIso8601MidnightDate(rule.getExpiration().getDate()));
        } else if (rule.getExpiration().getDays() != null) {
            expirationBuilder.elem("Days").t(rule.getExpiration().getDays().toString());
        }
    }

    private void transTransitions(Rule rule, OBSXMLBuilder b) {
        for (Transition transition : rule.getTransitions()) {
            if (transition.getObjectStorageClass() != null) {
                OBSXMLBuilder transitionBuilder = b.elem("Transition");
                if (transition.getDate() != null) {
                    transitionBuilder.elem("Date").t(
                            ServiceUtils.formatIso8601MidnightDate(transition.getDate()));
                } else if (transition.getDays() != null) {
                    transitionBuilder.elem("Days").t(transition.getDays().toString());
                }
                transitionBuilder.elem("StorageClass").t(
                        this.transStorageClass(transition.getObjectStorageClass()));
            }
        }
    }

    @Override
    public String transWebsiteConfiguration(WebsiteConfiguration config) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("WebsiteConfiguration");
            if (config.getRedirectAllRequestsTo() != null) {
                if (null != config.getRedirectAllRequestsTo().getHostName()) {
                    builder = builder.elem("RedirectAllRequestsTo").elem("HostName")
                            .text(ServiceUtils.toValid(config.getRedirectAllRequestsTo().getHostName()));
                }
                if (null != config.getRedirectAllRequestsTo().getRedirectProtocol()) {
                    builder = builder.up().elem("Protocol")
                            .text(config.getRedirectAllRequestsTo().getRedirectProtocol().getCode());
                }
                return builder.asString();
            }
            if (ServiceUtils.isValid2(config.getSuffix())) {
                builder.elem("IndexDocument").elem("Suffix").text(config.getSuffix());
            }
            if (ServiceUtils.isValid2(config.getKey())) {
                builder.elem("ErrorDocument").elem("Key").text(config.getKey());
            }
            if (null != config.getRouteRules() && config.getRouteRules().size() > 0) {
                builder = builder.elem("RoutingRules");
                for (RouteRule routingRule : config.getRouteRules()) {
                    builder = transWebsiteRoutingRule(builder, routingRule);
                }
                builder = builder.up();
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for website", e);
        }
    }

    private OBSXMLBuilder transWebsiteRoutingRule(OBSXMLBuilder builder, RouteRule routingRule) {
        builder = builder.elem("RoutingRule");
        RouteRuleCondition condition = routingRule.getCondition();
        if (null != condition) {
            builder = builder.elem("Condition");
            String keyPrefixEquals = condition.getKeyPrefixEquals();
            String hecre = condition.getHttpErrorCodeReturnedEquals();
            if (ServiceUtils.isValid2(keyPrefixEquals)) {
                builder = builder.elem("KeyPrefixEquals").text(keyPrefixEquals);
                builder = builder.up();
            }
            if (ServiceUtils.isValid2(hecre)) {
                builder = builder.elem("HttpErrorCodeReturnedEquals").text(hecre);
                builder = builder.up();
            }
            builder = builder.up();
        }
        
        Redirect redirect = routingRule.getRedirect();
        if (null != redirect) {
            builder = builder.elem("Redirect");
            String hostName = redirect.getHostName();
            String repalceKeyWith = redirect.getReplaceKeyWith();
            String replaceKeyPrefixWith = redirect.getReplaceKeyPrefixWith();
            String redirectCode = redirect.getHttpRedirectCode();
            if (ServiceUtils.isValid2(hostName)) {
                builder = builder.elem("HostName").text(hostName);
                builder = builder.up();
            }
            if (ServiceUtils.isValid2(redirectCode)) {
                builder = builder.elem("HttpRedirectCode").text(redirectCode);
                builder = builder.up();
            }
            if (ServiceUtils.isValid2(repalceKeyWith)) {
                builder = builder.elem("ReplaceKeyWith").text(repalceKeyWith);
                builder = builder.up();
            }
            if (ServiceUtils.isValid2(replaceKeyPrefixWith)) {
                builder = builder.elem("ReplaceKeyPrefixWith").text(replaceKeyPrefixWith);
                builder = builder.up();
            }
            if (redirect.getRedirectProtocol() != null) {
                builder = builder.elem("Protocol").text(redirect.getRedirectProtocol().getCode());
                builder = builder.up();
            }
            builder = builder.up();
        }
        builder = builder.up();
        return builder;
    }

    @Override
    public String transRestoreObjectRequest(RestoreObjectRequest req) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("RestoreRequest")
                    .elem("Days").t(String.valueOf(req.getDays())).up();
            if (req.getRestoreTier() != null) {
                builder.e("GlacierJobParameters").e("Tier").t(req.getRestoreTier().getCode());
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for restoreobject", e);
        }
    }

    @Override
    public String transStoragePolicy(BucketStoragePolicyConfiguration status) throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("StoragePolicy").elem("DefaultStorageClass")
                    .text(this.transStorageClass(status.getBucketStorageClass()));
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for StoragePolicy", e);
        }
    }

    @Override
    public String transAccessControlList(AccessControlList acl, boolean isBucket) throws ServiceException {
        Owner owner = acl.getOwner();
        GrantAndPermission[] grants = acl.getGrantAndPermissions();

        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("AccessControlPolicy");
            if (owner != null) {
                builder = builder.elem("Owner").elem("ID").text(ServiceUtils.toValid(owner.getId()));
                if (null != owner.getDisplayName()) {
                    builder.up().elem("DisplayName").text(owner.getDisplayName());
                }
                builder = builder.up().up();
            }
            if (grants.length > 0) {
                OBSXMLBuilder accessControlList = builder.elem("AccessControlList");
                for (GrantAndPermission gap : grants) {
                    GranteeInterface grantee = gap.getGrantee();
                    Permission permission = gap.getPermission();
                    OBSXMLBuilder subBuilder = null;
                    if (grantee instanceof CanonicalGrantee) {
                        subBuilder = buildCanonicalGrantee(grantee);
                    } else if (grantee instanceof GroupGrantee) {
                        subBuilder = buildGroupGrantee(grantee);
                    } else if (grantee != null) {
                        subBuilder = OBSXMLBuilder.create("Grantee")
                                .attr("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                                .attr("xsi:type", "CanonicalUser").element("ID")
                                .text(ServiceUtils.toValid(grantee.getIdentifier()));
                    }

                    if (subBuilder != null) {
                        OBSXMLBuilder grantBuilder = accessControlList.elem("Grant").importXMLBuilder(subBuilder);
                        if (permission != null) {
                            grantBuilder.elem("Permission")
                                    .text(ServiceUtils.toValid(permission.getPermissionString()));
                        }
                    }
                }
            }
            return builder.asString();
        } catch (ParserConfigurationException e) {
            throw new ServiceException("Failed to build XML document for ACL", e);
        } catch (TransformerException e) {
            throw new ServiceException("Failed to build XML document for ACL", e);
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document for ACL", e);
        }
    }

    public String transKeyAndVersion(KeyAndVersion[] objectNameAndVersions, boolean isQuiet, String encodingType)
            throws ServiceException {
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("Delete").elem("Quiet").text(String.valueOf(isQuiet)).up();
            if (encodingType != null) {
                builder.elem("EncodingType").text(encodingType);
            }
            for (KeyAndVersion nav : objectNameAndVersions) {
                String encodedString = getEncodedString(ServiceUtils.toValid(nav.getKey()), encodingType);
                OBSXMLBuilder objectBuilder = builder.elem("Object").elem("Key").text(encodedString).up();
                if (ServiceUtils.isValid(nav.getVersion())) {
                    objectBuilder.elem("VersionId").text(nav.getVersion());
                }
            }
            return builder.asString();
        } catch (Exception e) {
            throw new ServiceException("Failed to build XML document", e);
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
                    builder = builder.e("Destination").e("Bucket")
                            .t(bucketName.startsWith("arn:aws:s3:::") ? bucketName : "arn:aws:s3:::" + bucketName).up();
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
        String eventTypeStr = "";
        if (eventType != null) {
            switch (eventType) {
                case OBJECT_CREATED_ALL:
                    eventTypeStr = "s3:ObjectCreated:*";
                    break;
                case OBJECT_CREATED_PUT:
                    eventTypeStr = "s3:ObjectCreated:Put";
                    break;
                case OBJECT_CREATED_POST:
                    eventTypeStr = "s3:ObjectCreated:Post";
                    break;
                case OBJECT_CREATED_COPY:
                    eventTypeStr = "s3:ObjectCreated:Copy";
                    break;
                case OBJECT_CREATED_COMPLETE_MULTIPART_UPLOAD:
                    eventTypeStr = "s3:ObjectCreated:CompleteMultipartUpload";
                    break;
                case OBJECT_REMOVED_ALL:
                    eventTypeStr = "s3:ObjectRemoved:*";
                    break;
                case OBJECT_REMOVED_DELETE:
                    eventTypeStr = "s3:ObjectRemoved:Delete";
                    break;
                case OBJECT_REMOVED_DELETE_MARKER_CREATED:
                    eventTypeStr = "s3:ObjectRemoved:DeleteMarkerCreated";
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
                    storageClassStr = "STANDARD_IA";
                    break;
                case COLD:
                    storageClassStr = "GLACIER";
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
            return AccessControlList.REST_CANNED_PUBLIC_READ;
        } else if (Constants.ACL_PUBLIC_READ_WRITE_DELIVERED.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_PUBLIC_READ_WRITE;
        } else if (Constants.ACL_AUTHENTICATED_READ.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_AUTHENTICATED_READ;
        } else if (Constants.ACL_BUCKET_OWNER_READ.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_BUCKET_OWNER_READ;
        } else if (Constants.ACL_BUCKET_OWNER_FULL_CONTROL.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_BUCKET_OWNER_FULL_CONTROL;
        } else if (Constants.ACL_LOG_DELIVERY_WRITE.equals(cannedAcl)) {
            return AccessControlList.REST_CANNED_LOG_DELIVERY_WRITE;
        }
        return null;
    }

    @Override
    public String transGroupGrantee(GroupGranteeEnum groupGrantee) {
        String groupGranteeStr = "";
        if (groupGrantee != null) {
            switch (groupGrantee) {
                case ALL_USERS:
                    groupGranteeStr = Constants.ALL_USERS_URI;
                    break;
                case AUTHENTICATED_USERS:
                    groupGranteeStr = Constants.AUTHENTICATED_USERS_URI;
                    break;
                case LOG_DELIVERY:
                    groupGranteeStr = Constants.LOG_DELIVERY_URI;
                    break;
                default:
                    break;
            }
        }
        return groupGranteeStr;
    }

}
