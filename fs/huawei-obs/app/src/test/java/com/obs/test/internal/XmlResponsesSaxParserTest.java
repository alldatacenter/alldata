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

package com.obs.test.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.handler.XmlResponsesSaxParser.BucketEncryptionHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.BucketNotificationConfigurationHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.BucketReplicationConfigurationHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.BucketStorageInfoHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.BucketWebsiteConfigurationHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.ListBucketsHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.ListMultipartUploadsHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.ListObjectsHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.ListPartsHandler;
import com.obs.services.internal.handler.XmlResponsesSaxParser.ListVersionsHandler;
import com.obs.services.internal.io.HttpMethodReleaseInputStream;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.MultipartUpload;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.ObsObject;
import com.obs.services.model.Owner;
import com.obs.services.model.ProtocolEnum;
import com.obs.services.model.Redirect;
import com.obs.services.model.RedirectAllRequest;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RouteRule;
import com.obs.services.model.RouteRuleCondition;
import com.obs.services.model.SetBucketWebsiteRequest;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.VersionOrDeleteMarker;
import com.obs.services.model.WebsiteConfiguration;
import com.obs.test.TestTools;
import com.obs.test.objects.BaseObjectTest;

public class XmlResponsesSaxParserTest extends BaseObjectTest {
    private static final Logger logger = LogManager.getLogger(XmlResponsesSaxParserTest.class);
    
