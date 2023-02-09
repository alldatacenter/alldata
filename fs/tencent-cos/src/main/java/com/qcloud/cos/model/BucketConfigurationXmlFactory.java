/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.model;

import java.util.List;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.internal.XmlWriter;
import com.qcloud.cos.model.BucketLifecycleConfiguration.NoncurrentVersionTransition;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Rule;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Transition;
import com.qcloud.cos.model.CORSRule.AllowedMethods;
import com.qcloud.cos.model.Tag.LifecycleTagPredicate;
import com.qcloud.cos.model.Tag.Tag;
import com.qcloud.cos.model.inventory.InventoryConfiguration;
import com.qcloud.cos.model.inventory.InventoryCosBucketDestination;
import com.qcloud.cos.model.inventory.InventoryDestination;
import com.qcloud.cos.model.inventory.InventoryEncryption;
import com.qcloud.cos.model.inventory.InventoryFilter;
import com.qcloud.cos.model.inventory.InventoryFilterPredicate;
import com.qcloud.cos.model.inventory.InventoryPrefixPredicate;
import com.qcloud.cos.model.inventory.InventorySchedule;
import com.qcloud.cos.model.inventory.ServerSideEncryptionCOS;
import com.qcloud.cos.model.lifecycle.LifecycleAndOperator;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePredicateVisitor;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.qcloud.cos.utils.CollectionUtils;
import com.qcloud.cos.utils.DateUtils;


/**
 * Converts bucket configuration objects into XML byte arrays.
 */
public class BucketConfigurationXmlFactory {

    /**
     * Converts the specified {@link BucketCrossOriginConfiguration} object to an XML fragment that
     * can be sent to Qcloud COS.
     *
     * @param config The {@link BucketCrossOriginConfiguration}
     */
    /*
     * <CORSConfiguration>
             <CORSRule>
               <AllowedOrigin>http://www.foobar.com</AllowedOrigin>
               <AllowedMethod>GET</AllowedMethod>
               <MaxAgeSeconds>3000</MaxAgeSec>
               <ExposeHeader>x-cos-meta</ExposeHeader>
             </CORSRule>
       </CORSConfiguration>
     */
    public byte[] convertToXmlByteArray(BucketCrossOriginConfiguration config)
            throws CosClientException {

        XmlWriter xml = new XmlWriter();
        xml.start("CORSConfiguration");

        for (CORSRule rule : config.getRules()) {
            writeRule(xml, rule);
        }

        xml.end();

        return xml.getBytes();
    }

    /**
     * Converts the specified logging configuration into an XML byte array.
     *
     * @param loggingConfiguration
     *            The configuration to convert.
     *
     * @return The XML byte array representation.
     */
    public byte[] convertToXmlByteArray(BucketLoggingConfiguration loggingConfiguration) {
        // Default log file prefix to the empty string if none is specified
        String logFilePrefix = loggingConfiguration.getLogFilePrefix();
        if (logFilePrefix == null)
            logFilePrefix = "";

        XmlWriter xml = new XmlWriter();
        xml.start("BucketLoggingStatus");
        if (loggingConfiguration.isLoggingEnabled()) {
            xml.start("LoggingEnabled");
            xml.start("TargetBucket").value(loggingConfiguration.getDestinationBucketName()).end();
            xml.start("TargetPrefix").value(loggingConfiguration.getLogFilePrefix()).end();
            xml.end();
        }
        xml.end();

        return xml.getBytes();
    }


    /**
     * Converts the specified versioning configuration into an XML byte array.
     *
     * @param versioningConfiguration The configuration to convert.
     *
     * @return The XML byte array representation.
     */
    public byte[] convertToXmlByteArray(BucketVersioningConfiguration versioningConfiguration) {
        XmlWriter xml = new XmlWriter();
        xml.start("VersioningConfiguration");
        xml.start("Status").value(versioningConfiguration.getStatus()).end();

        xml.end();

        return xml.getBytes();
    }

    public byte[] convertToXmlByteArray(InventoryConfiguration config) throws CosClientException {
        XmlWriter xml = new XmlWriter();
        xml.start("InventoryConfiguration");

        xml.start("Id").value(config.getId()).end();
        xml.start("IsEnabled").value(String.valueOf(config.isEnabled())).end();
        xml.start("IncludedObjectVersions").value(config.getIncludedObjectVersions()).end();

        writeInventoryDestination(xml, config.getDestination());
        writeInventoryFilter(xml, config.getInventoryFilter());
        addInventorySchedule(xml, config.getSchedule());
        addInventoryOptionalFields(xml, config.getOptionalFields());

        xml.end(); // </InventoryConfiguration>

        return xml.getBytes();
    }

    public byte[] convertToXmlByteArray(BucketTaggingConfiguration config) throws CosClientException {

        XmlWriter xml = new XmlWriter();
        xml.start("Tagging");

        for (TagSet tagset : config.getAllTagSets()) {
            writeRule(xml, tagset);
        }

        xml.end();

        return xml.getBytes();
    }

    private void writeInventoryDestination(XmlWriter xml, InventoryDestination destination) {
        if (destination == null) {
            return;
        }

        xml.start("Destination");
        InventoryCosBucketDestination s3BucketDestination = destination.getCosBucketDestination();
        if (s3BucketDestination != null) {
            xml.start("COSBucketDestination");
            addParameterIfNotNull(xml, "AccountId", s3BucketDestination.getAccountId());
            addParameterIfNotNull(xml, "Bucket", s3BucketDestination.getBucketArn());
            addParameterIfNotNull(xml, "Prefix", s3BucketDestination.getPrefix());
            addParameterIfNotNull(xml, "Format", s3BucketDestination.getFormat());
            writeInventoryEncryption(xml, s3BucketDestination.getEncryption());
            xml.end(); // </COSBucketDestination>
        }
        xml.end(); // </Destination>
    }

    private void writeInventoryEncryption(XmlWriter xml, InventoryEncryption encryption) {
        if (encryption == null) {
            return;
        }
        xml.start("Encryption");
        if (encryption instanceof ServerSideEncryptionCOS) {
            xml.start("SSE-COS").end();
        }
        xml.end();
    }

    private void writeInventoryFilter(XmlWriter xml, InventoryFilter inventoryFilter) {
        if (inventoryFilter == null) {
            return;
        }

        xml.start("Filter");
        writeInventoryFilterPredicate(xml, inventoryFilter.getPredicate());
        xml.end();
    }

    private void writeInventoryFilterPredicate(XmlWriter xml, InventoryFilterPredicate predicate) {
        if (predicate == null) {
            return;
        }

        if (predicate instanceof InventoryPrefixPredicate) {
            writePrefix(xml, ((InventoryPrefixPredicate) predicate).getPrefix());
        }
    }

    private void addInventorySchedule(XmlWriter xml, InventorySchedule schedule) {
        if (schedule == null) {
            return;
        }

        xml.start("Schedule");
        addParameterIfNotNull(xml, "Frequency", schedule.getFrequency());
        xml.end();
    }

    private void addInventoryOptionalFields(XmlWriter xml, List<String> optionalFields) {
        if (CollectionUtils.isNullOrEmpty(optionalFields)) {
            return;
        }

        xml.start("OptionalFields");
        for (String field : optionalFields) {
            xml.start("Field").value(field).end();
        }
        xml.end();
    }

    public byte[] convertToXmlByteArray(BucketLifecycleConfiguration config)
            throws CosClientException {

        XmlWriter xml = new XmlWriter();
        xml.start("LifecycleConfiguration");

        for (Rule rule : config.getRules()) {
            writeRule(xml, rule);
        }

        xml.end();

        return xml.getBytes();
    }