    @Test
    public void test_BucketEncryptionHandler() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<ServerSideEncryptionConfiguration xmlns=\"http://obs.cn-north-4.myhuaweicloud.com/doc/2015-06-30/\">")
                .append("<Rule>")
                .append("<ApplyServerSideEncryptionByDefault>")
                .append("<SSEAlgorithm>kms</SSEAlgorithm>")
                .append("<KMSMasterKeyID>kmskeyid-value</KMSMasterKeyID>")
                .append("</ApplyServerSideEncryptionByDefault>")
                .append("</Rule>")
                .append("</ServerSideEncryptionConfiguration>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        BucketEncryption bucketEncryption = xmlResponsesSaxParser.parse(inputStream, BucketEncryptionHandler.class, false)
                .getEncryption();
        
        logger.info(bucketEncryption);
        
        assertEquals("kms", bucketEncryption.getSseAlgorithm().getCode());
        assertEquals("kmskeyid-value", bucketEncryption.getKmsKeyId());
    }
    
    @Test
    public void test_BucketStoragePolicyHandler() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<GetBucketStorageInfoResult xmlns=\"http://obs.cn-north-4.myhuaweicloud.com/doc/2015-06-30/\">")
                .append("<Size>10</Size>")
                .append("<ObjectNumber>20</ObjectNumber>")
                .append("</GetBucketStorageInfoResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        BucketStorageInfo bucketStorageInfo = xmlResponsesSaxParser.parse(inputStream, BucketStorageInfoHandler.class, false)
                .getStorageInfo();
        
        logger.info(bucketStorageInfo);
        
        assertEquals(10, bucketStorageInfo.getSize());
        assertEquals(20, bucketStorageInfo.getObjectNumber());
    }
    
    @Test
    public void test_BucketNotificationConfigurationHandler() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<NotificationConfiguration xmlns=\"http://obs.myhuaweicloud.com/doc/2015-06-30/\">")
                .append("<TopicConfiguration>")
                .append("<Topic>urn:smn:cn-east-3:4b29a3cb5bd64581bda5714566814bb7:tet522</Topic>")
                .append("<Id>ConfigurationId</Id>")
                .append("<Filter>")
                .append("<Object>")
                .append("<FilterRule>")
                .append("<Name>prefix</Name>")
                .append("<Value>object</Value>")
                .append("</FilterRule>")
                .append("<FilterRule>")
                .append("<Name>suffix</Name>")
                .append("<Value>txt</Value>")
                .append("</FilterRule>")
                .append("</Object>")
                .append("</Filter>")
                .append("<Event>ObjectCreated:Put</Event>")
                .append("</TopicConfiguration>")
                .append("</NotificationConfiguration>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        BucketNotificationConfiguration bucketNotificationConfiguration = xmlResponsesSaxParser.parse(inputStream, BucketNotificationConfigurationHandler.class, false)
                .getBucketNotificationConfiguration();
        
        logger.info(bucketNotificationConfiguration);
        
        assertEquals("ConfigurationId", bucketNotificationConfiguration.getTopicConfigurations().get(0).getId());
        assertEquals("urn:smn:cn-east-3:4b29a3cb5bd64581bda5714566814bb7:tet522", bucketNotificationConfiguration.getTopicConfigurations().get(0).getTopic());
        assertEquals("ObjectCreated:Put", bucketNotificationConfiguration.getTopicConfigurations().get(0).getEvents().get(0));
        assertEquals("prefix", bucketNotificationConfiguration.getTopicConfigurations().get(0).getFilter().getFilterRules().get(0).getName());
        assertEquals("object", bucketNotificationConfiguration.getTopicConfigurations().get(0).getFilter().getFilterRules().get(0).getValue());
        assertEquals("suffix", bucketNotificationConfiguration.getTopicConfigurations().get(0).getFilter().getFilterRules().get(1).getName());
        assertEquals("txt", bucketNotificationConfiguration.getTopicConfigurations().get(0).getFilter().getFilterRules().get(1).getValue());
    }
    
    @Test
    public void test_BucketReplicationConfigurationHandler() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<ReplicationConfiguration xmlns=\"http://obs.myhuaweicloud.com/doc/2006-03-01/\">")
                .append("<Rule>")
                .append("<ID>Rule-1</ID>")
                .append("<Status>Enabled</Status>")
                .append("<Prefix></Prefix>")
                .append("<Destination>")
                .append("<Bucket>dstbucket</Bucket>")
                .append("<StorageClass>STANDARD</StorageClass>")
                .append("</Destination>")
                .append("<HistoricalObjectReplication>Enabled</HistoricalObjectReplication>")
                .append("</Rule>")
                .append("<Agency>testAcy</Agency>")
                .append("</ReplicationConfiguration>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ReplicationConfiguration replicationConfiguration = xmlResponsesSaxParser.parse(inputStream, BucketReplicationConfigurationHandler.class, false)
                .getReplicationConfiguration();
        
        logger.info(replicationConfiguration);
        
        assertEquals("testAcy", replicationConfiguration.getAgency());
        assertEquals("Rule-1", replicationConfiguration.getRules().get(0).getId());
        assertEquals("Enabled", replicationConfiguration.getRules().get(0).getStatus().getCode());
        assertEquals("", replicationConfiguration.getRules().get(0).getPrefix());
        assertEquals("dstbucket", replicationConfiguration.getRules().get(0).getDestination().getBucket());
        assertEquals("STANDARD", replicationConfiguration.getRules().get(0).getDestination().getObjectStorageClass().getCode());
    }
    
    @Test
    public void test_BucketWebsiteConfigurationHandler_1() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<WebsiteConfiguration xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<IndexDocument><Suffix>test.html</Suffix></IndexDocument>"
                + "<ErrorDocument><Key>error_test.html</Key></ErrorDocument>"
                + "<RoutingRules>"
                + "<RoutingRule>"
                + "<Condition><HttpErrorCodeReturnedEquals>404</HttpErrorCodeReturnedEquals><KeyPrefixEquals>keyprefix-1</KeyPrefixEquals></Condition>"
                + "<Redirect>"
                + "<ReplaceKeyPrefixWith>replacekeyprefix-1</ReplaceKeyPrefixWith><HttpRedirectCode>305</HttpRedirectCode>"
                + "<HostName>www.example.com</HostName>"
                + "<Protocol>http</Protocol>"
                + "</Redirect>"
                + "</RoutingRule>"
                + "<RoutingRule>"
                + "<Condition><HttpErrorCodeReturnedEquals>405</HttpErrorCodeReturnedEquals><KeyPrefixEquals>keyprefix-2</KeyPrefixEquals></Condition>"
                + "<Redirect>"
                + "<ReplaceKeyPrefixWith>replacekeyprefix-2</ReplaceKeyPrefixWith><HttpRedirectCode>302</HttpRedirectCode>"
                + "<HostName>www.example.cn</HostName>"
                + "<Protocol>https</Protocol>"
                + "</Redirect>"
                + "</RoutingRule>"
                + "</RoutingRules>"
                + "</WebsiteConfiguration>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        WebsiteConfiguration websiteConfiguration = xmlResponsesSaxParser.parse(inputStream, BucketWebsiteConfigurationHandler.class, false)
                .getWebsiteConfig();
        
        logger.info(websiteConfiguration);
        
        assertEquals("test.html", websiteConfiguration.getSuffix());
        assertEquals("error_test.html", websiteConfiguration.getKey());
        assertNull(websiteConfiguration.getRedirectAllRequestsTo());
        
        assertEquals("404", websiteConfiguration.getRouteRules().get(0).getCondition().getHttpErrorCodeReturnedEquals());
        assertEquals("keyprefix-1", websiteConfiguration.getRouteRules().get(0).getCondition().getKeyPrefixEquals());
        assertEquals("www.example.com", websiteConfiguration.getRouteRules().get(0).getRedirect().getHostName());
        assertEquals("replacekeyprefix-1", websiteConfiguration.getRouteRules().get(0).getRedirect().getReplaceKeyPrefixWith());
        assertEquals("305", websiteConfiguration.getRouteRules().get(0).getRedirect().getHttpRedirectCode());
        assertEquals("http", websiteConfiguration.getRouteRules().get(0).getRedirect().getProtocol());
        
        assertEquals("405", websiteConfiguration.getRouteRules().get(1).getCondition().getHttpErrorCodeReturnedEquals());
        assertEquals("keyprefix-2", websiteConfiguration.getRouteRules().get(1).getCondition().getKeyPrefixEquals());
        assertEquals("www.example.cn", websiteConfiguration.getRouteRules().get(1).getRedirect().getHostName());
        assertEquals("replacekeyprefix-2", websiteConfiguration.getRouteRules().get(1).getRedirect().getReplaceKeyPrefixWith());
        assertEquals("302", websiteConfiguration.getRouteRules().get(1).getRedirect().getHttpRedirectCode());
        assertEquals("https", websiteConfiguration.getRouteRules().get(1).getRedirect().getProtocol());
    }
    
    @Test
    public void test_BucketWebsiteConfigurationHandler_2() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<WebsiteConfiguration xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<RedirectAllRequestsTo><HostName>www.example.com</HostName><Protocol>http</Protocol></RedirectAllRequestsTo>"
                + "</WebsiteConfiguration>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        WebsiteConfiguration websiteConfiguration = xmlResponsesSaxParser.parse(inputStream, BucketWebsiteConfigurationHandler.class, false)
                .getWebsiteConfig();
        
        logger.info(websiteConfiguration);
        
        assertEquals("http", websiteConfiguration.getRedirectAllRequestsTo().getRedirectProtocol().getCode());
        assertEquals("www.example.com", websiteConfiguration.getRedirectAllRequestsTo().getHostName());
        assertNull(websiteConfiguration.getSuffix());
        assertNull(websiteConfiguration.getKey());
        assertEquals(0, websiteConfiguration.getRouteRules().size());
    }
    
    @Test
    public void test_ListMultipartUploadsHandler_1() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListMultipartUploadsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<RedirectAllRequestsTo><HostName>www.example.com</HostName><Protocol>http</Protocol></RedirectAllRequestsTo>"
                + "<Bucket>testpayer</Bucket>"
                + "<KeyMarker></KeyMarker>"
                + "<UploadIdMarker></UploadIdMarker>"
                + "<Delimiter></Delimiter>"
                + "<Prefix></Prefix>"
                + "<MaxUploads>1000</MaxUploads>"
                + "<IsTruncated>false</IsTruncated>"
                + "<Upload>"
                + "<Key>object-1</Key>"
                + "<UploadId>000001724B4BA4B39051228929217877</UploadId>"
                + "<Initiator><ID>domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61</ID></Initiator>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "<Initiated>2020-05-25T10:05:48.083Z</Initiated>"
                + "</Upload>"
                + "<Upload>"
                + "<Key>object-2</Key>"
                + "<UploadId>000001724B4BA6E6904725F03B654010</UploadId>"
                + "<Initiator><ID>domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61</ID></Initiator>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "<Initiated>2020-05-25T10:05:48.646Z</Initiated>"
                + "</Upload>"
                + "<Upload>"
                + "<Key>object-3</Key>"
                + "<UploadId>000001724B4BAB5A90551D35696950F1</UploadId>"
                + "<Initiator><ID>domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61</ID></Initiator>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "<Initiated>2020-05-25T10:05:49.786Z</Initiated>"
                + "</Upload>"
                + "</ListMultipartUploadsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListMultipartUploadsHandler handler = xmlResponsesSaxParser.parse(inputStream, ListMultipartUploadsHandler.class, true);

        assertEquals("testpayer", handler.getBucketName());
        assertEquals("", handler.getKeyMarker());
        assertEquals("", handler.getUploadIdMarker());
        assertEquals(3, handler.getMultipartUploadList().size());
        assertEquals("000001724B4BA4B39051228929217877", handler.getMultipartUploadList().get(0).getUploadId());
        assertEquals("object-1", handler.getMultipartUploadList().get(0).getObjectKey());
        assertEquals("75b07a9802444d969df2f4326420db96", handler.getMultipartUploadList().get(0).getOwner().getId());
        assertEquals("domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61", handler.getMultipartUploadList().get(0).getInitiator().getId());
        
        assertEquals("000001724B4BA6E6904725F03B654010", handler.getMultipartUploadList().get(1).getUploadId());
        assertEquals("object-2", handler.getMultipartUploadList().get(1).getObjectKey());
        assertEquals("75b07a9802444d969df2f4326420db96", handler.getMultipartUploadList().get(1).getOwner().getId());
        assertEquals("domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61", handler.getMultipartUploadList().get(1).getInitiator().getId());
        
        assertEquals("000001724B4BAB5A90551D35696950F1", handler.getMultipartUploadList().get(2).getUploadId());
        assertEquals("object-3", handler.getMultipartUploadList().get(2).getObjectKey());
        assertEquals("75b07a9802444d969df2f4326420db96", handler.getMultipartUploadList().get(2).getOwner().getId());
        assertEquals("domainID/75b07a9802444d969df2f4326420db96:userID/e1a0cc5f033f4a9aaa3419b963e91c61", handler.getMultipartUploadList().get(2).getInitiator().getId());
    }
    
    @Test
    public void test_ListPartsHandler_1() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListPartsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Bucket>shenqing-backup-test</Bucket>"
                + "<Key>object-1</Key>"
                + "<UploadId>000001724C1622A08048311C9082E710</UploadId>"
                + "<Initiator><ID>xxxxxxxxxxxxxxxx</ID><DisplayName>xxxxxxxxxxx</DisplayName></Initiator>"
                + "<Owner><ID>xxxxxxxxxxxxxxxx</ID><DisplayName>xxxxxxxxxxxx</DisplayName></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "<PartNumberMarker>0</PartNumberMarker>"
                + "<NextPartNumberMarker>2</NextPartNumberMarker>"
                + "<MaxParts>1000</MaxParts>"
                + "<IsTruncated>false</IsTruncated>"
                + "<Part>"
                + "<PartNumber>1</PartNumber>"
                + "<LastModified>2020-05-25T13:46:58.722Z</LastModified>"
                + "<ETag>\"1111--097e307ac52ed9b4ad551801fc\"</ETag>"
                + "<Size>6291456</Size>"
                + "</Part>"
                + "<Part>"
                + "<PartNumber>2</PartNumber>"
                + "<LastModified>2020-05-25T13:46:58.722Z</LastModified>"
                + "<ETag>\"2222--d097e307ac52ed9b4ad551801fc\"</ETag>"
                + "<Size>6291456</Size>"
                + "</Part>"
                + "</ListPartsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListPartsHandler handler = xmlResponsesSaxParser.parse(inputStream,
                ListPartsHandler.class, true);

        assertEquals("shenqing-backup-test", handler.getBucketName());
        assertEquals("object-1", handler.getObjectKey());
        assertEquals("000001724C1622A08048311C9082E710", handler.getUploadId());
        assertEquals("xxxxxxxxxxxxxxxxxxx", handler.getInitiator().getId());
        assertEquals("xxxxxxxxxxxxxxxxx", handler.getInitiator().getDisplayName());
        assertEquals("xxxxxxxxxxxx", handler.getOwner().getId());
        assertEquals("xxxxxxxxxxx", handler.getOwner().getDisplayName());
        
        assertEquals(2, handler.getMultiPartList().size());
        
        assertEquals(new Integer(1), handler.getMultiPartList().get(0).getPartNumber());
        assertEquals("\"1111--097e307ac52ed9b4ad551801fc\"", handler.getMultiPartList().get(0).getEtag());
        
        assertEquals(new Integer(2), handler.getMultiPartList().get(1).getPartNumber());
        assertEquals("\"2222--d097e307ac52ed9b4ad551801fc\"", handler.getMultiPartList().get(1).getEtag());
    }
    
    @Test
    public void test_ListBucketsHandler() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListAllMyBucketsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Owner>"
                + "<ID>75b07a9802444d969df2f4326420db96</ID>"
                + "</Owner>"
                + "<Buckets>"
                + "<Bucket>"
                + "<Name>0hecmirrorsourcecnnorth4demodfv0</Name>"
                + "<CreationDate>2020-04-26T07:09:26.633Z</CreationDate>"
                + "<Location>cn-north-4</Location>"
                + "</Bucket>"
                + "<Bucket>"
                + "<Name>20190415</Name>"
                + "<CreationDate>2019-04-19T02:18:51.885Z</CreationDate>"
                + "<Location>cn-north-1</Location>"
                + "</Bucket>"
                + "<Bucket>"
                + "<Name>wl.posix</Name>"
                + "<CreationDate>2020-03-17T03:35:30.688Z</CreationDate>"
                + "<Location>cn-east-2</Location>"
                + "</Bucket>"
                + "</Buckets>"
                + "</ListAllMyBucketsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListBucketsHandler handler = xmlResponsesSaxParser.parse(inputStream, ListBucketsHandler.class, true);
        
        List<ObsBucket> buckets = handler.getBuckets();
        Owner owner = handler.getOwner();
        
        logger.info(owner);
        logger.info(Arrays.toString(buckets.toArray()));
        
        assertEquals("75b07a9802444d969df2f4326420db96", owner.getId());
        assertEquals(3, buckets.size());
        assertEquals("75b07a9802444d969df2f4326420db96", buckets.get(0).getOwner().getId());
        assertEquals("75b07a9802444d969df2f4326420db96", buckets.get(1).getOwner().getId());
        assertEquals("75b07a9802444d969df2f4326420db96", buckets.get(2).getOwner().getId());
        
        assertEquals("0hecmirrorsourcecnnorth4demodfv0", buckets.get(0).getBucketName());
        assertEquals("20190415", buckets.get(1).getBucketName());
        assertEquals("wl.posix", buckets.get(2).getBucketName());
        
        assertEquals("cn-north-4", buckets.get(0).getLocation());
        assertEquals("cn-north-1", buckets.get(1).getLocation());
        assertEquals("cn-east-2", buckets.get(2).getLocation());
        
        assertEquals("Sun Apr 26 15:09:26 CST 2020", buckets.get(0).getCreationDate().toString());
        assertEquals("Fri Apr 19 10:18:51 CST 2019", buckets.get(1).getCreationDate().toString());
        assertEquals("Tue Mar 17 11:35:30 CST 2020", buckets.get(2).getCreationDate().toString());
    }
    
    @Test
    public void test_ListObjectsHandler_1() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListBucketResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix>obssftp-dbbkt-log/2020</Prefix>"
                + "<Marker>obssftp-dbbkt-log/2020</Marker>"
                + "<NextMarker>obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS</NextMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<Delimiter>/</Delimiter>"
                + "<IsTruncated>true</IsTruncated>"
                + "<Contents>"
                + "<Key>obssftp-dbbkt-log/2020-02-25-02-58-23-LYXAHNPQ70EELSXE</Key>"
                + "<LastModified>2020-02-25T02:58:23.694Z</LastModified>"
                + "<ETag>\"84ef7d8a93ec03307404f9625754340f\"</ETag>"
                + "<Size>104712</Size>"
                + "<Owner><ID>00000000000000000000000000000000</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Contents>"
                + "<Contents>"
                + "<Key>obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS</Key>"
                + "<LastModified>2020-02-25T02:58:49.993Z</LastModified>"
                + "<ETag>\"1bee45a60f4aaef23305d217b0653c6a\"</ETag>"
                + "<Size>254</Size>"
                + "<Owner><ID>00000000000000000000000000000000</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Contents>"
                + "</ListBucketResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListObjectsHandler listObjectsHandler = xmlResponsesSaxParser.parse(inputStream, ListObjectsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getMarkerForNextListing());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getNextMarker());
        assertEquals("/", listObjectsHandler.getRequestDelimiter());
        assertTrue(listObjectsHandler.isListingTruncated());
        assertEquals("obssftp-dbbkt-log/2020", listObjectsHandler.getRequestMarker());
        assertEquals(2, listObjectsHandler.getRequestMaxKeys());
        assertEquals("obssftp-dbbkt-log/2020", listObjectsHandler.getRequestPrefix());
        assertEquals(0, listObjectsHandler.getCommonPrefixes().size());
        assertEquals(0, listObjectsHandler.getExtenedCommonPrefixes().size());
        assertEquals(2, listObjectsHandler.getObjects().size());
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getObjects().get(0).getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-23-LYXAHNPQ70EELSXE", listObjectsHandler.getObjects().get(0).getObjectKey());
        assertEquals("Tue Feb 25 10:58:23 CST 2020", listObjectsHandler.getObjects().get(0).getMetadata().getLastModified().toString());
        assertEquals("\"84ef7d8a93ec03307404f9625754340f\"", listObjectsHandler.getObjects().get(0).getMetadata().getEtag());
        assertEquals(Long.valueOf(104712), listObjectsHandler.getObjects().get(0).getMetadata().getContentLength());
        assertEquals("00000000000000000000000000000000", listObjectsHandler.getObjects().get(0).getOwner().getId());
        assertEquals("STANDARD", listObjectsHandler.getObjects().get(0).getMetadata().getStorageClass());
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getObjects().get(1).getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getObjects().get(1).getObjectKey());
        assertEquals("Tue Feb 25 10:58:49 CST 2020", listObjectsHandler.getObjects().get(1).getMetadata().getLastModified().toString());
        assertEquals("\"1bee45a60f4aaef23305d217b0653c6a\"", listObjectsHandler.getObjects().get(1).getMetadata().getEtag());
        assertEquals(Long.valueOf(254), listObjectsHandler.getObjects().get(1).getMetadata().getContentLength());
        assertEquals("00000000000000000000000000000000", listObjectsHandler.getObjects().get(1).getOwner().getId());
        assertEquals("STANDARD", listObjectsHandler.getObjects().get(1).getMetadata().getStorageClass());
    }
    
    @Test
    public void test_ListObjectsHandler_2() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListBucketResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix></Prefix>"
                + "<Marker></Marker>"
                + "<NextMarker>obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS</NextMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<IsTruncated>true</IsTruncated>"
                + "<Contents>"
                + "<Key>obssftp-dbbkt-log/2020-02-25-02-58-23-LYXAHNPQ70EELSXE</Key>"
                + "<LastModified>2020-02-25T02:58:23.694Z</LastModified>"
                + "<ETag>\"84ef7d8a93ec03307404f9625754340f\"</ETag>"
                + "<Size>104712</Size>"
                + "<Owner><ID>00000000000000000000000000000000</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Contents>"
                + "<Contents>"
                + "<Key>obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS</Key>"
                + "<LastModified>2020-02-25T02:58:49.993Z</LastModified>"
                + "<ETag>\"1bee45a60f4aaef23305d217b0653c6a\"</ETag>"
                + "<Size>254</Size>"
                + "<Owner><ID>00000000000000000000000000000000</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Contents>"
                + "</ListBucketResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListObjectsHandler listObjectsHandler = xmlResponsesSaxParser.parse(inputStream, ListObjectsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getMarkerForNextListing());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getNextMarker());
        assertNull(listObjectsHandler.getRequestDelimiter());
        assertTrue(listObjectsHandler.isListingTruncated());
        assertEquals("", listObjectsHandler.getRequestMarker());
        assertEquals(2, listObjectsHandler.getRequestMaxKeys());
        assertEquals("", listObjectsHandler.getRequestPrefix());
        assertEquals(0, listObjectsHandler.getCommonPrefixes().size());
        assertEquals(0, listObjectsHandler.getExtenedCommonPrefixes().size());
        assertEquals(2, listObjectsHandler.getObjects().size());
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getObjects().get(0).getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-23-LYXAHNPQ70EELSXE", listObjectsHandler.getObjects().get(0).getObjectKey());
        assertEquals("Tue Feb 25 10:58:23 CST 2020", listObjectsHandler.getObjects().get(0).getMetadata().getLastModified().toString());
        assertEquals("\"84ef7d8a93ec03307404f9625754340f\"", listObjectsHandler.getObjects().get(0).getMetadata().getEtag());
        assertEquals(Long.valueOf(104712), listObjectsHandler.getObjects().get(0).getMetadata().getContentLength());
        assertEquals("00000000000000000000000000000000", listObjectsHandler.getObjects().get(0).getOwner().getId());
        assertEquals("STANDARD", listObjectsHandler.getObjects().get(0).getMetadata().getStorageClass());
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getObjects().get(1).getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getObjects().get(1).getObjectKey());
        assertEquals("Tue Feb 25 10:58:49 CST 2020", listObjectsHandler.getObjects().get(1).getMetadata().getLastModified().toString());
        assertEquals("\"1bee45a60f4aaef23305d217b0653c6a\"", listObjectsHandler.getObjects().get(1).getMetadata().getEtag());
        assertEquals(Long.valueOf(254), listObjectsHandler.getObjects().get(1).getMetadata().getContentLength());
        assertEquals("00000000000000000000000000000000", listObjectsHandler.getObjects().get(1).getOwner().getId());
        assertEquals("STANDARD", listObjectsHandler.getObjects().get(1).getMetadata().getStorageClass());
    }
    
    @Test
    public void test_ListObjectsHandler_3() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListBucketResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix></Prefix>"
                + "<Marker></Marker>"
                + "<NextMarker>obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS</NextMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<IsTruncated>true</IsTruncated>"
                + "</ListBucketResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListObjectsHandler listObjectsHandler = xmlResponsesSaxParser.parse(inputStream, ListObjectsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listObjectsHandler.getBucketName());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getMarkerForNextListing());
        assertEquals("obssftp-dbbkt-log/2020-02-25-02-58-49-AMONRZDP0YLD0RBS", listObjectsHandler.getNextMarker());
        assertNull(listObjectsHandler.getRequestDelimiter());
        assertTrue(listObjectsHandler.isListingTruncated());
        assertEquals("", listObjectsHandler.getRequestMarker());
        assertEquals(2, listObjectsHandler.getRequestMaxKeys());
        assertEquals("", listObjectsHandler.getRequestPrefix());
        assertEquals(0, listObjectsHandler.getCommonPrefixes().size());
        assertEquals(0, listObjectsHandler.getExtenedCommonPrefixes().size());
        assertEquals(0, listObjectsHandler.getObjects().size());
    }
    
    @Test
    public void test_ListVersionsHandler_1() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListVersionsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix>11/</Prefix>"
                + "<KeyMarker>11</KeyMarker>"
                + "<VersionIdMarker></VersionIdMarker>"
                + "<NextKeyMarker>11/1.jpg</NextKeyMarker>"
                + "<NextVersionIdMarker>G0011171B9720C8FFFFF801B04AED369</NextVersionIdMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<Delimiter>/</Delimiter>"
                + "<IsTruncated>true</IsTruncated>"
                + "<Version>"
                + "<Key>11/</Key>"
                + "<VersionId>G0011171B9718417FFFF801A04B805CE</VersionId>"
                + "<IsLatest>true</IsLatest>"
                + "<LastModified>2020-04-27T02:22:36.567Z</LastModified>"
                + "<ETag>\"d41d8cd98f00b204e9800998ecf8427e\"</ETag>"
                + "<Size>0</Size>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Version>"
                
                + "<Version>"
                + "<Key>11/1.jpg</Key>"
                + "<VersionId>G0011171B9720C8FFFFF801B04AED369</VersionId>"
                + "<IsLatest>true</IsLatest>"
                + "<LastModified>2020-04-27T02:23:11.503Z</LastModified>"
                + "<ETag>\"7ea44103eacc6027668bda886c9dda47\"</ETag>"
                + "<Size>82740</Size>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Version>"
                + "</ListVersionsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListVersionsHandler listVersionsHandler = xmlResponsesSaxParser.parse(inputStream, ListVersionsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getBucketName());
        assertEquals("/", listVersionsHandler.getDelimiter());
        assertEquals("11", listVersionsHandler.getKeyMarker());
        assertEquals("11/1.jpg", listVersionsHandler.getNextKeyMarker());
        assertEquals("G0011171B9720C8FFFFF801B04AED369", listVersionsHandler.getNextVersionIdMarker());
        assertEquals(2, listVersionsHandler.getRequestMaxKeys());
        assertEquals("11/", listVersionsHandler.getRequestPrefix());
        assertTrue(listVersionsHandler.isListingTruncated());
        
        assertEquals(0, listVersionsHandler.getCommonPrefixes().size());
        assertEquals(2, listVersionsHandler.getItems().size());
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getItems().get(0).getBucketName());
        assertEquals("11/", listVersionsHandler.getItems().get(0).getKey());
        assertEquals("\"d41d8cd98f00b204e9800998ecf8427e\"", listVersionsHandler.getItems().get(0).getEtag());
        assertEquals("G0011171B9718417FFFF801A04B805CE", listVersionsHandler.getItems().get(0).getVersionId());
        assertTrue(listVersionsHandler.getItems().get(0).isLatest());
        assertEquals("Mon Apr 27 10:22:36 CST 2020", listVersionsHandler.getItems().get(0).getLastModified().toString());
        assertEquals(0L, listVersionsHandler.getItems().get(0).getSize());
        assertEquals("75b07a9802444d969df2f4326420db96", listVersionsHandler.getItems().get(0).getOwner().getId());
        assertEquals("STANDARD", listVersionsHandler.getItems().get(0).getStorageClass());
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getItems().get(1).getBucketName());
        assertEquals("11/1.jpg", listVersionsHandler.getItems().get(1).getKey());
        assertEquals("\"7ea44103eacc6027668bda886c9dda47\"", listVersionsHandler.getItems().get(1).getEtag());
        assertEquals("G0011171B9720C8FFFFF801B04AED369", listVersionsHandler.getItems().get(1).getVersionId());
        assertTrue(listVersionsHandler.getItems().get(1).isLatest());
        assertEquals("Mon Apr 27 10:23:11 CST 2020", listVersionsHandler.getItems().get(1).getLastModified().toString());
        assertEquals(82740L, listVersionsHandler.getItems().get(1).getSize());
        assertEquals("75b07a9802444d969df2f4326420db96", listVersionsHandler.getItems().get(1).getOwner().getId());
        assertEquals("STANDARD", listVersionsHandler.getItems().get(1).getStorageClass());
    }
    
    @Test
    public void test_ListVersionsHandler_2() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListVersionsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix></Prefix>"
                + "<KeyMarker></KeyMarker>"
                + "<VersionIdMarker></VersionIdMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<IsTruncated>true</IsTruncated>"
                + "<Version>"
                + "<Key>11/</Key>"
                + "<VersionId>G0011171B9718417FFFF801A04B805CE</VersionId>"
                + "<IsLatest>true</IsLatest>"
                + "<LastModified>2020-04-27T02:22:36.567Z</LastModified>"
                + "<ETag>\"d41d8cd98f00b204e9800998ecf8427e\"</ETag>"
                + "<Size>0</Size>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Version>"
                
                + "<Version>"
                + "<Key>11/1.jpg</Key>"
                + "<VersionId>G0011171B9720C8FFFFF801B04AED369</VersionId>"
                + "<IsLatest>true</IsLatest>"
                + "<LastModified>2020-04-27T02:23:11.503Z</LastModified>"
                + "<ETag>\"7ea44103eacc6027668bda886c9dda47\"</ETag>"
                + "<Size>82740</Size>"
                + "<Owner><ID>75b07a9802444d969df2f4326420db96</ID></Owner>"
                + "<StorageClass>STANDARD</StorageClass>"
                + "</Version>"
                + "</ListVersionsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListVersionsHandler listVersionsHandler = xmlResponsesSaxParser.parse(inputStream, ListVersionsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getBucketName());
        assertNull(listVersionsHandler.getDelimiter());
        assertEquals("", listVersionsHandler.getKeyMarker());
        assertNull(listVersionsHandler.getNextKeyMarker());
        assertNull(listVersionsHandler.getNextVersionIdMarker());
        assertEquals(2, listVersionsHandler.getRequestMaxKeys());
        assertEquals("", listVersionsHandler.getRequestPrefix());
        assertTrue(listVersionsHandler.isListingTruncated());
        
        assertEquals(0, listVersionsHandler.getCommonPrefixes().size());
        assertEquals(2, listVersionsHandler.getItems().size());
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getItems().get(0).getBucketName());
        assertEquals("11/", listVersionsHandler.getItems().get(0).getKey());
        assertEquals("\"d41d8cd98f00b204e9800998ecf8427e\"", listVersionsHandler.getItems().get(0).getEtag());
        assertEquals("G0011171B9718417FFFF801A04B805CE", listVersionsHandler.getItems().get(0).getVersionId());
        assertTrue(listVersionsHandler.getItems().get(0).isLatest());
        assertEquals("Mon Apr 27 10:22:36 CST 2020", listVersionsHandler.getItems().get(0).getLastModified().toString());
        assertEquals(0L, listVersionsHandler.getItems().get(0).getSize());
        assertEquals("75b07a9802444d969df2f4326420db96", listVersionsHandler.getItems().get(0).getOwner().getId());
        assertEquals("STANDARD", listVersionsHandler.getItems().get(0).getStorageClass());
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getItems().get(1).getBucketName());
        assertEquals("11/1.jpg", listVersionsHandler.getItems().get(1).getKey());
        assertEquals("\"7ea44103eacc6027668bda886c9dda47\"", listVersionsHandler.getItems().get(1).getEtag());
        assertEquals("G0011171B9720C8FFFFF801B04AED369", listVersionsHandler.getItems().get(1).getVersionId());
        assertTrue(listVersionsHandler.getItems().get(1).isLatest());
        assertEquals("Mon Apr 27 10:23:11 CST 2020", listVersionsHandler.getItems().get(1).getLastModified().toString());
        assertEquals(82740L, listVersionsHandler.getItems().get(1).getSize());
        assertEquals("75b07a9802444d969df2f4326420db96", listVersionsHandler.getItems().get(1).getOwner().getId());
        assertEquals("STANDARD", listVersionsHandler.getItems().get(1).getStorageClass());
    }
    
    @Test
    public void test_ListVersionsHandler_3() {
        XmlResponsesSaxParser xmlResponsesSaxParser = new XmlResponsesSaxParser();
        
        String xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<ListVersionsResult xmlns=\"http://obs.myhwclouds.com/doc/2015-06-30/\">"
                + "<Name>obssftp-dbbkt</Name>"
                + "<Prefix></Prefix>"
                + "<KeyMarker></KeyMarker>"
                + "<VersionIdMarker></VersionIdMarker>"
                + "<MaxKeys>2</MaxKeys>"
                + "<IsTruncated>true</IsTruncated>"
                + "</ListVersionsResult>")
                .toString();
        
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        
        ListVersionsHandler listVersionsHandler = xmlResponsesSaxParser.parse(inputStream, ListVersionsHandler.class, true);
        
        assertEquals("obssftp-dbbkt", listVersionsHandler.getBucketName());
        assertNull(listVersionsHandler.getDelimiter());
        assertEquals("", listVersionsHandler.getKeyMarker());
        assertNull(listVersionsHandler.getNextKeyMarker());
        assertNull(listVersionsHandler.getNextVersionIdMarker());
        assertEquals(2, listVersionsHandler.getRequestMaxKeys());
        assertEquals("", listVersionsHandler.getRequestPrefix());
        assertTrue(listVersionsHandler.isListingTruncated());
        
        assertEquals(0, listVersionsHandler.getCommonPrefixes().size());
        assertEquals(0, listVersionsHandler.getItems().size());
    }
    
    @Test
    @Ignore
    public void test_set_bucket_website_1() {
        WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
        websiteConfig.setSuffix("test.html");
        websiteConfig.setKey("error_test.html");
        
        RouteRule rule1 = new RouteRule();
        Redirect r1 = new Redirect();
        r1.setHostName("www.example.com");
        r1.setHttpRedirectCode("305");
        r1.setRedirectProtocol(ProtocolEnum.HTTP);
        r1.setReplaceKeyPrefixWith("replacekeyprefix-1");
        rule1.setRedirect(r1);
        RouteRuleCondition condition1 = new RouteRuleCondition();
        condition1.setHttpErrorCodeReturnedEquals("404");
        condition1.setKeyPrefixEquals("keyprefix-1");
        rule1.setCondition(condition1);
        
        RouteRule rule2 = new RouteRule();
        Redirect r2 = new Redirect();
        r2.setHostName("www.example.cn");
        r2.setHttpRedirectCode("302");
        r2.setRedirectProtocol(ProtocolEnum.HTTPS);
        r2.setReplaceKeyPrefixWith("replacekeyprefix-2");
        rule2.setRedirect(r2);
        RouteRuleCondition condition2 = new RouteRuleCondition();
        condition2.setHttpErrorCodeReturnedEquals("405");
        condition2.setKeyPrefixEquals("keyprefix-2");
        rule2.setCondition(condition2);
        
        websiteConfig.getRouteRules().add(rule1);
        websiteConfig.getRouteRules().add(rule2);
        
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest("shenqing-test-bucket", websiteConfig);
        
        TestTools.getExternalEnvironment().setBucketWebsite(request);
    }
    
    @Test
    @Ignore
    public void test_set_bucket_website_2() {
        WebsiteConfiguration websiteConfig = new WebsiteConfiguration();
        RedirectAllRequest redirectAll = new RedirectAllRequest();
        redirectAll.setHostName("www.example.com");
        redirectAll.setRedirectProtocol(ProtocolEnum.HTTP);
        websiteConfig.setRedirectAllRequestsTo(redirectAll);
        
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest("shenqing-test-bucket", websiteConfig);
        
        TestTools.getExternalEnvironment().setBucketWebsite(request);
    }
    
    @Test
    @Ignore
    public void test_list_bucket() {
        ListBucketsRequest request = new ListBucketsRequest();
        request.setQueryLocation(true);
        request.setBucketType(BucketTypeEnum.PFS);
        List<ObsBucket> buckets = TestTools.getExternalEnvironment().listBuckets(request);
        for(ObsBucket bucket : buckets){
            System.out.println("BucketName:" + bucket.getBucketName() + "  CreationDate:" + bucket.getCreationDate() + "  Location:" + bucket.getLocation());
        }
    }
    
    @Test
    @Ignore
    public void test_list_objects() {
        ListObjectsRequest request = new ListObjectsRequest("0hecmirrortargetcnnorth4demodfv0");
        request.setMaxKeys(2);
        request.setMarker("obssftp-dbbkt-log/2020");
        request.setPrefix("obssftp-dbbkt-log/2020");
        request.setDelimiter("/");
        ObjectListing result = TestTools.getExternalEnvironment().listObjects(request);
        for(ObsObject obsObject : result.getObjects()){
            System.out.println(obsObject);
        }
    }
    
    @Test
    @Ignore
    public void test_list_versions() {
        ListVersionsRequest request = new ListVersionsRequest("shenqing-test-bucket");
        request.setMaxKeys(2);
        request.setKeyMarker("11");
        request.setPrefix("11/");
        request.setVersionIdMarker("");
        request.setDelimiter("/");
        ListVersionsResult result = TestTools.getExternalEnvironment().listVersions(request);
        for(VersionOrDeleteMarker obsObject : result.getVersions()){
            System.out.println(obsObject);
        }
    }
    
    @Test
    @Ignore
    public void test_list_multipartuploads() {
//        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest("testpayer", "object-1");
//        TestTools.getExternalEnvironment().initiateMultipartUpload(initRequest);
//        initRequest = new InitiateMultipartUploadRequest("testpayer", "object-2");
//        TestTools.getExternalEnvironment().initiateMultipartUpload(initRequest);
//        initRequest = new InitiateMultipartUploadRequest("testpayer", "object-3");
//        TestTools.getExternalEnvironment().initiateMultipartUpload(initRequest);
//        initRequest = new InitiateMultipartUploadRequest("testpayer", "object-4");
//        TestTools.getExternalEnvironment().initiateMultipartUpload(initRequest);
        
        ListMultipartUploadsRequest request = new ListMultipartUploadsRequest("testpayer");
        MultipartUploadListing result = TestTools.getExternalEnvironment().listMultipartUploads(request);
        for(MultipartUpload multipartUpload : result.getMultipartTaskList()){
            System.out.println(multipartUpload);
        }
    }
    
    @Test
    @Ignore
    public void test_list_parts() {
        
        String bucketName = "shenqing-backup-test";
        String objectKey = "object-1";
        /**
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult initResult = TestTools.getInnerEnvironment().initiateMultipartUpload(initRequest);
      
        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(initResult.getUploadId());
        request.setPartNumber(1);
        request.setInput(new ByteArrayInputStream(getByte(6 * 1024 * 1024)));
        TestTools.getInnerEnvironment().uploadPart(request);
        
        request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setUploadId(initResult.getUploadId());
        request.setPartNumber(2);
        request.setInput(new ByteArrayInputStream(getByte(6 * 1024 * 1024)));
        TestTools.getInnerEnvironment().uploadPart(request);
        */
        ListPartsRequest listPartRequest = new ListPartsRequest(bucketName, objectKey, "000001724C1622A08048311C9082E710");
        ListPartsResult result = TestTools.getPipelineEnvironment().listParts(listPartRequest);
        for(Multipart part : result.getMultipartList()){
            System.out.println(part.getPartNumber());
        }
    }
}