    /**
     * Converts the specified website configuration into an XML byte array to
     * send to COS.
     *
     * Sample XML:
     * <WebsiteConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
     *    <IndexDocument>
     *      <Suffix>index.html</Suffix>
     *    </IndexDocument>
     *    <ErrorDocument>
     *      <Key>404.html</Key>
     *    </ErrorDocument>
     *  </WebsiteConfiguration>
     *
     * @param websiteConfiguration
     *            The configuration to convert.
     * @return The XML byte array representation.
     */
    public byte[] convertToXmlByteArray(BucketWebsiteConfiguration websiteConfiguration) {
        XmlWriter xml = new XmlWriter();
        xml.start("WebsiteConfiguration");

        if (websiteConfiguration.getIndexDocumentSuffix() != null) {
            XmlWriter indexDocumentElement = xml.start("IndexDocument");
            indexDocumentElement.start("Suffix").value(websiteConfiguration.getIndexDocumentSuffix()).end();
            indexDocumentElement.end();
        }

        if (websiteConfiguration.getErrorDocument() != null) {
            XmlWriter errorDocumentElement = xml.start("ErrorDocument");
            errorDocumentElement.start("Key").value(websiteConfiguration.getErrorDocument()).end();
            errorDocumentElement.end();
        }

        RedirectRule redirectAllRequestsTo = websiteConfiguration.getRedirectAllRequestsTo();
        if (redirectAllRequestsTo != null) {
            XmlWriter redirectAllRequestsElement = xml.start("RedirectAllRequestsTo");
            if (redirectAllRequestsTo.getprotocol() != null) {
                xml.start("Protocol").value(redirectAllRequestsTo.getprotocol()).end();
            }

            if (redirectAllRequestsTo.getHostName() != null) {
                xml.start("HostName").value(redirectAllRequestsTo.getHostName()).end();
            }

            if (redirectAllRequestsTo.getReplaceKeyPrefixWith() != null) {
                xml.start("ReplaceKeyPrefixWith").value(redirectAllRequestsTo.getReplaceKeyPrefixWith()).end();
            }

            if (redirectAllRequestsTo.getReplaceKeyWith() != null) {
                xml.start("ReplaceKeyWith").value(redirectAllRequestsTo.getReplaceKeyWith()).end();
            }
            redirectAllRequestsElement.end();
        }

        if (websiteConfiguration.getRoutingRules() != null && websiteConfiguration.getRoutingRules().size() > 0) {

            XmlWriter routingRules = xml.start("RoutingRules");
            for (RoutingRule rule : websiteConfiguration.getRoutingRules()) {
                writeRule(routingRules, rule);
            }

            routingRules.end();
        }

        xml.end();
        return xml.getBytes();
    }

    private void writeRule(XmlWriter xml, CORSRule rule) {
        xml.start("CORSRule");
        if (rule.getId() != null) {
            xml.start("ID").value(rule.getId()).end();
        }
        if (rule.getAllowedOrigins() != null) {
            for (String origin : rule.getAllowedOrigins()) {
                xml.start("AllowedOrigin").value(origin).end();
            }
        }
        if (rule.getAllowedMethods() != null) {
            for (AllowedMethods method : rule.getAllowedMethods()) {
                xml.start("AllowedMethod").value(method.toString()).end();
            }
        }
        if (rule.getMaxAgeSeconds() != 0) {
            xml.start("MaxAgeSeconds").value(Integer.toString(rule.getMaxAgeSeconds())).end();
        }
        if (rule.getExposedHeaders() != null) {
            for (String header : rule.getExposedHeaders()) {
                xml.start("ExposeHeader").value(header).end();
            }
        }
        if (rule.getAllowedHeaders() != null) {
            for (String header : rule.getAllowedHeaders()) {
                xml.start("AllowedHeader").value(header).end();
            }
        }
        xml.end();// </CORSRule>
    }

    private void writeRule(XmlWriter xml, Rule rule) {
        xml.start("Rule");
        if (rule.getId() != null) {
            xml.start("ID").value(rule.getId()).end();
        }
        xml.start("Status").value(rule.getStatus()).end();
        writeLifecycleFilter(xml, rule.getFilter());

        addTransitions(xml, rule.getTransitions());
        addNoncurrentTransitions(xml, rule.getNoncurrentVersionTransitions());

        if (hasCurrentExpirationPolicy(rule)) {
            // The rule attributes below are mutually exclusive, the service will throw an error if
            // more than one is provided
            xml.start("Expiration");
            if (rule.getExpirationInDays() != -1) {
                xml.start("Days").value("" + rule.getExpirationInDays()).end();
            }
            if (rule.getExpirationDate() != null) {
                xml.start("Date").value(DateUtils.formatISO8601Date(rule.getExpirationDate()))
                        .end();
            }
            if (rule.isExpiredObjectDeleteMarker() == true) {
                xml.start("ExpiredObjectDeleteMarker").value("true").end();
            }
            xml.end(); // </Expiration>
        }

        if (rule.getNoncurrentVersionExpirationInDays() != -1) {
            xml.start("NoncurrentVersionExpiration");
            xml.start("NoncurrentDays")
                    .value(Integer.toString(rule.getNoncurrentVersionExpirationInDays())).end();
            xml.end(); // </NoncurrentVersionExpiration>
        }

        if (rule.getAbortIncompleteMultipartUpload() != null) {
            xml.start("AbortIncompleteMultipartUpload");
            xml.start("DaysAfterInitiation")
                    .value(Integer.toString(
                            rule.getAbortIncompleteMultipartUpload().getDaysAfterInitiation()))
                    .end();
            xml.end(); // </AbortIncompleteMultipartUpload>
        }

        xml.end(); // </Rule>
    }

    private void writeRule(XmlWriter xml, RoutingRule rule) {
        xml.start("RoutingRule");
        RoutingRuleCondition condition = rule.getCondition();
        if (condition != null) {
            xml.start("Condition");
            xml.start("KeyPrefixEquals");
            if (condition.getKeyPrefixEquals() != null) {
                xml.value(condition.getKeyPrefixEquals());
            }
            xml.end(); // </KeyPrefixEquals">

            if (condition.getHttpErrorCodeReturnedEquals() != null) {
                xml.start("HttpErrorCodeReturnedEquals ").value(condition.getHttpErrorCodeReturnedEquals()).end();
            }

            xml.end(); // </Condition>
        }

        xml.start("Redirect");
        RedirectRule redirect = rule.getRedirect();
        if (redirect != null) {
            if (redirect.getprotocol() != null) {
                xml.start("Protocol").value(redirect.getprotocol()).end();
            }

            if (redirect.getHostName() != null) {
                xml.start("HostName").value(redirect.getHostName()).end();
            }

            if (redirect.getReplaceKeyPrefixWith() != null) {
                xml.start("ReplaceKeyPrefixWith").value(redirect.getReplaceKeyPrefixWith()).end();
            }

            if (redirect.getReplaceKeyWith() != null) {
                xml.start("ReplaceKeyWith").value(redirect.getReplaceKeyWith()).end();
            }

            if (redirect.getHttpRedirectCode() != null) {
                xml.start("HttpRedirectCode").value(redirect.getHttpRedirectCode()).end();
            }
        }
        xml.end(); // </Redirect>
        xml.end();// </CORSRule>
    }

    private void writeRule(XmlWriter xml, DomainRule rule) {
        xml.start("DomainRule");
        xml.start("Status").value(rule.getStatus()).end();
        xml.start("Name").value(rule.getName()).end();
        xml.start("Type").value(rule.getType()).end();
        if(rule.getForcedReplacement() != null) {
            xml.start("ForcedReplacement").value(rule.getForcedReplacement()).end();
        }
        xml.end();// </DomainRule>
    }

    private void writeRule(XmlWriter xml, TagSet tagset) {
        xml.start("TagSet");
        for ( String key : tagset.getAllTags().keySet() ) {
            xml.start("Tag");
            xml.start("Key").value(key).end();
            xml.start("Value").value(tagset.getTag(key)).end();
            xml.end(); // </Tag>
        }
        xml.end(); // </TagSet>
    }

    private void addTransitions(XmlWriter xml, List<Transition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return;
        }

        for (Transition t : transitions) {
            if (t != null) {
                xml.start("Transition");
                if (t.getDate() != null) {
                    xml.start("Date");
                    xml.value(DateUtils.formatISO8601Date(t.getDate()));
                    xml.end();
                }
                if (t.getDays() != -1) {
                    xml.start("Days");
                    xml.value(Integer.toString(t.getDays()));
                    xml.end();
                }

                xml.start("StorageClass");
                xml.value(t.getStorageClass().toString());
                xml.end(); // <StorageClass>
                xml.end(); // </Transition>
            }
        }
    }

    private void addNoncurrentTransitions(XmlWriter xml,
            List<NoncurrentVersionTransition> transitions) {
        if (transitions == null || transitions.isEmpty()) {
            return;
        }

        for (NoncurrentVersionTransition t : transitions) {
            if (t != null) {
                xml.start("NoncurrentVersionTransition");
                if (t.getDays() != -1) {
                    xml.start("NoncurrentDays");
                    xml.value(Integer.toString(t.getDays()));
                    xml.end();
                }

                xml.start("StorageClass");
                xml.value(t.getStorageClassAsString());
                xml.end(); // </StorageClass>
                xml.end(); // </NoncurrentVersionTransition>
            }
        }
    }

    private void writeLifecycleFilter(XmlWriter xml, LifecycleFilter filter) {
        if (filter == null) {
            return;
        }

        xml.start("Filter");
        writeLifecycleFilterPredicate(xml, filter.getPredicate());
        xml.end();
    }

    private void writeLifecycleFilterPredicate(XmlWriter xml, LifecycleFilterPredicate predicate) {
        if (predicate == null) {
            return;
        }
        predicate.accept(new LifecyclePredicateVisitorImpl(xml));
    }

    private class LifecyclePredicateVisitorImpl implements LifecyclePredicateVisitor {
        private final XmlWriter xml;

        public LifecyclePredicateVisitorImpl(XmlWriter xml) {
            this.xml = xml;
        }

        @Override
        public void visit(LifecyclePrefixPredicate lifecyclePrefixPredicate) {
            writePrefix(xml, lifecyclePrefixPredicate.getPrefix());
        }

        @Override
        public void visit(LifecycleTagPredicate lifecycleTagPredicate) {
            writeTag(xml, lifecycleTagPredicate.getTag());
        }

        @Override
        public void visit(LifecycleAndOperator lifecycleAndOperator) {
            xml.start("And");
            for (LifecycleFilterPredicate predicate : lifecycleAndOperator.getOperands()) {
                predicate.accept(this);
            }
            xml.end(); // </And>
        }
    }

    public byte[] convertToXmlByteArray(BucketReplicationConfiguration replicationConfiguration) {
        XmlWriter xml = new XmlWriter();
        xml.start("ReplicationConfiguration");
        List<ReplicationRule> rules = replicationConfiguration.getRules();

        final String role = replicationConfiguration.getRoleName();
        xml.start("Role").value(role).end();
        for (ReplicationRule rule : rules) {
            final String ruleId = rule.getID();

            xml.start("Rule");
            xml.start("ID").value(ruleId).end();
            xml.start("Prefix").value(rule.getPrefix()).end();
            xml.start("Status").value(rule.getStatus()).end();

            final ReplicationDestinationConfig config = rule.getDestinationConfig();
            xml.start("Destination");
            xml.start("Bucket").value(config.getBucketQCS()).end();
            if (config.getStorageClass() != null) {
                xml.start("StorageClass").value(config.getStorageClass()).end();
            }
            xml.end();

            xml.end();
        }
        xml.end();
        return xml.getBytes();
    }

    public byte[] convertToXmlByteArray(BucketDomainConfiguration domainConfiguration) {
        XmlWriter xml = new XmlWriter();
        xml.start("DomainConfiguration");
        for (DomainRule rule : domainConfiguration.getDomainRules()) {
            writeRule(xml, rule);
        }
        xml.end();
        return xml.getBytes();
    }

    public byte[] convertToXmlByteArray(BucketRefererConfiguration refererConfiguration) {
        XmlWriter xml = new XmlWriter();

        xml.start("RefererConfiguration");

        xml.start("Status").value(refererConfiguration.getStatus()).end();
        xml.start("RefererType").value(refererConfiguration.getRefererType()).end();

        xml.start("DomainList");
        for (String domain : refererConfiguration.getDomainList()) {
            xml.start("Domain").value(domain).end();
        }
        xml.end();

        String emptyReferConfiguration = refererConfiguration.getEmptyReferConfiguration();
        if (emptyReferConfiguration != null &&
                (emptyReferConfiguration == BucketRefererConfiguration.DENY || emptyReferConfiguration == BucketRefererConfiguration.ALLOW)) {

            xml.start("EmptyReferConfiguration").value(emptyReferConfiguration).end();
        }

        xml.end();
        return xml.getBytes();
    }

    /**
     * @param rule
     * @return True if rule has a current expiration (<Expiration/>) policy set
     */
    private boolean hasCurrentExpirationPolicy(Rule rule) {
        return rule.getExpirationInDays() != -1 || rule.getExpirationDate() != null
                || rule.isExpiredObjectDeleteMarker();
    }

    private void addParameterIfNotNull(XmlWriter xml, String xmlTagName, String value) {
        if (value != null) {
            xml.start(xmlTagName).value(value).end();
        }
    }

    private void writePrefix(XmlWriter xml, String prefix) {
        addParameterIfNotNull(xml, "Prefix", prefix);
    }

    private void writeTag(XmlWriter xml, Tag tag) {
        if (tag == null) {
            return;
        }
        xml.start("Tag");
        xml.start("Key").value(tag.getKey()).end();
        xml.start("Value").value(tag.getValue()).end();
        xml.end();
    }


    public byte[] convertToXmlByteArray(BucketIntelligentTierConfiguration configuration)
            throws CosClientException {

        XmlWriter xml = new XmlWriter();
        xml.start("IntelligentTieringConfiguration");
        String status = configuration.getStatus();
        xml.start("Status").value(status).end();
        BucketIntelligentTierConfiguration.Transition transition = configuration.getTransition();
        if(status.equals(BucketIntelligentTierConfiguration.ENABLED) && transition != null) {
            xml.start("Transition");
            xml.start("Days").value(Integer.toString(transition.getDays())).end();
            xml.start("RequestFrequent").value(Integer.toString(transition.getRequestFrequent())).end();
            xml.end();
        }

        xml.end();

        return xml.getBytes();
    }

}
