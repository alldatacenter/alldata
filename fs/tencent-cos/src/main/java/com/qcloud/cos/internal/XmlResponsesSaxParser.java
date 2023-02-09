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


package com.qcloud.cos.internal;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException.DeleteError;
import com.qcloud.cos.model.AbortIncompleteMultipartUpload;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.BucketDomainConfiguration;
import com.qcloud.cos.model.BucketIntelligentTierConfiguration;
import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.qcloud.cos.model.BucketLifecycleConfiguration.NoncurrentVersionTransition;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Rule;
import com.qcloud.cos.model.BucketLifecycleConfiguration.Transition;
import com.qcloud.cos.model.BucketLoggingConfiguration;
import com.qcloud.cos.model.BucketRefererConfiguration;
import com.qcloud.cos.model.BucketReplicationConfiguration;
import com.qcloud.cos.model.BucketTaggingConfiguration;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.BucketWebsiteConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.qcloud.cos.model.CORSRule.AllowedMethods;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.COSVersionSummary;
import com.qcloud.cos.model.CompleteMultipartUploadResult;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.DeleteObjectsResult.DeletedObject;
import com.qcloud.cos.model.DomainRule;
import com.qcloud.cos.model.GetBucketInventoryConfigurationResult;
import com.qcloud.cos.model.GetObjectTaggingResult;
import com.qcloud.cos.model.Grantee;
import com.qcloud.cos.model.GroupGrantee;
import com.qcloud.cos.model.InitiateMultipartUploadResult;
import com.qcloud.cos.model.ListBucketInventoryConfigurationsResult;
import com.qcloud.cos.model.MultipartUpload;
import com.qcloud.cos.model.MultipartUploadListing;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.Owner;
import com.qcloud.cos.model.PartListing;
import com.qcloud.cos.model.PartSummary;
import com.qcloud.cos.model.Permission;
import com.qcloud.cos.model.RedirectRule;
import com.qcloud.cos.model.ReplicationDestinationConfig;
import com.qcloud.cos.model.ReplicationRule;
import com.qcloud.cos.model.RoutingRule;
import com.qcloud.cos.model.RoutingRuleCondition;
import com.qcloud.cos.model.Tag.LifecycleTagPredicate;
import com.qcloud.cos.model.Tag.Tag;
import com.qcloud.cos.model.TagSet;
import com.qcloud.cos.model.UinGrantee;
import com.qcloud.cos.model.VersionListing;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.AuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.AudtingCommonInfo;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.BatchImageJobDetail;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.DocumentResultInfo;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.ObjectResults;
import com.qcloud.cos.model.ciModel.auditing.OcrResults;
import com.qcloud.cos.model.ciModel.auditing.ResultsImageAuditingDetail;
import com.qcloud.cos.model.ciModel.auditing.ResultsTextAuditingDetail;
import com.qcloud.cos.model.ciModel.auditing.SectionInfo;
import com.qcloud.cos.model.ciModel.auditing.SnapshotInfo;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingJobsDetail;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingResponse;
import com.qcloud.cos.model.ciModel.bucket.DocBucketObject;
import com.qcloud.cos.model.ciModel.bucket.DocBucketResponse;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketObject;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketResponse;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.ImageLabelV2Response;
import com.qcloud.cos.model.ciModel.image.Lobel;
import com.qcloud.cos.model.ciModel.image.LobelV2;
import com.qcloud.cos.model.ciModel.image.LocationLabel;
import com.qcloud.cos.model.ciModel.job.DocJobDetail;
import com.qcloud.cos.model.ciModel.job.DocJobListResponse;
import com.qcloud.cos.model.ciModel.job.DocJobResponse;
import com.qcloud.cos.model.ciModel.job.DocProcessObject;
import com.qcloud.cos.model.ciModel.job.DocProcessPageInfo;
import com.qcloud.cos.model.ciModel.job.DocProcessResult;
import com.qcloud.cos.model.ciModel.job.MediaAudioObject;
import com.qcloud.cos.model.ciModel.job.MediaConcatFragmentObject;
import com.qcloud.cos.model.ciModel.job.MediaConcatTemplateObject;
import com.qcloud.cos.model.ciModel.job.MediaContainerObject;
import com.qcloud.cos.model.ciModel.job.MediaJobObject;
import com.qcloud.cos.model.ciModel.job.MediaJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaListJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaRemoveWaterMark;
import com.qcloud.cos.model.ciModel.job.MediaTimeIntervalObject;
import com.qcloud.cos.model.ciModel.job.MediaTransConfigObject;
import com.qcloud.cos.model.ciModel.job.MediaTranscodeVideoObject;
import com.qcloud.cos.model.ciModel.job.MediaVideoObject;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaFormat;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoAudio;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoResponse;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoSubtitle;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoVideo;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaStream;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.qcloud.cos.model.ciModel.queue.DocListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaNotifyConfig;
import com.qcloud.cos.model.ciModel.queue.MediaQueueObject;
import com.qcloud.cos.model.ciModel.queue.MediaQueueResponse;
import com.qcloud.cos.model.ciModel.recognition.CodeLocation;
import com.qcloud.cos.model.ciModel.recognition.QRcodeInfo;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotResponse;
import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaSnapshotObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateObject;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateTransTplObject;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkImage;
import com.qcloud.cos.model.ciModel.template.MediaWaterMarkText;
import com.qcloud.cos.model.ciModel.template.MediaWatermark;
import com.qcloud.cos.model.ciModel.workflow.MediaTasks;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowDependency;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionObject;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionsResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowInput;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowNode;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowObject;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowResponse;
import com.qcloud.cos.model.inventory.InventoryConfiguration;
import com.qcloud.cos.model.inventory.InventoryCosBucketDestination;
import com.qcloud.cos.model.inventory.InventoryDestination;
import com.qcloud.cos.model.inventory.InventoryFilter;
import com.qcloud.cos.model.inventory.InventoryPrefixPredicate;
import com.qcloud.cos.model.inventory.InventorySchedule;
import com.qcloud.cos.model.inventory.ServerSideEncryptionCOS;
import com.qcloud.cos.model.lifecycle.LifecycleAndOperator;
import com.qcloud.cos.model.lifecycle.LifecycleFilter;
import com.qcloud.cos.model.lifecycle.LifecycleFilterPredicate;
import com.qcloud.cos.model.lifecycle.LifecyclePrefixPredicate;
import com.qcloud.cos.utils.DateUtils;
import com.qcloud.cos.utils.StringUtils;
import com.qcloud.cos.utils.UrlEncoderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * XML Sax parser to read XML documents returned by COS via the REST interface, converting these
 * documents into objects.
 */
public class XmlResponsesSaxParser {
    private static final Logger log = LoggerFactory.getLogger(XmlResponsesSaxParser.class);

    private XMLReader xr = null;

    private boolean sanitizeXmlDocument = true;

    /**
     * Constructs the XML SAX parser.
     *
     * @throws CosClientException
     */
    public XmlResponsesSaxParser() throws CosClientException {
        // Ensure we can load the XML Reader.
        try {
            xr = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            throw new CosClientException("Couldn't initialize a SAX driver to create an XMLReader",
                    e);
        }
    }

    /**
     * Parses an XML document from an input stream using a document handler.
     *
     * @param handler the handler for the XML document
     * @param inputStream an input stream containing the XML document to parse
     *
     * @throws IOException on error reading from the input stream (ie connection reset)
     * @throws CosClientException on error with malformed XML, etc
     */
    protected void parseXmlInputStream(DefaultHandler handler, InputStream inputStream)
            throws IOException {
        try {

            if (log.isDebugEnabled()) {
                log.debug("Parsing XML response document with handler: " + handler.getClass());
            }

            BufferedReader breader =
                    new BufferedReader(new InputStreamReader(inputStream, StringUtils.UTF8));
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(new InputSource(breader));

        } catch (IOException e) {
            throw e;

        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (IOException e) {
                if (log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after XML parse failure", e);
                }
            }
            throw new CosClientException(
                    "Failed to parse XML document with handler " + handler.getClass(), t);
        }
    }

    protected InputStream sanitizeXmlDocument(DefaultHandler handler, InputStream inputStream)
            throws IOException {

        if (!sanitizeXmlDocument) {
            // No sanitizing will be performed, return the original input stream unchanged.
            return inputStream;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sanitizing XML document destined for handler " + handler.getClass());
            }

            InputStream sanitizedInputStream = null;

            try {

                /*
                 * Read object listing XML document from input stream provided into a string buffer,
                 * so we can replace troublesome characters before sending the document to the XML
                 * parser.
                 */
                StringBuilder listingDocBuffer = new StringBuilder();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(inputStream, StringUtils.UTF8));

                char[] buf = new char[8192];
                int read = -1;
                while ((read = br.read(buf)) != -1) {
                    listingDocBuffer.append(buf, 0, read);
                }
                br.close();

                /*
                 * Replace any carriage return (\r) characters with explicit XML character entities,
                 * to prevent the SAX parser from misinterpreting 0x0D characters as 0x0A and being
                 * unable to parse the XML.
                 */
                String listingDoc = listingDocBuffer.toString().replaceAll("\r", "&#013;");

                sanitizedInputStream =
                        new ByteArrayInputStream(listingDoc.getBytes(StringUtils.UTF8));

            } catch (IOException e) {
                throw e;

            } catch (Throwable t) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    if (log.isErrorEnabled()) {
                        log.error(
                                "Unable to close response InputStream after failure sanitizing XML document",
                                e);
                    }
                }
                throw new CosClientException("Failed to sanitize XML document destined for handler "
                        + handler.getClass(), t);
            }
            return sanitizedInputStream;
        }
    }

    /**
     * Safely parses the specified string as an integer and returns the value. If a
     * NumberFormatException occurs while parsing the integer, an error is logged and -1 is
     * returned.
     *
     * @param s The string to parse and return as an integer.
     *
     * @return The integer value of the specified string, otherwise -1 if there were any problems
     *         parsing the string as an integer.
     */
    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            log.error("Unable to parse integer value '" + s + "'", nfe);
        }

        return -1;
    }

    /**
     * Safely parses the specified string as a long and returns the value. If a
     * NumberFormatException occurs while parsing the long, an error is logged and -1 is returned.
     *
     * @param s The string to parse and return as a long.
     *
     * @return The long value of the specified string, otherwise -1 if there were any problems
     *         parsing the string as a long.
     */
    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            log.error("Unable to parse long value '" + s + "'", nfe);
        }

        return -1;
    }

    /**
     * Checks if the specified string is empty or null and if so, returns null. Otherwise simply
     * returns the string.
     *
     * @param s The string to check.
     * @return Null if the specified string was null, or empty, otherwise returns the string the
     *         caller passed in.
     */
    private static String checkForEmptyString(String s) {
        if (s == null)
            return null;
        if (s.length() == 0)
            return null;

        return s;
    }

    /**
     * Perform a url decode on the given value if specified. Return value by default;
     */
    private static String decodeIfSpecified(String value, boolean decode) {
        return decode ? UrlEncoderUtils.urlDecode(value) : value;
    }

    /**
     * Parses a ListBucket response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @return the XML handler object populated with data parsed from the XML stream.
     * @throws CosClientException
     */
    public ListBucketHandler parseListBucketObjectsResponse(InputStream inputStream,
            final boolean shouldSDKDecodeResponse) throws IOException {
        ListBucketHandler handler = new ListBucketHandler(shouldSDKDecodeResponse);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }


    /**
     * Parses a ListVersions response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @param shouldSDKDecodeResponse
     * @return the XML handler object populated with data parsed from the XML stream.
     * @throws CosClientException
     */
    public ListVersionsHandler parseListVersionsResponse(InputStream inputStream,
            boolean shouldSDKDecodeResponse) throws IOException {
        ListVersionsHandler handler = new ListVersionsHandler(shouldSDKDecodeResponse);
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    /**
     * Parses a ListAllMyBuckets response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @return the XML handler object populated with data parsed from the XML stream.
     * @throws CosClientException
     */
    public ListAllMyBucketsHandler parseListMyBucketsResponse(InputStream inputStream)
            throws IOException {
        ListAllMyBucketsHandler handler = new ListAllMyBucketsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    //
    /**
     * Parses an AccessControlListHandler response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @return the XML handler object populated with data parsed from the XML stream.
     *
     * @throws CosClientException
     */
    public AccessControlListHandler parseAccessControlListResponse(InputStream inputStream)
            throws IOException {
        AccessControlListHandler handler = new AccessControlListHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    /**
     * Parses an AccessControlListHandler response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @return the XML handler object populated with data parsed from the XML stream.
     *
     * @throws CosClientException
     */
    public ImagePersistenceHandler parseImagePersistenceResponse(InputStream inputStream)
            throws IOException {
        ImagePersistenceHandler handler = new ImagePersistenceHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    //
    // /**
    // * Parses a LoggingStatus response XML document for a bucket from an input stream.
    // *
    // * @param inputStream XML data input stream.
    // * @return the XML handler object populated with data parsed from the XML stream.
    // *
    // * @throws CosClientException
    // */
    // public BucketLoggingConfigurationHandler parseLoggingStatusResponse(InputStream inputStream)
    // throws IOException {
    // BucketLoggingConfigurationHandler handler = new BucketLoggingConfigurationHandler();
    // parseXmlInputStream(handler, inputStream);
    // return handler;
    // }
    //
    public BucketLifecycleConfigurationHandler parseBucketLifecycleConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketLifecycleConfigurationHandler handler = new BucketLifecycleConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }


    public BucketCrossOriginConfigurationHandler parseBucketCrossOriginConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketCrossOriginConfigurationHandler handler = new BucketCrossOriginConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public BucketDomainConfigurationHandler parseBucketDomainConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketDomainConfigurationHandler handler = new BucketDomainConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public BucketRefererConfigurationHandler parseBucketRefererConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketRefererConfigurationHandler handler = new BucketRefererConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public String parseBucketLocationResponse(InputStream inputStream) throws IOException {
        BucketLocationHandler handler = new BucketLocationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler.getLocation();
    }

    public BucketVersioningConfigurationHandler parseVersioningConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketVersioningConfigurationHandler handler = new BucketVersioningConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    //
    // public BucketWebsiteConfigurationHandler parseWebsiteConfigurationResponse(
    // InputStream inputStream) throws IOException {
    // BucketWebsiteConfigurationHandler handler = new BucketWebsiteConfigurationHandler();
    // parseXmlInputStream(handler, inputStream);
    // return handler;
    // }
    //
    //
    public BucketReplicationConfigurationHandler parseReplicationConfigurationResponse(
            InputStream inputStream) throws IOException {
        BucketReplicationConfigurationHandler handler = new BucketReplicationConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public BucketTaggingConfigurationHandler parseTaggingConfigurationResponse(InputStream inputStream)
            throws IOException {
        BucketTaggingConfigurationHandler handler = new BucketTaggingConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    //
    // public BucketTaggingConfigurationHandler parseTaggingConfigurationResponse(
    // InputStream inputStream) throws IOException {
    // BucketTaggingConfigurationHandler handler = new BucketTaggingConfigurationHandler();
    // parseXmlInputStream(handler, inputStream);
    // return handler;
    // }
    //
    public DeleteObjectsHandler parseDeletedObjectsResult(InputStream inputStream)
            throws IOException {
        DeleteObjectsHandler handler = new DeleteObjectsHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public CopyObjectResultHandler parseCopyObjectResponse(InputStream inputStream)
            throws IOException {
        CopyObjectResultHandler handler = new CopyObjectResultHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public CompleteMultipartUploadHandler parseCompleteMultipartUploadResponse(
            InputStream inputStream) throws IOException {
        CompleteMultipartUploadHandler handler = new CompleteMultipartUploadHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public InitiateMultipartUploadHandler parseInitiateMultipartUploadResponse(
            InputStream inputStream) throws IOException {
        InitiateMultipartUploadHandler handler = new InitiateMultipartUploadHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public ListMultipartUploadsHandler parseListMultipartUploadsResponse(InputStream inputStream)
            throws IOException {
        ListMultipartUploadsHandler handler = new ListMultipartUploadsHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public ListPartsHandler parseListPartsResponse(InputStream inputStream) throws IOException {
        ListPartsHandler handler = new ListPartsHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public BucketWebsiteConfigurationHandler parseWebsiteConfigurationResponse(InputStream inputStream)
            throws IOException {
        BucketWebsiteConfigurationHandler handler = new BucketWebsiteConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    /**
     * Parses a LoggingStatus response XML document for a bucket from an input
     * stream.
     *
     * @param inputStream
     *            XML data input stream.
     * @return the XML handler object populated with data parsed from the XML
     *         stream.
     *
     * @throws CosClientException
     */

    public BucketLoggingConfigurationHandler parseLoggingStatusResponse(InputStream inputStream)
            throws IOException {
        BucketLoggingConfigurationHandler handler = new BucketLoggingConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public GetBucketInventoryConfigurationHandler parseGetBucketInventoryConfigurationResponse(InputStream inputStream)
            throws IOException {
        GetBucketInventoryConfigurationHandler handler = new GetBucketInventoryConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public ListBucketInventoryConfigurationsHandler parseBucketListInventoryConfigurationsResponse(InputStream inputStream)
            throws IOException {
        ListBucketInventoryConfigurationsHandler handler = new ListBucketInventoryConfigurationsHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public GetObjectTaggingHandler parseObjectTaggingResponse(InputStream inputStream) throws IOException {
        GetObjectTaggingHandler handler = new GetObjectTaggingHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public GetBucketIntelligentTierConfigurationHandler parseBucketIntelligentTierConfigurationsResponse(InputStream inputStream)
            throws IOException {
        GetBucketIntelligentTierConfigurationHandler handler = new GetBucketIntelligentTierConfigurationHandler();
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public ListQueueHandler parseListQueueResponse(InputStream inputStream) throws IOException {
        ListQueueHandler handler = new ListQueueHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DocListQueueHandler parseDocListQueueResponse(InputStream inputStream) throws IOException {
        DocListQueueHandler handler = new DocListQueueHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public MediaQueueResponseHandler parseUpdateMediaQueueResponse(InputStream inputStream) throws IOException {
        MediaQueueResponseHandler handler = new MediaQueueResponseHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public MediaTemplateHandler parseMediaTemplateResponse(InputStream inputStream) throws IOException {
        MediaTemplateHandler handler = new MediaTemplateHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public SnapshotHandler parseSnapshotResponse(InputStream inputStream) throws IOException {
        SnapshotHandler handler = new SnapshotHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public GenerateMediainfoHandler parseGenerateMediainfoResponse(InputStream inputStream) throws IOException {
        GenerateMediainfoHandler handler = new GenerateMediainfoHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WorkflowExecutionsHandler parseMediaWorkflowExecutionsResponse(InputStream inputStream) throws IOException {
        WorkflowExecutionsHandler handler = new WorkflowExecutionsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public MediaTemplatesHandler parseDescribeMediaTemplatesResponse(InputStream inputStream) throws IOException {
        MediaTemplatesHandler handler = new MediaTemplatesHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ListMediaBucketHandler parseListBucketResponse(InputStream inputStream) throws IOException {
        ListMediaBucketHandler handler = new ListMediaBucketHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ListDocBucketHandler parseDocListBucketResponse(InputStream inputStream) throws IOException {
        ListDocBucketHandler handler = new ListDocBucketHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WorkflowListHandler parseWorkflowListResponse(InputStream inputStream)
            throws IOException {
        WorkflowListHandler handler = new WorkflowListHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WorkflowHandler parseWorkflowResponse(InputStream inputStream)
            throws IOException {
        WorkflowHandler handler = new WorkflowHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }


    public WorkflowExecutionHandler parseWorkflowExecutionResponse(InputStream inputStream)
            throws IOException {
        WorkflowExecutionHandler handler = new WorkflowExecutionHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public MediaJobCreatHandler parseJobCreatResponse(InputStream inputStream) throws IOException {
        MediaJobCreatHandler handler = new MediaJobCreatHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeMediaJobHandler parseMediaJobRespones(InputStream inputStream) throws IOException {
        DescribeMediaJobHandler handler = new DescribeMediaJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeMediaJobsHandler parseMediaJobsRespones(InputStream inputStream) throws IOException {
        DescribeMediaJobsHandler handler = new DescribeMediaJobsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DocJobHandler parseDocJobResponse(InputStream inputStream) throws IOException {
        DocJobHandler handler = new DocJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeDocProcessJobHandler parseDescribeDocJobResponse(InputStream inputStream) throws IOException {
        DescribeDocProcessJobHandler handler = new DescribeDocProcessJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeDocProcessJobsHandler parseDocJobListResponse(InputStream inputStream) throws IOException {
        DescribeDocProcessJobsHandler handler = new DescribeDocProcessJobsHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ImageAuditingHandler parseImageAuditingResponse(InputStream inputStream) throws IOException {
        ImageAuditingHandler handler = new ImageAuditingHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public CreateVideoAuditingJobHandler parseVideoAuditingJobResponse(InputStream inputStream) throws IOException {
        CreateVideoAuditingJobHandler handler = new CreateVideoAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public CreateAudioAuditingJobHandler parseAudioAuditingJobResponse(InputStream inputStream) throws IOException {
        CreateAudioAuditingJobHandler handler = new CreateAudioAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeVideoAuditingJobHandler parseDescribeVideoAuditingJobResponse(InputStream inputStream) throws IOException {
        DescribeVideoAuditingJobHandler handler = new DescribeVideoAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DescribeAudioAuditingJobHandler parseDescribeAudioAuditingJobResponse(InputStream inputStream) throws IOException {
        DescribeAudioAuditingJobHandler handler = new DescribeAudioAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ImageLabelHandler parseImageLabelResponse(InputStream inputStream) throws IOException {
        ImageLabelHandler handler = new ImageLabelHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public ImageLabelV2Handler parseImageLabelV2Response(InputStream inputStream) throws IOException {
        ImageLabelV2Handler handler = new ImageLabelV2Handler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public TextAuditingJobHandler parseTextAuditingResponse(InputStream inputStream) throws IOException {
        TextAuditingJobHandler handler = new TextAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public TextAuditingDescribeJobHandler parseTextAuditingDescribeResponse(InputStream inputStream) throws IOException {
        TextAuditingDescribeJobHandler handler = new TextAuditingDescribeJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DocumentAuditingJobHandler parseDocumentAuditingResponse(InputStream inputStream) throws IOException {
        DocumentAuditingJobHandler handler = new DocumentAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public DocumentAuditingDescribeJobHandler parseDocumentAuditingDescribeResponse(InputStream inputStream) throws IOException {
        DocumentAuditingDescribeJobHandler handler = new DocumentAuditingDescribeJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public BatchImageAuditingHandler parseBatchImageAuditingResponse(InputStream inputStream) throws IOException {
        BatchImageAuditingHandler handler = new BatchImageAuditingHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WebpageAuditingJobHandler parseWebpageAuditingJobResponse(InputStream inputStream) throws IOException {
        WebpageAuditingJobHandler handler = new WebpageAuditingJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    public WebpageAuditingDescribeJobHandler parseDWebpageAuditingDescribeResponse(InputStream inputStream) throws IOException {
        WebpageAuditingDescribeJobHandler handler = new WebpageAuditingDescribeJobHandler();
        parseXmlInputStream(handler, sanitizeXmlDocument(handler, inputStream));
        return handler;
    }

    /**
     * @param inputStream
     *
     * @return true if the bucket's is configured as Requester Pays, false if it is configured as
     *         Owner pays.
     *
     * @throws CosClientException
     */
    // public RequestPaymentConfigurationHandler parseRequestPaymentConfigurationResponse(
    // InputStream inputStream) throws IOException {
    // RequestPaymentConfigurationHandler handler = new RequestPaymentConfigurationHandler();
    // parseXmlInputStream(handler, inputStream);
    // return handler;
    // }

    // ////////////
    // Handlers //
    // ////////////

    /**
     * Handler for ListBucket response XML documents. //
     */
    public static class ListBucketHandler extends AbstractHandler {

        private final ObjectListing objectListing = new ObjectListing();
        private final boolean shouldSDKDecodeResponse;

        private COSObjectSummary currentObject = null;
        private Owner currentOwner = null;
        private String lastKey = null;

        public ListBucketHandler(final boolean shouldSDKDecodeResponse) {
            this.shouldSDKDecodeResponse = shouldSDKDecodeResponse;
        }

        public ObjectListing getObjectListing() {
            return objectListing;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("ListBucketResult")) {
                if (name.equals("Contents")) {
                    currentObject = new COSObjectSummary();
                    currentObject.setBucketName(objectListing.getBucketName());
                }
            }

            else if (in("ListBucketResult", "Contents")) {
                if (name.equals("Owner")) {
                    currentOwner = new Owner();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (atTopLevel()) {
                if (name.equals("ListBucketResult")) {
                    /*
                     * COS only includes the NextMarker XML element if the
                     * request specified a delimiter, but for consistency we'd
                     * like to always give easy access to the next marker if
                     * we're returning a list of results that's truncated.
                     */
                    if (objectListing.isTruncated() && objectListing.getNextMarker() == null) {

                        String nextMarker = null;
                        if (!objectListing.getObjectSummaries().isEmpty()) {
                            nextMarker = objectListing.getObjectSummaries()
                                    .get(objectListing.getObjectSummaries().size() - 1).getKey();

                        } else if (!objectListing.getCommonPrefixes().isEmpty()) {
                            nextMarker = objectListing.getCommonPrefixes()
                                    .get(objectListing.getCommonPrefixes().size() - 1);
                        } else {
                            log.error("COS response indicates truncated results, "
                                    + "but contains no object summaries or " + "common prefixes.");
                        }

                        objectListing.setNextMarker(nextMarker);
                    }
                }
            }

            else if (in("ListBucketResult")) {
                if (name.equals("Name")) {
                    objectListing.setBucketName(getText());
                    if (log.isDebugEnabled()) {
                        log.debug("Examining listing for bucket: " + objectListing.getBucketName());
                    }

                } else if (name.equals("Prefix")) {
                    objectListing.setPrefix(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));

                } else if (name.equals("Marker")) {
                    objectListing.setMarker(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));

                } else if (name.equals("NextMarker")) {
                    objectListing
                            .setNextMarker(decodeIfSpecified(getText(), shouldSDKDecodeResponse));

                } else if (name.equals("MaxKeys")) {
                    objectListing.setMaxKeys(parseInt(getText()));

                } else if (name.equals("Delimiter")) {
                    objectListing.setDelimiter(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));

                } else if (name.equals("EncodingType")) {
                    objectListing.setEncodingType(
                            shouldSDKDecodeResponse ? null : checkForEmptyString(getText()));
                } else if (name.equals("IsTruncated")) {
                    String isTruncatedStr = getText();

                    if (isTruncatedStr.startsWith("false")) {
                        objectListing.setTruncated(false);
                    } else if (isTruncatedStr.startsWith("true")) {
                        objectListing.setTruncated(true);
                    } else {
                        throw new IllegalStateException(
                                "Invalid value for IsTruncated field: " + isTruncatedStr);
                    }

                } else if (name.equals("Contents")) {
                    objectListing.getObjectSummaries().add(currentObject);
                    currentObject = null;
                }
            }

            else if (in("ListBucketResult", "Contents")) {
                if (name.equals("Key")) {
                    lastKey = getText();
                    currentObject.setKey(decodeIfSpecified(lastKey, shouldSDKDecodeResponse));
                } else if (name.equals("LastModified")) {
                    currentObject.setLastModified(DateUtils.parseISO8601Date(getText()));

                } else if (name.equals("ETag")) {
                    currentObject.setETag(StringUtils.removeQuotes(getText()));

                } else if (name.equals("Size")) {
                    currentObject.setSize(parseLong(getText()));

                } else if (name.equals("StorageClass")) {
                    currentObject.setStorageClass(getText());

                } else if (name.equals("Owner")) {
                    currentObject.setOwner(currentOwner);
                    currentOwner = null;
                }
            }

            else if (in("ListBucketResult", "Contents", "Owner")) {
                if (name.equals("ID")) {
                    currentOwner.setId(getText());

                } else if (name.equals("DisplayName")) {
                    currentOwner.setDisplayName(getText());
                }
            }

            else if (in("ListBucketResult", "CommonPrefixes")) {
                if (name.equals("Prefix")) {
                    objectListing.getCommonPrefixes()
                            .add(decodeIfSpecified(getText(), shouldSDKDecodeResponse));
                }
            }
        }

    }

    /**
     * Handler for ListAllMyBuckets response XML documents. The document is parsed into
     * {@link Bucket}s available via the {@link #getBuckets()} method.
     */
    public static class ListAllMyBucketsHandler extends AbstractHandler {

        private final List<Bucket> buckets = new ArrayList<Bucket>();
        private Owner bucketsOwner = null;

        private Bucket currentBucket = null;

        /**
         * @return the buckets listed in the document.
         */
        public List<Bucket> getBuckets() {
            return buckets;
        }

        /**
         * @return the owner of the buckets.
         */
        public Owner getOwner() {
            return bucketsOwner;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("ListAllMyBucketsResult")) {
                if (name.equals("Owner")) {
                    bucketsOwner = new Owner();
                }
            } else if (in("ListAllMyBucketsResult", "Buckets")) {
                if (name.equals("Bucket")) {
                    currentBucket = new Bucket();
                    currentBucket.setOwner(bucketsOwner);
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("ListAllMyBucketsResult", "Owner")) {
                if (name.equals("ID")) {
                    bucketsOwner.setId(getText());

                } else if (name.equals("DisplayName")) {
                    bucketsOwner.setDisplayName(getText());
                }
            }

            else if (in("ListAllMyBucketsResult", "Buckets")) {
                if (name.equals("Bucket")) {
                    buckets.add(currentBucket);
                    currentBucket = null;
                }
            }

            else if (in("ListAllMyBucketsResult", "Buckets", "Bucket")) {
                if (name.equals("Name")) {
                    currentBucket.setName(getText());

                } else if (name.equals("CreationDate")) {
                    Date creationDate = DateUtils.parseISO8601Date(getText());
                    currentBucket.setCreationDate(creationDate);
                } else if (name.equals("CreateDate")) {
                    Date creationDate = DateUtils.parseISO8601Date(getText());
                    currentBucket.setCreationDate(creationDate);
                } else if (name.equals("Location")) {
                    currentBucket.setLocation(getText());
                } else if (name.equals("BucketType")) {
                    currentBucket.setBucketType(getText());
                } else if (name.equals("Type")) {
                    currentBucket.setType(getText());
                }
            }
        }
    }

    public static class ImagePersistenceHandler extends AbstractHandler {

        private final CIUploadResult ciUploadResult = new CIUploadResult();
        private CIObject ciObject;
        private QRcodeInfo qRcodeInfo;
        public CIUploadResult getCiUploadResult() {
            return ciUploadResult;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if (in("UploadResult")) {
                if (name.equals("OriginalInfo")) {
                    ciUploadResult.setOriginalInfo(new OriginalInfo());
                } else if (name.equals("ProcessResults")) {
                    ciUploadResult.setProcessResults(new ProcessResults());
                }
            } else if(in("UploadResult", "OriginalInfo")) {
                if(name.equals("ImageInfo")) {
                    ciUploadResult.getOriginalInfo().setImageInfo(new ImageInfo());
                }
            } else if(in("UploadResult", "ProcessResults")) {
                if(name.equals("Object")) {
                    ciObject = new CIObject();
                }
            } else if(in("UploadResult", "ProcessResults", "Object")) {
                if(name.equals("QRcodeInfo")) {
                    qRcodeInfo = new QRcodeInfo();
                }
            } else if(in("UploadResult", "ProcessResults", "Object", "QRcodeInfo")) {
                if(name.equals("CodeLocation")) {
                    qRcodeInfo.setCodeLocation(new CodeLocation());
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("UploadResult", "OriginalInfo")) {
                OriginalInfo originalInfo = ciUploadResult.getOriginalInfo();
                if (name.equals("Key")) {
                    originalInfo.setKey(getText());
                } else if (name.equals("Location")) {
                    originalInfo.setLocation(getText());
                } else if(name.equals("ETag")) {
                    originalInfo.setEtag(StringUtils.removeQuotes(getText()));
                }
            } else if (in("UploadResult", "OriginalInfo", "ImageInfo")) {
                ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
                if (name.equals("Format")) {
                    imageInfo.setFormat(getText());
                } else if(name.equals("Width")) {
                    imageInfo.setWidth(Integer.parseInt(getText()));
                } else if(name.equals("Height")) {
                    imageInfo.setHeight(Integer.parseInt(getText()));
                } else if(name.equals("Quality")) {
                    imageInfo.setQuality(Integer.parseInt(getText()));
                } else if(name.equals("Ave")) {
                    imageInfo.setAve(getText());
                } else if(name.equals("Orientation")) {
                    imageInfo.setOrientation(Integer.parseInt(getText()));
                }
            } else if(in("UploadResult", "ProcessResults")) {
                if(name.equals("Object")) {
                    if(ciUploadResult.getProcessResults().getObjectList() == null) {
                        ciUploadResult.getProcessResults().setObjectList(new LinkedList<CIObject>());
                    }
                    ciUploadResult.getProcessResults().getObjectList().add(ciObject);
                }
            } else if (in("UploadResult", "ProcessResults", "Object")) {
                if (name.equals("Key")) {
                    ciObject.setKey(getText());
                } else if(name.equals("Location")) {
                    ciObject.setLocation(getText());
                } else if(name.equals("Format")) {
                    ciObject.setFormat(getText());
                } else if(name.equals("Width")) {
                    ciObject.setWidth(Integer.parseInt(getText()));
                } else if(name.equals("Height")) {
                    ciObject.setHeight(Integer.parseInt(getText()));
                } else if(name.equals("Size")) {
                    ciObject.setSize(Integer.parseInt(getText()));
                } else if(name.equals("Quality")) {
                    ciObject.setQuality(Integer.parseInt(getText()));
                }  else if(name.equals("ETag")) {
                    ciObject.setEtag(StringUtils.removeQuotes(getText()));
                } else if(name.equals("CodeStatus")) {
                    ciObject.setCodeStatus(Integer.parseInt(getText()));
                } else if(name.equals("QRcodeInfo")) {
                    if(ciObject.getQRcodeInfoList() == null) {
                        ciObject.setQRcodeInfoList(new LinkedList<QRcodeInfo>());
                    }
                    ciObject.getQRcodeInfoList().add(qRcodeInfo);
                } else if(name.equals("WatermarkStatus")) {
                    ciObject.setWatermarkStatus(Integer.parseInt(getText()));
                }
            } else if(in("UploadResult", "ProcessResults", "Object", "QRcodeInfo")) {
                if(name.equals("CodeUrl")) {
                    qRcodeInfo.setCodeUrl(getText());
                }
            } else if(in("UploadResult", "ProcessResults", "Object", "QRcodeInfo", "CodeLocation")) {
                CodeLocation codeLocation = qRcodeInfo.getCodeLocation();
                if(codeLocation.getPoints() == null) {
                    codeLocation.setPoints(new LinkedList<String>());
                }
                if(name.equals("Point")) {
                    codeLocation.getPoints().add(getText());
                }
            }
        }
    }

    /**
     * Handler for AccessControlList response XML documents. The document is parsed into an
     * {@link AccessControlList} object available via the {@link #getAccessControlList()} method.
     */
    public static class AccessControlListHandler extends AbstractHandler {

        private final AccessControlList accessControlList = new AccessControlList();

        private Grantee currentGrantee = null;
        private Permission currentPermission = null;

        /**
         * @return an object representing the ACL document.
         */
        public AccessControlList getAccessControlList() {
            return accessControlList;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("AccessControlPolicy")) {
                if (name.equals("Owner")) {
                    accessControlList.setOwner(new Owner());

                }
            }

            else if (in("AccessControlPolicy", "AccessControlList", "Grant")) {
                if (name.equals("Grantee")) {
                    String type = XmlResponsesSaxParser.findAttributeValue("xsi:type", attrs);

                    if ("Group".equals(type)) {
                        /*
                         * Nothing to do for GroupGrantees here since we
                         * can't construct an empty enum value early.
                         */
                    } else if ("CanonicalUser".equals(type)) {
                        currentGrantee = new UinGrantee(null);
                    }
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("AccessControlPolicy", "Owner")) {
                if (name.equals("ID")) {
                    accessControlList.getOwner().setId(getText());
                } else if (name.equals("DisplayName")) {
                    accessControlList.getOwner().setDisplayName(getText());
                }
            }

            else if (in("AccessControlPolicy", "AccessControlList")) {
                if (name.equals("Grant")) {
                    accessControlList.grantPermission(currentGrantee, currentPermission);

                    currentGrantee = null;
                    currentPermission = null;
                }
            }

            else if (in("AccessControlPolicy", "AccessControlList", "Grant")) {
                if (name.equals("Permission")) {
                    currentPermission = Permission.parsePermission(getText());
                }
            }

            else if (in("AccessControlPolicy", "AccessControlList", "Grant", "Grantee")) {
                if (name.equals("ID")) {
                    currentGrantee.setIdentifier(getText());
                } else if (name.equals("URI")) {
                    currentGrantee = GroupGrantee.parseGroupGrantee(getText());
                } else if (name.equals("DisplayName")) {
                    ((UinGrantee) currentGrantee).setDisplayName(getText());
                }
            }
        }
    }


    /**
     * Handler for LoggingStatus response XML documents for a bucket. The document is parsed into an
     * {@link BucketLoggingConfiguration} object available via the
     * {@link #getBucketLoggingConfiguration()} method.
     */
    // public static class BucketLoggingConfigurationHandler extends AbstractHandler {
    //
    // private final BucketLoggingConfiguration bucketLoggingConfiguration =
    // new BucketLoggingConfiguration();
    //
    // /**
    // * @return
    // * an object representing the bucket's LoggingStatus document.
    // */
    // public BucketLoggingConfiguration getBucketLoggingConfiguration() {
    // return bucketLoggingConfiguration;
    // }
    //
    // @Override
    // protected void doStartElement(
    // String uri,
    // String name,
    // String qName,
    // Attributes attrs) {
    //
    // }
    //
    // @Override
    // protected void doEndElement(String uri, String name, String qName) {
    // if (in("BucketLoggingStatus", "LoggingEnabled")) {
    // if (name.equals("TargetBucket")) {
    // bucketLoggingConfiguration
    // .setDestinationBucketName(getText());
    //
    // } else if (name.equals("TargetPrefix")) {
    // bucketLoggingConfiguration
    // .setLogFilePrefix(getText());
    // }
    // }
    // }
    // }

    /**
     * Handler for CreateBucketConfiguration response XML documents for a bucket. The document is
     * parsed into a String representing the bucket's location, available via the
     * {@link #getLocation()} method.
     */
    public static class BucketLocationHandler extends AbstractHandler {

        private String location = null;

        /**
         * @return the bucket's location.
         */
        public String getLocation() {
            return location;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (atTopLevel()) {
                if (name.equals("LocationConstraint")) {
                    String elementText = getText();
                    if (elementText.length() == 0) {
                        location = null;
                    } else {
                        location = elementText;
                    }
                }
            }
        }
    }


    public static class CopyObjectResultHandler extends AbstractSSEHandler
            implements ObjectExpirationResult, VIDResult {

        // Data items for successful copy
        private final CopyObjectResult result = new CopyObjectResult();

        // Data items for failed copy
        private String errorCode = null;
        private String errorMessage = null;
        private String errorRequestId = null;
        private String errorHostId = null;
        /** The crc64ecma value for this object */
        private String crc64Ecma;
        private boolean receivedErrorResponse = false;

        @Override
        public void setDateStr(String dateStr) {
            result.setDateStr(dateStr);
        }

        @Override
        public String getDateStr() {
            return result.getDateStr();
        }

        @Override
        public void setRequestId(String requestId) {
            result.setRequestId(requestId);
        }

        @Override
        public String getRequestId() {
            return result.getRequestId();
        }

        @Override
        protected ServerSideEncryptionResult sseResult() {
            return result;
        }

        public Date getLastModified() {
            return result.getLastModifiedDate();
        }

        public String getVersionId() {
            return result.getVersionId();
        }

        public void setVersionId(String versionId) {
            result.setVersionId(versionId);
        }

        public String getCrc64Ecma() {
            return result.getCrc64Ecma();
        }

        public void setCrc64Ecma(String crc64Ecma) {
            result.setCrc64Ecma(crc64Ecma);
        }

        @Override
        public Date getExpirationTime() {
            return result.getExpirationTime();
        }

        @Override
        public void setExpirationTime(Date expirationTime) {
            result.setExpirationTime(expirationTime);
        }

        @Override
        public String getExpirationTimeRuleId() {
            return result.getExpirationTimeRuleId();
        }

        @Override
        public void setExpirationTimeRuleId(String expirationTimeRuleId) {
            result.setExpirationTimeRuleId(expirationTimeRuleId);
        }

        public String getETag() {
            return result.getETag();
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorHostId() {
            return errorHostId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorRequestId() {
            return errorRequestId;
        }

        public boolean isErrorResponse() {
            return receivedErrorResponse;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (atTopLevel()) {
                if (name.equals("CopyObjectResult") || name.equals("CopyPartResult")) {
                    receivedErrorResponse = false;
                } else if (name.equals("Error")) {
                    receivedErrorResponse = true;
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("CopyObjectResult") || in("CopyPartResult")) {
                if (name.equals("LastModified")) {
//                     result.setLastModifiedDate(DateUtils.parseISO8601Date(getText()));
                } else if (name.equals("ETag")) {
                    result.setETag(StringUtils.removeQuotes(getText()));
                } else if (name.equals("VersionId")) {
                    result.setVersionId(getText());
                } else if(name.equals("CRC64")) {
                    result.setCrc64Ecma(getText());
                }
            }

            else if (in("Error")) {
                if (name.equals("Code")) {
                    errorCode = getText();
                } else if (name.equals("Message")) {
                    errorMessage = getText();
                } else if (name.equals("RequestId")) {
                    errorRequestId = getText();
                } else if (name.equals("HostId")) {
                    errorHostId = getText();
                }
            }
        }
    }


    // /**
    // * Handler for parsing RequestPaymentConfiguration XML response associated
    // * with an Qcloud COS bucket. The XML response is parsed into a
    // * <code>RequestPaymentConfiguration</code> object.
    // */
    // public static class RequestPaymentConfigurationHandler extends AbstractHandler {
    //
    // private String payer = null;
    //
    // public RequestPaymentConfiguration getConfiguration(){
    // return new RequestPaymentConfiguration(Payer.valueOf(payer));
    // }
    //
    // @Override
    // protected void doStartElement(
    // String uri,
    // String name,
    // String qName,
    // Attributes attrs) {
    //
    // }
    //
    // @Override
    // protected void doEndElement(String uri, String name, String qName) {
    // if (in("RequestPaymentConfiguration")) {
    // if (name.equals("Payer")) {
    // payer = getText();
    // }
    // }
    // }
    // }
    //
    /**
     * Handler for ListVersionsResult XML document.
     */
    public static class ListVersionsHandler extends AbstractHandler {

        private final VersionListing versionListing = new VersionListing();
        private final boolean shouldSDKDecodeResponse;

        private COSVersionSummary currentVersionSummary;
        private Owner currentOwner;

        public ListVersionsHandler(final boolean shouldSDKDecodeResponse) {
            this.shouldSDKDecodeResponse = shouldSDKDecodeResponse;
        }

        public VersionListing getListing() {
            return versionListing;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("ListVersionsResult")) {
                if (name.equals("Version")) {
                    currentVersionSummary = new COSVersionSummary();
                    currentVersionSummary.setBucketName(versionListing.getBucketName());

                } else if (name.equals("DeleteMarker")) {
                    currentVersionSummary = new COSVersionSummary();
                    currentVersionSummary.setBucketName(versionListing.getBucketName());
                    currentVersionSummary.setIsDeleteMarker(true);
                }
            }

            else if (in("ListVersionsResult", "Version")
                    || in("ListVersionsResult", "DeleteMarker")) {
                if (name.equals("Owner")) {
                    currentOwner = new Owner();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("ListVersionsResult")) {
                if (name.equals("Name")) {
                    versionListing.setBucketName(getText());

                } else if (name.equals("Prefix")) {
                    versionListing.setPrefix(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));
                } else if (name.equals("KeyMarker")) {
                    versionListing.setKeyMarker(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));
                } else if (name.equals("VersionIdMarker")) {
                    versionListing.setVersionIdMarker(checkForEmptyString(getText()));

                } else if (name.equals("MaxKeys")) {
                    versionListing.setMaxKeys(Integer.parseInt(getText()));

                } else if (name.equals("Delimiter")) {
                    versionListing.setDelimiter(decodeIfSpecified(checkForEmptyString(getText()),
                            shouldSDKDecodeResponse));

                } else if (name.equals("EncodingType")) {
                    versionListing.setEncodingType(
                            shouldSDKDecodeResponse ? null : checkForEmptyString(getText()));
                } else if (name.equals("NextKeyMarker")) {
                    versionListing.setNextKeyMarker(decodeIfSpecified(
                            checkForEmptyString(getText()), shouldSDKDecodeResponse));

                } else if (name.equals("NextVersionIdMarker")) {
                    versionListing.setNextVersionIdMarker(getText());

                } else if (name.equals("IsTruncated")) {
                    versionListing.setTruncated("true".equals(getText()));

                } else if (name.equals("Version") || name.equals("DeleteMarker")) {

                    versionListing.getVersionSummaries().add(currentVersionSummary);

                    currentVersionSummary = null;
                }
            }

            else if (in("ListVersionsResult", "CommonPrefixes")) {
                if (name.equals("Prefix")) {
                    final String commonPrefix = checkForEmptyString(getText());
                    versionListing.getCommonPrefixes().add(shouldSDKDecodeResponse
                            ? UrlEncoderUtils.urlDecode(commonPrefix) : commonPrefix);
                }
            }

            else if (in("ListVersionsResult", "Version")
                    || in("ListVersionsResult", "DeleteMarker")) {

                if (name.equals("Key")) {
                    currentVersionSummary
                            .setKey(decodeIfSpecified(getText(), shouldSDKDecodeResponse));

                } else if (name.equals("VersionId")) {
                    currentVersionSummary.setVersionId(getText());

                } else if (name.equals("IsLatest")) {
                    currentVersionSummary.setIsLatest("true".equals(getText()));

                } else if (name.equals("LastModified")) {
                    currentVersionSummary.setLastModified(DateUtils.parseISO8601Date(getText()));

                } else if (name.equals("ETag")) {
                    currentVersionSummary.setETag(StringUtils.removeQuotes(getText()));

                } else if (name.equals("Size")) {
                    currentVersionSummary.setSize(Long.parseLong(getText()));

                } else if (name.equals("Owner")) {
                    currentVersionSummary.setOwner(currentOwner);
                    currentOwner = null;

                } else if (name.equals("StorageClass")) {
                    currentVersionSummary.setStorageClass(getText());
                }
            }

            else if (in("ListVersionsResult", "Version", "Owner")
                    || in("ListVersionsResult", "DeleteMarker", "Owner")) {

                if (name.equals("ID")) {
                    currentOwner.setId(getText());
                } else if (name.equals("DisplayName")) {
                    currentOwner.setDisplayName(getText());
                }
            }
        }
    }
    //
    // public static class BucketWebsiteConfigurationHandler extends AbstractHandler {
    //
    // private final BucketWebsiteConfiguration configuration =
    // new BucketWebsiteConfiguration(null);
    //
    // private RoutingRuleCondition currentCondition = null;
    // private RedirectRule currentRedirectRule = null;
    // private RoutingRule currentRoutingRule = null;
    //
    // public BucketWebsiteConfiguration getConfiguration() {
    // return configuration;
    // }
    //
    // @Override
    // protected void doStartElement(
    // String uri,
    // String name,
    // String qName,
    // Attributes attrs) {
    //
    // if (in("WebsiteConfiguration")) {
    // if (name.equals("RedirectAllRequestsTo")) {
    // currentRedirectRule = new RedirectRule();
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RoutingRules")) {
    // if (name.equals("RoutingRule")) {
    // currentRoutingRule = new RoutingRule();
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule")) {
    // if (name.equals("Condition")) {
    // currentCondition = new RoutingRuleCondition();
    // } else if (name.equals("Redirect")) {
    // currentRedirectRule = new RedirectRule();
    // }
    // }
    // }
    //
    // @Override
    // protected void doEndElement(String uri, String name, String qName) {
    // if (in("WebsiteConfiguration")) {
    // if (name.equals("RedirectAllRequestsTo")) {
    // configuration.setRedirectAllRequestsTo(currentRedirectRule);
    // currentRedirectRule = null;
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "IndexDocument")) {
    // if (name.equals("Suffix")) {
    // configuration.setIndexDocumentSuffix(getText());
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "ErrorDocument")) {
    // if (name.equals("Key")) {
    // configuration.setErrorDocument(getText());
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RoutingRules")) {
    // if (name.equals("RoutingRule")) {
    // configuration.getRoutingRules().add(currentRoutingRule);
    // currentRoutingRule = null;
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule")) {
    // if (name.equals("Condition")) {
    // currentRoutingRule.setCondition(currentCondition);
    // currentCondition = null;
    // } else if (name.equals("Redirect")) {
    // currentRoutingRule.setRedirect(currentRedirectRule);
    // currentRedirectRule = null;
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule", "Condition")) {
    // if (name.equals("KeyPrefixEquals")) {
    // currentCondition.setKeyPrefixEquals(getText());
    // } else if (name.equals("HttpErrorCodeReturnedEquals")) {
    // currentCondition.setHttpErrorCodeReturnedEquals(getText());
    // }
    // }
    //
    // else if (in("WebsiteConfiguration", "RedirectAllRequestsTo")
    // || in("WebsiteConfiguration", "RoutingRules", "RoutingRule", "Redirect")) {
    //
    // if (name.equals("Protocol")) {
    // currentRedirectRule.setProtocol(getText());
    //
    // } else if (name.equals("HostName")) {
    // currentRedirectRule.setHostName(getText());
    //
    // } else if (name.equals("ReplaceKeyPrefixWith")) {
    // currentRedirectRule.setReplaceKeyPrefixWith(getText());
    //
    // } else if (name.equals("ReplaceKeyWith")) {
    // currentRedirectRule.setReplaceKeyWith(getText());
    //
    // } else if (name.equals("HttpRedirectCode")) {
    // currentRedirectRule.setHttpRedirectCode(getText());
    // }
    // }
    // }
    // }
    //
    public static class BucketVersioningConfigurationHandler extends AbstractHandler {

        private final BucketVersioningConfiguration configuration =
                new BucketVersioningConfiguration();

        public BucketVersioningConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("VersioningConfiguration")) {
                if (name.equals("Status")) {
                    configuration.setStatus(getText());

                }
            }
        }
    }


    /*
     * <?xml version="1.0" encoding="UTF-8"?> <CompleteMultipartUploadResult>
     * <Location>http://Example-Bucket.cn-north.myqcloud.com/Example-Object</Location>
     * <Bucket>Example-Bucket</Bucket> <Key>Example-Object</Key>
     * <ETag>"3858f62230ac3c915f300c664312c11f-9"</ETag> </CompleteMultipartUploadResult>
     *
     * Or if an error occurred while completing:
     *
     * <?xml version="1.0" encoding="UTF-8"?> <Error> <Code>InternalError</Code> <Message>We
     * encountered an internal error. Please try again.</Message>
     * <RequestId>656c76696e6727732072657175657374</RequestId>
     * <HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId> </Error>
     */
    public static class CompleteMultipartUploadHandler extends AbstractSSEHandler
            implements ObjectExpirationResult, VIDResult {
        // Successful completion
        private CompleteMultipartUploadResult result;
        private CIUploadResult ciUploadResult = new CIUploadResult();
        private OriginalInfo originalInfo;
        private CIObject ciObject;
        private QRcodeInfo qRcodeInfo;


        // Error during completion
        private CosServiceException cse;
        private String traceId;
        private String requestId;
        private String errorCode;

        /**
         * @see com.qcloud.cos.model.CompleteMultipartUploadResult#getExpirationTime()
         */
        @Override
        public Date getExpirationTime() {
            return result == null ? null : result.getExpirationTime();
        }

        /**
         * @see com.qcloud.cos.model.CompleteMultipartUploadResult#setExpirationTime(java.util.Date)
         */
        @Override
        public void setExpirationTime(Date expirationTime) {
            if (result != null) {
                result.setExpirationTime(expirationTime);
            }
        }

        /**
         * @see com.qcloud.cos.model.CompleteMultipartUploadResult#getExpirationTimeRuleId()
         */
        @Override
        public String getExpirationTimeRuleId() {
            return result == null ? null : result.getExpirationTimeRuleId();
        }

        /**
         * @see com.qcloud.cos.model.CompleteMultipartUploadResult#setExpirationTimeRuleId(java.lang.String)
         */
        @Override
        public void setExpirationTimeRuleId(String expirationTimeRuleId) {
            if (result != null) {
                result.setExpirationTimeRuleId(expirationTimeRuleId);
            }
        }

        @Override
        public String getRequestId() {
            return result == null ? null : result.getRequestId();
        }

        @Override
        public void setRequestId(String requestId) {
            if (result != null) {
                result.setRequestId(requestId);
            }
        }

        @Override
        public String getDateStr() {
            return result == null ? null : result.getDateStr();
        }

        @Override
        public void setDateStr(String dateStr) {
            if (result != null) {
                result.setDateStr(dateStr);
            }
        }

        public CompleteMultipartUploadResult getCompleteMultipartUploadResult() {
            return result;
        }

        public CosServiceException getCOSException() {
            return cse;
        }

        public CIUploadResult getCiUploadResult() {
            return ciUploadResult;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if (atTopLevel()) {
                if (name.equals("CompleteMultipartUploadResult")) {
                    result = new CompleteMultipartUploadResult();
                    originalInfo = new OriginalInfo();
                    ciUploadResult.setOriginalInfo(originalInfo);
                }
            } else if(in("CompleteMultipartUploadResult")) {
                if(name.equals("ImageInfo")) {
                    ciUploadResult.getOriginalInfo().setImageInfo(new ImageInfo());
                } else if (name.equals("ProcessResults")) {
                    ciUploadResult.setProcessResults(new ProcessResults());
                }
            } else if(in("CompleteMultipartUploadResult", "ProcessResults")) {
                if(name.equals("Object")) {
                    ciObject = new CIObject();
                }
            } else if(in("CompleteMultipartUploadResult", "ProcessResults", "Object")) {
                if(name.equals("QRcodeInfo")) {
                    qRcodeInfo = new QRcodeInfo();
                }
            }  else if(in("CompleteMultipartUploadResult", "ProcessResults", "Object", "QRcodeInfo")) {
                if(name.equals("CodeLocation")) {
                    qRcodeInfo.setCodeLocation(new CodeLocation());
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (atTopLevel()) {
                if (name.equals("Error")) {
                    if (cse != null) {
                        cse.setErrorCode(errorCode);
                        cse.setRequestId(requestId);
                        cse.setTraceId(traceId);
                    }
                }
            } else if (in("Error")) {
                if (name.equals("Code")) {
                    errorCode = getText();
                } else if (name.equals("Message")) {
                    cse = new CosServiceException(getText());
                } else if (name.equals("RequestId")) {
                    requestId = getText();
                } else if (name.equals("HostId")) {
                    traceId = getText();
                }
            } else if (in("CompleteMultipartUploadResult")) {
                if (name.equals("Location")) {
                    result.setLocation(getText());
                    originalInfo.setLocation(getText());
                } else if (name.equals("Bucket")) {
                    result.setBucketName(getText());
                } else if (name.equals("Key")) {
                    result.setKey(getText());
                    originalInfo.setKey(getText());
                } else if (name.equals("ETag")) {
                    result.setETag(StringUtils.removeQuotes(getText()));
                    originalInfo.setEtag(StringUtils.removeQuotes(getText()));
                }
            }  else if (in("CompleteMultipartUploadResult", "ImageInfo")) {
                ImageInfo imageInfo = ciUploadResult.getOriginalInfo().getImageInfo();
                if (name.equals("Format")) {
                    imageInfo.setFormat(getText());
                } else if(name.equals("Width")) {
                    imageInfo.setWidth(Integer.parseInt(getText()));
                } else if(name.equals("Height")) {
                    imageInfo.setHeight(Integer.parseInt(getText()));
                } else if(name.equals("Quality")) {
                    imageInfo.setQuality(Integer.parseInt(getText()));
                } else if(name.equals("Ave")) {
                    imageInfo.setAve(getText());
                } else if(name.equals("Orientation")) {
                    imageInfo.setOrientation(Integer.parseInt(getText()));
                }
            } else if(in("CompleteMultipartUploadResult", "ProcessResults")) {
                if(name.equals("Object")) {
                    if(ciUploadResult.getProcessResults().getObjectList() == null) {
                        ciUploadResult.getProcessResults().setObjectList(new LinkedList<CIObject>());
                    }
                    ciUploadResult.getProcessResults().getObjectList().add(ciObject);
                }
            } else if (in("CompleteMultipartUploadResult", "ProcessResults", "Object")) {
                if (name.equals("Key")) {
                    ciObject.setKey(getText());
                } else if(name.equals("Location")) {
                    ciObject.setLocation(getText());
                } else if(name.equals("Format")) {
                    ciObject.setFormat(getText());
                } else if(name.equals("Width")) {
                    ciObject.setWidth(Integer.parseInt(getText()));
                } else if(name.equals("Height")) {
                    ciObject.setHeight(Integer.parseInt(getText()));
                } else if(name.equals("Size")) {
                    ciObject.setSize(Integer.parseInt(getText()));
                } else if(name.equals("Quality")) {
                    ciObject.setQuality(Integer.parseInt(getText()));
                }  else if(name.equals("ETag")) {
                    ciObject.setEtag(StringUtils.removeQuotes(getText()));
                } else if(name.equals("CodeStatus")) {
                    ciObject.setCodeStatus(Integer.parseInt(getText()));
                } else if(name.equals("QRcodeInfo")) {
                    if(ciObject.getQRcodeInfoList() == null) {
                        ciObject.setQRcodeInfoList(new LinkedList<QRcodeInfo>());
                    }
                    ciObject.getQRcodeInfoList().add(qRcodeInfo);
                } else if(name.equals("WatermarkStatus")) {
                    ciObject.setWatermarkStatus(Integer.parseInt(getText()));
                }
            } else if(in("CompleteMultipartUploadResult", "ProcessResults", "Object", "QRcodeInfo")) {
                if(name.equals("CodeUrl")) {
                    qRcodeInfo.setCodeUrl(getText());
                }
            } else if(in("CompleteMultipartUploadResult", "ProcessResults", "Object",
                    "QRcodeInfo", "CodeLocation")) {
                CodeLocation codeLocation = qRcodeInfo.getCodeLocation();
                if(codeLocation.getPoints() == null) {
                    codeLocation.setPoints(new LinkedList<String>());
                }
                if(name.equals("Point")) {
                    codeLocation.getPoints().add(getText());
                }
            }
        }

        @Override
        protected ServerSideEncryptionResult sseResult() {
            return result;
        }
    }


    /*
     * <?xml version="1.0" encoding="UTF-8"?> <InitiateMultipartUploadResult>
     * <Bucket>example-bucket</Bucket> <Key>example-object</Key>
     * <UploadId>VXBsb2FkIElEIGZvciA2aWWpbmcncyBteS1tb3ZpZS5tMnRzIHVwbG9hZA</UploadId>
     * </InitiateMultipartUploadResult>
     */
    public static class InitiateMultipartUploadHandler extends AbstractHandler {

        private final InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();

        public InitiateMultipartUploadResult getInitiateMultipartUploadResult() {
            return result;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("InitiateMultipartUploadResult")) {
                if (name.equals("Bucket")) {
                    result.setBucketName(getText());

                } else if (name.equals("Key")) {
                    result.setKey(getText());

                } else if (name.equals("UploadId")) {
                    result.setUploadId(getText());
                }
            }
        }
    }


    public static class ListMultipartUploadsHandler extends AbstractHandler {

        private final MultipartUploadListing result = new MultipartUploadListing();

        private MultipartUpload currentMultipartUpload;
        private Owner currentOwner;

        public MultipartUploadListing getListMultipartUploadsResult() {
            return result;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("ListMultipartUploadsResult")) {
                if (name.equals("Upload")) {
                    currentMultipartUpload = new MultipartUpload();
                }
            } else if (in("ListMultipartUploadsResult", "Upload")) {
                if (name.equals("Owner") || name.equals("Initiator")) {
                    currentOwner = new Owner();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("ListMultipartUploadsResult")) {
                if (name.equals("Bucket")) {
                    result.setBucketName(getText());
                } else if (name.equals("KeyMarker")) {
                    result.setKeyMarker(checkForEmptyString(getText()));
                } else if (name.equals("Delimiter")) {
                    result.setDelimiter(checkForEmptyString(getText()));
                } else if (name.equals("Prefix")) {
                    result.setPrefix(checkForEmptyString(getText()));
                } else if (name.equals("UploadIdMarker")) {
                    result.setUploadIdMarker(checkForEmptyString(getText()));
                } else if (name.equals("NextKeyMarker")) {
                    result.setNextKeyMarker(checkForEmptyString(getText()));
                } else if (name.equals("NextUploadIdMarker")) {
                    result.setNextUploadIdMarker(checkForEmptyString(getText()));
                } else if (name.equals("MaxUploads")) {
                    result.setMaxUploads(Integer.parseInt(getText()));
                } else if (name.equals("EncodingType")) {
                    result.setEncodingType(checkForEmptyString(getText()));
                } else if (name.equals("IsTruncated")) {
                    result.setTruncated(Boolean.parseBoolean(getText()));
                } else if (name.equals("Upload")) {
                    result.getMultipartUploads().add(currentMultipartUpload);
                    currentMultipartUpload = null;
                }
            }

            else if (in("ListMultipartUploadsResult", "CommonPrefixes")) {
                if (name.equals("Prefix")) {
                    result.getCommonPrefixes().add(getText());
                }
            }

            else if (in("ListMultipartUploadsResult", "Upload")) {
                if (name.equals("Key")) {
                    currentMultipartUpload.setKey(getText());
                } else if (name.equals("UploadId")) {
                    currentMultipartUpload.setUploadId(getText());
                } else if (name.equals("Owner")) {
                    currentMultipartUpload.setOwner(currentOwner);
                    currentOwner = null;
                } else if (name.equals("Initiator")) {
                    currentMultipartUpload.setInitiator(currentOwner);
                    currentOwner = null;
                } else if (name.equals("Initiated")) {
                    currentMultipartUpload.setInitiated(DateUtils.parseISO8601Date(getText()));
                }
            }

            else if (in("ListMultipartUploadsResult", "Upload", "Owner")
                    || in("ListMultipartUploadsResult", "Upload", "Initiator")) {

                if (name.equals("ID")) {
                    currentOwner.setId(checkForEmptyString(getText()));
                } else if (name.equals("DisplayName")) {
                    currentOwner.setDisplayName(checkForEmptyString(getText()));
                }
            }
        }
    }


    public static class ListPartsHandler extends AbstractHandler {

        private final PartListing result = new PartListing();

        private PartSummary currentPart;
        private Owner currentOwner;

        public PartListing getListPartsResult() {
            return result;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("ListPartsResult")) {
                if (name.equals("Part")) {
                    currentPart = new PartSummary();
                } else if (name.equals("Owner") || name.equals("Initiator")) {
                    currentOwner = new Owner();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("ListPartsResult")) {
                if (name.equals("Bucket")) {
                    result.setBucketName(getText());
                } else if (name.equals("Key")) {
                    result.setKey(getText());
                } else if (name.equals("UploadId")) {
                    result.setUploadId(getText());
                } else if (name.equals("Owner")) {
                    result.setOwner(currentOwner);
                    currentOwner = null;
                } else if (name.equals("Initiator")) {
                    result.setInitiator(currentOwner);
                    currentOwner = null;
                } else if (name.equals("StorageClass")) {
                    result.setStorageClass(getText());
                } else if (name.equals("PartNumberMarker")) {
                    result.setPartNumberMarker(parseInteger(getText()));
                } else if (name.equals("NextPartNumberMarker")) {
                    result.setNextPartNumberMarker(parseInteger(getText()));
                } else if (name.equals("MaxParts")) {
                    result.setMaxParts(parseInteger(getText()));
                } else if (name.equals("EncodingType")) {
                    result.setEncodingType(checkForEmptyString(getText()));
                } else if (name.equals("IsTruncated")) {
                    result.setTruncated(Boolean.parseBoolean(getText()));
                } else if (name.equals("Part")) {
                    result.getParts().add(currentPart);
                    currentPart = null;
                }
            }

            else if (in("ListPartsResult", "Part")) {
                if (name.equals("PartNumber")) {
                    currentPart.setPartNumber(Integer.parseInt(getText()));
                } else if (name.equals("LastModified")) {
                    currentPart.setLastModified(DateUtils.parseISO8601Date(getText()));
                } else if (name.equals("ETag")) {
                    currentPart.setETag(StringUtils.removeQuotes(getText()));
                } else if (name.equals("Size")) {
                    currentPart.setSize(Long.parseLong(getText()));
                }
            }

            else if (in("ListPartsResult", "Owner") || in("ListPartsResult", "Initiator")) {

                if (name.equals("ID")) {
                    currentOwner.setId(checkForEmptyString(getText()));
                } else if (name.equals("DisplayName")) {
                    currentOwner.setDisplayName(checkForEmptyString(getText()));
                }
            }
        }

        private Integer parseInteger(String text) {
            text = checkForEmptyString(getText());
            if (text == null)
                return null;
            return Integer.parseInt(text);
        }
    }

    public static class BucketWebsiteConfigurationHandler extends AbstractHandler {

        private final BucketWebsiteConfiguration configuration =
                new BucketWebsiteConfiguration(null);

        private RoutingRuleCondition currentCondition = null;
        private RedirectRule currentRedirectRule = null;
        private RoutingRule currentRoutingRule = null;

        public BucketWebsiteConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected  void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

            if (in("WebsiteConfiguration")) {
                if (name.equals("RedirectAllRequestsTo")) {
                    currentRedirectRule = new RedirectRule();
                }
            }

            else if (in("WebsiteConfiguration", "RoutingRules")) {
                if (name.equals("RoutingRule")) {
                    currentRoutingRule = new RoutingRule();
                }
            }

            else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule")) {
                if (name.equals("Condition")) {
                    currentCondition = new RoutingRuleCondition();
                } else if (name.equals("Redirect")) {
                    currentRedirectRule = new RedirectRule();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("WebsiteConfiguration")) {
                if (name.equals("RedirectAllRequestsTo")) {
                    configuration.setRedirectAllRequestsTo(currentRedirectRule);
                    currentRedirectRule = null;
                }
            }

            else if (in("WebsiteConfiguration", "IndexDocument")) {
                if (name.equals("Suffix")) {
                    configuration.setIndexDocumentSuffix(getText());
                }
            }

            else if (in("WebsiteConfiguration", "ErrorDocument")) {
                if (name.equals("Key")) {
                    configuration.setErrorDocument(getText());
                }
            }

            else if (in("WebsiteConfiguration", "RoutingRules")) {
                if (name.equals("RoutingRule")) {
                    configuration.getRoutingRules().add(currentRoutingRule);
                    currentRoutingRule = null;
                }
            }

            else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule")) {
                if (name.equals("Condition")) {
                    currentRoutingRule.setCondition(currentCondition);
                    currentCondition = null;
                } else if (name.equals("Redirect")) {
                    currentRoutingRule.setRedirect(currentRedirectRule);
                    currentRedirectRule = null;
                }
            }

            else if (in("WebsiteConfiguration", "RoutingRules", "RoutingRule", "Condition")) {
                if (name.equals("KeyPrefixEquals")) {
                    currentCondition.setKeyPrefixEquals(getText());
                } else if (name.equals("HttpErrorCodeReturnedEquals")) {
                    currentCondition.setHttpErrorCodeReturnedEquals(getText());
                }
            }

            else if (in("WebsiteConfiguration", "RedirectAllRequestsTo")
                    || in("WebsiteConfiguration", "RoutingRules", "RoutingRule", "Redirect")) {

                if (name.equals("Protocol")) {
                    currentRedirectRule.setProtocol(getText());

                } else if (name.equals("HostName")) {
                    currentRedirectRule.setHostName(getText());

                } else if (name.equals("ReplaceKeyPrefixWith")) {
                    currentRedirectRule.setReplaceKeyPrefixWith(getText());

                } else if (name.equals("ReplaceKeyWith")) {
                    currentRedirectRule.setReplaceKeyWith(getText());

                } else if (name.equals("HttpRedirectCode")) {
                    currentRedirectRule.setHttpRedirectCode(getText());
                }
            }
        }
    }

    public static class BucketReplicationConfigurationHandler extends AbstractHandler {

        private final BucketReplicationConfiguration bucketReplicationConfiguration =
                new BucketReplicationConfiguration();
        private ReplicationRule currentRule;
        private ReplicationDestinationConfig destinationConfig;
        private static final String REPLICATION_CONFIG = "ReplicationConfiguration";
        private static final String ROLE = "Role";
        private static final String RULE = "Rule";
        private static final String DESTINATION = "Destination";
        private static final String ID = "ID";
        private static final String PREFIX = "Prefix";
        private static final String STATUS = "Status";
        private static final String BUCKET = "Bucket";
        private static final String STORAGECLASS = "StorageClass";

        public BucketReplicationConfiguration getConfiguration() {
            return bucketReplicationConfiguration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in(REPLICATION_CONFIG)) {
                if (name.equals(RULE)) {
                    currentRule = new ReplicationRule();
                }
            } else if (in(REPLICATION_CONFIG, RULE)) {
                if (name.equals(DESTINATION)) {
                    destinationConfig = new ReplicationDestinationConfig();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in(REPLICATION_CONFIG)) {
                if (name.equals(RULE)) {
                    bucketReplicationConfiguration.addRule(currentRule);
                    currentRule = null;
                    destinationConfig = null;
                } else if (name.equals(ROLE)) {
                    bucketReplicationConfiguration.setRoleName(getText());
                }
            } else if (in(REPLICATION_CONFIG, RULE)) {
                if (name.equals(ID)) {
                    currentRule.setID(getText());
                } else if (name.equals(PREFIX)) {
                    currentRule.setPrefix(getText());
                } else {
                    if (name.equals(STATUS)) {
                        currentRule.setStatus(getText());

                    } else if (name.equals(DESTINATION)) {
                        currentRule.setDestinationConfig(destinationConfig);
                    }
                }
            } else if (in(REPLICATION_CONFIG, RULE, DESTINATION)) {
                if (name.equals(BUCKET)) {
                    destinationConfig.setBucketQCS(getText());
                } else if (name.equals(STORAGECLASS)) {
                    destinationConfig.setStorageClass(getText());
                }
            }
        }
    }
    //
    // public static class BucketTaggingConfigurationHandler extends AbstractHandler {
    //
    // private final BucketTaggingConfiguration configuration =
    // new BucketTaggingConfiguration();
    //
    // private Map<String, String> currentTagSet;
    // private String currentTagKey;
    // private String currentTagValue;
    //
    // public BucketTaggingConfiguration getConfiguration() {
    // return configuration;
    // }
    //
    // @Override
    // protected void doStartElement(
    // String uri,
    // String name,
    // String qName,
    // Attributes attrs) {
    //
    // if (in("Tagging")) {
    // if (name.equals("TagSet")) {
    // currentTagSet = new HashMap<String, String>();
    // }
    // }
    // }
    //
    // @Override
    // protected void doEndElement(String uri, String name, String qName) {
    // if (in("Tagging")) {
    // if (name.equals("TagSet")) {
    // configuration.getAllTagSets()
    // .add(new TagSet(currentTagSet));
    // currentTagSet = null;
    // }
    // }
    //
    // else if (in("Tagging", "TagSet")) {
    // if (name.equals("Tag")) {
    // if (currentTagKey != null && currentTagValue != null) {
    // currentTagSet.put(currentTagKey, currentTagValue);
    // }
    // currentTagKey = null;
    // currentTagValue = null;
    // }
    // }
    //
    // else if (in("Tagging", "TagSet", "Tag")) {
    // if (name.equals("Key")) {
    // currentTagKey = getText();
    // } else if (name.equals("Value")) {
    // currentTagValue = getText();
    // }
    // }
    // }
    // }


    public static class DeleteObjectsHandler extends AbstractHandler {

        private final DeleteObjectsResponse response = new DeleteObjectsResponse();

        private DeletedObject currentDeletedObject = null;
        private DeleteError currentError = null;

        public DeleteObjectsResponse getDeleteObjectResult() {
            return response;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("DeleteResult")) {
                if (name.equals("Deleted")) {
                    currentDeletedObject = new DeletedObject();
                } else if (name.equals("Error")) {
                    currentError = new DeleteError();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("DeleteResult")) {
                if (name.equals("Deleted")) {
                    response.getDeletedObjects().add(currentDeletedObject);
                    currentDeletedObject = null;
                } else if (name.equals("Error")) {
                    response.getErrors().add(currentError);
                    currentError = null;
                }
            }

            else if (in("DeleteResult", "Deleted")) {
                if (name.equals("Key")) {
                    currentDeletedObject.setKey(getText());

                } else if (name.equals("VersionId")) {
                    currentDeletedObject.setVersionId(getText());

                } else if (name.equals("DeleteMarker")) {
                    currentDeletedObject.setDeleteMarker(getText().equals("true"));

                } else if (name.equals("DeleteMarkerVersionId")) {
                    currentDeletedObject.setDeleteMarkerVersionId(getText());
                }
            }

            else if (in("DeleteResult", "Error")) {
                if (name.equals("Key")) {
                    currentError.setKey(getText());

                } else if (name.equals("VersionId")) {
                    currentError.setVersionId(getText());

                } else if (name.equals("Code")) {
                    currentError.setCode(getText());

                } else if (name.equals("Message")) {
                    currentError.setMessage(getText());
                }
            }
        }
    }

    /**
     * <LifecycleConfiguration> <Rule> <ID>logs-rule</ID> <Prefix>logs/</Prefix>
     * <Status>Enabled</Status> <Transition> <Days>30</Days>
     * <StorageClass>STANDARD_IA</StorageClass> </Transition> <Transition> <Days>90</Days>
     * <StorageClass>ARCHIVE</StorageClass> </Transition>
     * <Expiration> <Days>365</Days> </Expiration>
     * <NoncurrentVersionTransition> <NoncurrentDays>7</NoncurrentDays>
     * <StorageClass>STANDARD_IA</StorageClass> </NoncurrentVersionTransition>
     * <NoncurrentVersionTransition> <NoncurrentDays>14</NoncurrentDays>
     * <StorageClass>ARCHIVE</StorageClass> </NoncurrentVersionTransition>
     * <NoncurrentVersionExpiration> <NoncurrentDays>365</NoncurrentDays>
     * </NoncurrentVersionExpiration> </Rule> <Rule> <ID>image-rule</ID> <Prefix>image/</Prefix>
     * <Status>Enabled</Status> <Transition> <Date>2012-12-31T00:00:00.000Z</Date>
     * <StorageClass>ARCHIVE</StorageClass> </Transition>
     * <Expiration> <Date>2020-12-31T00:00:00.000Z</Date> </Expiration> </Rule>
     * </LifecycleConfiguration>
     */
    public static class BucketLifecycleConfigurationHandler extends AbstractHandler {

        private final BucketLifecycleConfiguration configuration =
                new BucketLifecycleConfiguration(new ArrayList<Rule>());

        private Rule currentRule;
        private Transition currentTransition;
        private NoncurrentVersionTransition currentNcvTransition;
        private AbortIncompleteMultipartUpload abortIncompleteMultipartUpload;
        private LifecycleFilter currentFilter;
        private List<LifecycleFilterPredicate> andOperandsList;
        private String currentTagKey;
        private String currentTagValue;

        public BucketLifecycleConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("LifecycleConfiguration")) {
                if (name.equals("Rule")) {
                    currentRule = new Rule();
                }
            } else if (in("LifecycleConfiguration", "Rule")) {
                if (name.equals("Transition")) {
                    currentTransition = new Transition();
                } else if (name.equals("NoncurrentVersionTransition")) {
                    currentNcvTransition = new NoncurrentVersionTransition();
                } else if (name.equals("AbortIncompleteMultipartUpload")) {
                    abortIncompleteMultipartUpload = new AbortIncompleteMultipartUpload();
                } else if (name.equals("Filter")) {
                    currentFilter = new LifecycleFilter();
                }
            } else if (in("LifecycleConfiguration", "Rule", "Filter")) {
                if (name.equals("And")) {
                    andOperandsList = new ArrayList<LifecycleFilterPredicate>();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("LifecycleConfiguration")) {
                if (name.equals("Rule")) {
                    configuration.getRules().add(currentRule);
                    currentRule = null;
                }
            }

            else if (in("LifecycleConfiguration", "Rule")) {
                if (name.equals("ID")) {
                    currentRule.setId(getText());

                } else if (name.equals("Status")) {
                    currentRule.setStatus(getText());

                } else if (name.equals("Transition")) {
                    currentRule.addTransition(currentTransition);
                    currentTransition = null;

                } else if (name.equals("NoncurrentVersionTransition")) {
                    currentRule.addNoncurrentVersionTransition(currentNcvTransition);
                    currentNcvTransition = null;
                } else if (name.equals("AbortIncompleteMultipartUpload")) {
                    currentRule.setAbortIncompleteMultipartUpload(abortIncompleteMultipartUpload);
                    abortIncompleteMultipartUpload = null;
                } else if (name.equals("Filter")) {
                    currentRule.setFilter(currentFilter);
                    currentFilter = null;
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Expiration")) {
                if (name.equals("Date")) {
                    currentRule.setExpirationDate(DateUtils.parseISO8601Date(getText()));
                } else if (name.equals("Days")) {
                    currentRule.setExpirationInDays(Integer.parseInt(getText()));
                } else if (name.equals("ExpiredObjectDeleteMarker")) {
                    if ("true".equals(getText())) {
                        currentRule.setExpiredObjectDeleteMarker(true);
                    }
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Transition")) {
                if (name.equals("StorageClass")) {
                    currentTransition.setStorageClass(getText());
                } else if (name.equals("Date")) {
                    currentTransition.setDate(DateUtils.parseISO8601Date(getText()));

                } else if (name.equals("Days")) {
                    currentTransition.setDays(Integer.parseInt(getText()));
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "NoncurrentVersionExpiration")) {
                if (name.equals("NoncurrentDays")) {
                    currentRule.setNoncurrentVersionExpirationInDays(Integer.parseInt(getText()));
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "NoncurrentVersionTransition")) {
                if (name.equals("StorageClass")) {
                    currentNcvTransition.setStorageClass(getText());
                } else if (name.equals("NoncurrentDays")) {
                    currentNcvTransition.setDays(Integer.parseInt(getText()));
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "AbortIncompleteMultipartUpload")) {
                if (name.equals("DaysAfterInitiation")) {
                    abortIncompleteMultipartUpload
                            .setDaysAfterInitiation(Integer.parseInt(getText()));
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Filter")) {
                if (name.equals("Prefix")) {
                    currentFilter.setPredicate(new LifecyclePrefixPredicate(getText()));
                } else if (name.equals("Tag")) {
                    currentFilter.setPredicate(
                            new LifecycleTagPredicate(new Tag(currentTagKey, currentTagValue)));
                    currentTagKey = null;
                    currentTagValue = null;
                } else if (name.equals("And")) {
                    currentFilter.setPredicate(new LifecycleAndOperator(andOperandsList));
                    andOperandsList = null;
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Filter", "Tag")) {
                if (name.equals("Key")) {
                    currentTagKey = getText();
                } else if (name.equals("Value")) {
                    currentTagValue = getText();
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Filter", "And")) {
                if (name.equals("Prefix")) {
                    andOperandsList.add(new LifecyclePrefixPredicate(getText()));
                } else if (name.equals("Tag")) {
                    andOperandsList.add(
                            new LifecycleTagPredicate(new Tag(currentTagKey, currentTagValue)));
                    currentTagKey = null;
                    currentTagValue = null;
                }
            }

            else if (in("LifecycleConfiguration", "Rule", "Filter", "And", "Tag")) {
                if (name.equals("Key")) {
                    currentTagKey = getText();
                } else if (name.equals("Value")) {
                    currentTagValue = getText();
                }
            }

        }
    }

    public static class BucketCrossOriginConfigurationHandler extends AbstractHandler {

        private final BucketCrossOriginConfiguration configuration =
                new BucketCrossOriginConfiguration(new ArrayList<CORSRule>());

        private CORSRule currentRule;
        private List<AllowedMethods> allowedMethods = null;
        private List<String> allowedOrigins = null;
        private List<String> exposedHeaders = null;
        private List<String> allowedHeaders = null;

        public BucketCrossOriginConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("CORSConfiguration")) {
                if (name.equals("CORSRule")) {
                    currentRule = new CORSRule();
                }
            } else if (in("CORSConfiguration", "CORSRule")) {
                if (name.equals("AllowedOrigin")) {
                    if (allowedOrigins == null) {
                        allowedOrigins = new ArrayList<String>();
                    }
                } else if (name.equals("AllowedMethod")) {
                    if (allowedMethods == null) {
                        allowedMethods = new ArrayList<AllowedMethods>();
                    }
                } else if (name.equals("ExposeHeader")) {
                    if (exposedHeaders == null) {
                        exposedHeaders = new ArrayList<String>();
                    }
                } else if (name.equals("AllowedHeader")) {
                    if (allowedHeaders == null) {
                        allowedHeaders = new LinkedList<String>();
                    }
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("CORSConfiguration")) {
                if (name.equals("CORSRule")) {
                    currentRule.setAllowedHeaders(allowedHeaders);
                    currentRule.setAllowedMethods(allowedMethods);
                    currentRule.setAllowedOrigins(allowedOrigins);
                    currentRule.setExposedHeaders(exposedHeaders);
                    allowedHeaders = null;
                    allowedMethods = null;
                    allowedOrigins = null;
                    exposedHeaders = null;

                    configuration.getRules().add(currentRule);
                    currentRule = null;
                }
            } else if (in("CORSConfiguration", "CORSRule")) {
                if (name.equals("ID")) {
                    currentRule.setId(getText());

                } else if (name.equals("AllowedOrigin")) {
                    allowedOrigins.add(getText());

                } else if (name.equals("AllowedMethod")) {
                    allowedMethods.add(AllowedMethods.fromValue(getText()));

                } else if (name.equals("MaxAgeSeconds")) {
                    currentRule.setMaxAgeSeconds(Integer.parseInt(getText()));

                } else if (name.equals("ExposeHeader")) {
                    exposedHeaders.add(getText());

                } else if (name.equals("AllowedHeader")) {
                    allowedHeaders.add(getText());
                }
            }
        }

    }

    public static class BucketDomainConfigurationHandler extends AbstractHandler {

        private final BucketDomainConfiguration configuration =
                new BucketDomainConfiguration();

        private DomainRule currentRule;
        private String status;
        private String mname;
        private String type;
        private String forcedReplacement;

        public BucketDomainConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if (in("DomainConfiguration")) {
                if (name.equals("DomainRule")) {
                    currentRule = new DomainRule();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("DomainConfiguration")) {
                if (name.equals("DomainRule")) {
                    currentRule.setStatus(status);
                    currentRule.setName(mname);
                    currentRule.setType(type);
                    currentRule.setForcedReplacement(forcedReplacement);

                    configuration.getDomainRules().add(currentRule);
                    currentRule = null;
                    status = null;
                    mname = null;
                    type = null;
                    forcedReplacement = null;
                }
            } else if (in("DomainConfiguration", "DomainRule")) {
                if (name.equals("Status")) {
                    status = getText();
                } else if(name.equals("Name")) {
                    mname = getText();
                } else if(name.equals("Type")) {
                    type = getText();
                } else if(name.equals("ForcedReplacement")) {
                    forcedReplacement = getText();
                }
            }
        }

    }

    public static class BucketRefererConfigurationHandler extends AbstractHandler {

        private final BucketRefererConfiguration configuration =
                new BucketRefererConfiguration();

        public BucketRefererConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("RefererConfiguration")) {
                if (name.equals("Status")) {
                    configuration.setStatus(getText());
                } else if (name.equals("RefererType")) {
                    configuration.setRefererType(getText());
                } else if (name.equals("EmptyReferConfiguration")) {
                    configuration.setEmptyReferConfiguration(getText());
                }
            } else if (in("RefererConfiguration", "DomainList")) {
                if (name.equals("Domain")) {
                    configuration.addDomain(getText());
                }
            }
        }
    }

    private static String findAttributeValue(String qnameToFind, Attributes attrs) {

        for (int i = 0; i < attrs.getLength(); i++) {
            String qname = attrs.getQName(i);
            if (qname.trim().equalsIgnoreCase(qnameToFind.trim())) {
                return attrs.getValue(i);
            }
        }

        return null;
    }

    /**
     * Handler for LoggingStatus response XML documents for a bucket. The
     * document is parsed into an {@link BucketLoggingConfiguration} object available
     * via the {@link #getBucketLoggingConfiguration()} method.
     */
    public static class BucketLoggingConfigurationHandler extends AbstractHandler {

        private final BucketLoggingConfiguration bucketLoggingConfiguration =
                new BucketLoggingConfiguration();

        /**
         * @return
         * an object representing the bucket's LoggingStatus document.
         */
        public BucketLoggingConfiguration getBucketLoggingConfiguration() {
            return bucketLoggingConfiguration;
        }

        @Override
        protected void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("BucketLoggingStatus", "LoggingEnabled")) {
                if (name.equals("TargetBucket")) {
                    bucketLoggingConfiguration
                            .setDestinationBucketName(getText());

                } else if (name.equals("TargetPrefix")) {
                    bucketLoggingConfiguration
                            .setLogFilePrefix(getText());
                }
            }
        }
    }

    public static class GetBucketInventoryConfigurationHandler extends AbstractHandler {

        public static final String SSE_COS = "SSE-COS";
        private final GetBucketInventoryConfigurationResult result = new GetBucketInventoryConfigurationResult();
        private final InventoryConfiguration configuration = new InventoryConfiguration();

        private List<String> optionalFields;
        private InventoryDestination inventoryDestination;
        private InventoryFilter filter;
        private InventoryCosBucketDestination cosBucketDestination;
        private InventorySchedule inventorySchedule;

        public GetBucketInventoryConfigurationResult getResult() {
            return result.withInventoryConfiguration(configuration);
        }

        @Override
        protected void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

            if (in("InventoryConfiguration")) {
                if (name.equals("Destination")) {
                    inventoryDestination = new InventoryDestination();
                } else if(name.equals("Filter")) {
                    filter = new InventoryFilter();
                } else if(name.equals("Schedule")) {
                    inventorySchedule = new InventorySchedule();
                } else if(name.equals("OptionalFields")) {
                    optionalFields = new ArrayList<String>();
                }

            } else if (in("InventoryConfiguration", "Destination")) {
                if (name.equals("COSBucketDestination")) {
                    cosBucketDestination = new InventoryCosBucketDestination();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("InventoryConfiguration")) {
                if (name.equals("Id")) {
                    configuration.setId(getText());

                } else if (name.equals("Destination")) {
                    configuration.setDestination(inventoryDestination);
                    inventoryDestination = null;

                } else if (name.equals("IsEnabled")) {
                    configuration.setEnabled("true".equals(getText()));

                } else if (name.equals("Filter")) {
                    configuration.setInventoryFilter(filter);
                    filter = null;

                } else if (name.equals("IncludedObjectVersions")) {
                    configuration.setIncludedObjectVersions(getText());

                } else if (name.equals("Schedule")) {
                    configuration.setSchedule(inventorySchedule);
                    inventorySchedule = null;

                } else if (name.equals("OptionalFields")) {
                    configuration.setOptionalFields(optionalFields);
                    optionalFields = null;
                }

            } else if (in("InventoryConfiguration", "Destination")) {
                if ( name.equals("COSBucketDestination") ) {
                    inventoryDestination.setCosBucketDestination(cosBucketDestination);
                    cosBucketDestination = null;
                }

            } else if (in("InventoryConfiguration", "Destination", "COSBucketDestination")) {
                if (name.equals("AccountId")) {
                    cosBucketDestination.setAccountId(getText());
                } else if (name.equals("Bucket")) {
                    cosBucketDestination.setBucketArn(getText());
                } else if (name.equals("Format")) {
                    cosBucketDestination.setFormat(getText());
                } else if (name.equals("Prefix")) {
                    cosBucketDestination.setPrefix(getText());
                }
            } else if (in("InventoryConfiguration", "Destination", "COSBucketDestination", "Encryption")) {
                if (name.equals(SSE_COS)) {
                    cosBucketDestination.setEncryption(new ServerSideEncryptionCOS());
                }
            } else if (in("InventoryConfiguration", "Filter")) {
                if (name.equals("Prefix")) {
                    filter.setPredicate(new InventoryPrefixPredicate(getText()));
                }

            } else if (in("InventoryConfiguration", "Schedule")) {
                if (name.equals("Frequency")) {
                    inventorySchedule.setFrequency(getText());
                }

            } else if (in("InventoryConfiguration", "OptionalFields")) {
                if (name.equals("Field")) {
                    optionalFields.add(getText());
                }
            }
        }

    }
    public static class BucketTaggingConfigurationHandler extends AbstractHandler {

        private final BucketTaggingConfiguration configuration =
                new BucketTaggingConfiguration();

        private Map<String, String> currentTagSet;
        private String currentTagKey;
        private String currentTagValue;

        public BucketTaggingConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

            if (in("Tagging")) {
                if (name.equals("TagSet")) {
                    currentTagSet = new LinkedHashMap<String, String>();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Tagging")) {
                if (name.equals("TagSet")) {
                    configuration.getAllTagSets()
                            .add(new TagSet(currentTagSet));
                    currentTagSet = null;
                }
            }

            else if (in("Tagging", "TagSet")) {
                if (name.equals("Tag")) {
                    if (currentTagKey != null && currentTagValue != null) {
                        currentTagSet.put(currentTagKey, currentTagValue);
                    }
                    currentTagKey = null;
                    currentTagValue = null;
                }
            }

            else if (in("Tagging", "TagSet", "Tag")) {
                if (name.equals("Key")) {
                    currentTagKey = getText();
                } else if (name.equals("Value")) {
                    currentTagValue = getText();
                }
            }
        }
    }
    /*
        HTTP/1.1 200 OK
        x-amz-id-2: ITnGT1y4RyTmXa3rPi4hklTXouTf0hccUjo0iCPjz6FnfIutBj3M7fPGlWO2SEWp
        x-amz-request-id: 51991C342C575321
        Date: Wed, 14 May 2014 02:11:22 GMT
        Content-Length: ...

        <ListInventoryConfigurationsResult>
          <InventoryConfiguration>
            ...
          </InventoryConfiguration>
          <InventoryConfiguration>
            ...
          </InventoryConfiguration>
          <IsTruncated>true</IsTruncated>
          <ContinuationToken>token1</ContinuationToken>
          <NextContinuationToken>token2</NextContinuationToken>
        </ListInventoryConfigurationsResult>
 */
    public static class ListBucketInventoryConfigurationsHandler extends AbstractHandler {

        public static final String SSE_COS = "SSE-COS";
        private final ListBucketInventoryConfigurationsResult result = new ListBucketInventoryConfigurationsResult();

        private InventoryConfiguration currentConfiguration;
        private List<String> currentOptionalFieldsList;
        private InventoryDestination currentDestination;
        private InventoryFilter currentFilter;
        private InventoryCosBucketDestination currentCosBucketDestination;
        private InventorySchedule currentSchedule;

        public ListBucketInventoryConfigurationsResult getResult() {
            return result;
        }

        @Override
        protected void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

            if (in("ListInventoryConfigurationResult")) {
                if (name.equals("InventoryConfiguration")) {
                    currentConfiguration = new InventoryConfiguration();
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration")) {
                if (name.equals("Destination")) {
                    currentDestination = new InventoryDestination();
                } else if(name.equals("Filter")) {
                    currentFilter = new InventoryFilter();
                } else if(name.equals("Schedule")) {
                    currentSchedule = new InventorySchedule();
                } else if(name.equals("OptionalFields")) {
                    currentOptionalFieldsList = new ArrayList<String>();
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Destination")) {
                if (name.equals("COSBucketDestination")) {
                    currentCosBucketDestination = new InventoryCosBucketDestination();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("ListInventoryConfigurationResult")) {
                if (name.equals("InventoryConfiguration")) {
                    if (result.getInventoryConfigurationList() == null) {
                        result.setInventoryConfigurationList(new ArrayList<InventoryConfiguration>());
                    }
                    result.getInventoryConfigurationList().add(currentConfiguration);
                    currentConfiguration = null;
                } else if (name.equals("IsTruncated")) {
                    result.setTruncated("true".equals(getText()));
                } else if (name.equals("ContinuationToken")) {
                    result.setContinuationToken(getText());
                } else if (name.equals("NextContinuationToken")) {
                    result.setNextContinuationToken(getText());
                }
            }

            else if (in("ListInventoryConfigurationResult", "InventoryConfiguration")) {
                if (name.equals("Id")) {
                    currentConfiguration.setId(getText());

                } else if (name.equals("Destination")) {
                    currentConfiguration.setDestination(currentDestination);
                    currentDestination = null;

                } else if (name.equals("IsEnabled")) {
                    currentConfiguration.setEnabled("true".equals(getText()));

                } else if (name.equals("Filter")) {
                    currentConfiguration.setInventoryFilter(currentFilter);
                    currentFilter = null;

                } else if (name.equals("IncludedObjectVersions")) {
                    currentConfiguration.setIncludedObjectVersions(getText());

                } else if (name.equals("Schedule")) {
                    currentConfiguration.setSchedule(currentSchedule);
                    currentSchedule = null;

                } else if (name.equals("OptionalFields")) {
                    currentConfiguration.setOptionalFields(currentOptionalFieldsList);
                    currentOptionalFieldsList = null;
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Destination")) {
                if ( name.equals("COSBucketDestination") ) {
                    currentDestination.setCosBucketDestination(currentCosBucketDestination);
                    currentCosBucketDestination = null;
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Destination", "COSBucketDestination")) {
                if (name.equals("AccountId")) {
                    currentCosBucketDestination.setAccountId(getText());
                } else if (name.equals("Bucket")) {
                    currentCosBucketDestination.setBucketArn(getText());
                } else if (name.equals("Format")) {
                    currentCosBucketDestination.setFormat(getText());
                } else if (name.equals("Prefix")) {
                    currentCosBucketDestination.setPrefix(getText());
                }
            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Destination", "COSBucketDestination", "Encryption")) {
                if (name.equals(SSE_COS)) {
                    currentCosBucketDestination.setEncryption(new ServerSideEncryptionCOS());
                }
            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Filter")) {
                if (name.equals("Prefix")) {
                    currentFilter.setPredicate(new InventoryPrefixPredicate(getText()));
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "Schedule")) {
                if (name.equals("Frequency")) {
                    currentSchedule.setFrequency(getText());
                }

            } else if (in("ListInventoryConfigurationResult", "InventoryConfiguration", "OptionalFields")) {
                if (name.equals("Field")) {
                    currentOptionalFieldsList.add(getText());
                }
            }
        }
    }

    /**
     * Handler for unmarshalling the response from GET Object Tagging.
     *
     * <Tagging>
     *     <TagSet>
     *         <Tag>
     *             <Key>Foo</Key>
     *             <Value>1</Value>
     *         </Tag>
     *         <Tag>
     *             <Key>Bar</Key>
     *             <Value>2</Value>
     *         </Tag>
     *         <Tag>
     *             <Key>Baz</Key>
     *             <Value>3</Value>
     *         </Tag>
     *     </TagSet>
     * </Tagging>
     */
    public static class GetObjectTaggingHandler extends AbstractHandler {
        private GetObjectTaggingResult getObjectTaggingResult;
        private List<Tag> tagSet;
        private String currentTagValue;
        private String currentTagKey;

        public GetObjectTaggingResult getResult() {
            return getObjectTaggingResult;
        }


        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if (in("Tagging")) {
                if (name.equals("TagSet")) {
                    tagSet = new ArrayList<Tag>();
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Tagging")) {
                if (name.equals("TagSet")) {
                    getObjectTaggingResult = new GetObjectTaggingResult(tagSet);
                    tagSet = null;
                }
            }
            if (in("Tagging", "TagSet")) {
                if (name.equals("Tag")) {
                    tagSet.add(new Tag(currentTagKey, currentTagValue));
                    currentTagKey = null;
                    currentTagValue = null;
                }
            } else if (in("Tagging", "TagSet", "Tag")) {
                if (name.equals("Key")) {
                    currentTagKey = getText();
                } else if (name.equals("Value")) {
                    currentTagValue = getText();
                }
            }
        }
    }

    public static class GetBucketIntelligentTierConfigurationHandler extends AbstractHandler {

        private final BucketIntelligentTierConfiguration configuration =
                new BucketIntelligentTierConfiguration();

        private BucketIntelligentTierConfiguration.Transition transition;

        public BucketIntelligentTierConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        protected void doStartElement(
                String uri,
                String name,
                String qName,
                Attributes attrs) {

            if (in("IntelligentTieringConfiguration")) {
                if (name.equals("Transition")) {
                    configuration.setTransition(new BucketIntelligentTierConfiguration.Transition());
                }
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("IntelligentTieringConfiguration")) {
                if(name.equals("Status")) {
                    configuration.setStatus(getText());
                }
            } else if(in("IntelligentTieringConfiguration", "Transition")) {
                if (name.equals("Days")) {
                    configuration.getTransition().setDays(Integer.parseInt(getText()));
                }
            }

        }
    }

    public static class ListQueueHandler extends AbstractHandler {
        private MediaListQueueResponse response = new MediaListQueueResponse();
        boolean isNew = true;
        MediaQueueObject queueObject;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("QueueList".equals(name)) {
                isNew = true;
            }
            if (isNew) {
                queueObject = new MediaQueueObject();
                isNew = false;
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    case "TotalCount":
                        response.setTotalCount(getText());
                        break;
                    case "PageNumber":
                        response.setPageNumber(getText());
                        break;
                    case "PageSize":
                        response.setPageSize(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "QueueList")) {
                switch (name) {
                    case "QueueId":
                        queueObject.setQueueId(getText());
                        break;
                    case "Name":
                        queueObject.setName(getText());
                        break;
                    case "State":
                        queueObject.setState(getText());
                        break;
                    case "MaxSize":
                        queueObject.setMaxSize(getText());
                        break;
                    case "MaxConcurrent":
                        queueObject.setMaxConcurrent(getText());
                        break;
                    case "CreateTime":
                        queueObject.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        queueObject.setUpdateTime(getText());
                        break;
                    case "Category":
                        queueObject.setCategory(getText());
                        break;
                    case "BucketId":
                        queueObject.setBucketId(getText());
                        break;
                    case "BucketName":
                        queueObject.setBucketName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "QueueList", "NotifyConfig")) {
                switch (name) {
                    case "Url":
                        queueObject.getNotifyConfig().setUrl(getText());
                        break;
                    case "State":
                        queueObject.getNotifyConfig().setState(getText());
                        break;
                    case "Type":
                        queueObject.getNotifyConfig().setType(getText());
                        break;
                    case "Event":
                        queueObject.getNotifyConfig().setEvent(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "NonExistPIDs")) {
                if ("QueueID".equals(name)) {
                    response.getNonExistPIDs().add(getText());
                }
            }
            if ("QueueList".equals(name) && !isNew) {
                response.getQueueList().add(queueObject);
                queueObject = null;
            }

        }

        public MediaListQueueResponse getResponse() {
            return response;
        }

        public void setResponse(MediaListQueueResponse response) {
            this.response = response;
        }
    }

    public static class DocListQueueHandler extends AbstractHandler {
        private DocListQueueResponse response = new DocListQueueResponse();
        boolean isNew = true;
        MediaQueueObject queueObject;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("QueueList".equals(name)) {
                isNew = true;
            }
            if (isNew) {
                queueObject = new MediaQueueObject();
                isNew = false;
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    case "TotalCount":
                        response.setTotalCount(getText());
                        break;
                    case "PageNumber":
                        response.setPageNumber(getText());
                        break;
                    case "PageSize":
                        response.setPageSize(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "QueueList")) {
                switch (name) {
                    case "QueueId":
                        queueObject.setQueueId(getText());
                        break;
                    case "Name":
                        queueObject.setName(getText());
                        break;
                    case "State":
                        queueObject.setState(getText());
                        break;
                    case "MaxSize":
                        queueObject.setMaxSize(getText());
                        break;
                    case "MaxConcurrent":
                        queueObject.setMaxConcurrent(getText());
                        break;
                    case "CreateTime":
                        queueObject.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        queueObject.setUpdateTime(getText());
                        break;
                    case "Category":
                        queueObject.setCategory(getText());
                        break;
                    case "BucketId":
                        queueObject.setBucketId(getText());
                        break;
                    case "BucketName":
                        queueObject.setBucketName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "QueueList", "NotifyConfig")) {
                switch (name) {
                    case "Url":
                        queueObject.getNotifyConfig().setUrl(getText());
                        break;
                    case "State":
                        queueObject.getNotifyConfig().setState(getText());
                        break;
                    case "Type":
                        queueObject.getNotifyConfig().setType(getText());
                        break;
                    case "Event":
                        queueObject.getNotifyConfig().setEvent(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "NonExistPIDs")) {
                if ("QueueID".equals(name)) {
                    response.getNonExistPIDs().add(getText());
                }
            }
            if ("QueueList".equals(name) && !isNew) {
                response.getQueueList().add(queueObject);
                queueObject = null;
            }

        }

        public DocListQueueResponse getResponse() {
            return response;
        }

        public void setResponse(DocListQueueResponse response) {
            this.response = response;
        }
    }

    public static class ListMediaBucketHandler extends AbstractHandler {
        private MediaBucketResponse response = new MediaBucketResponse();
        boolean isNew = true;
        MediaBucketObject bucketObject;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("MediaBucketList".equals(name)) {
                isNew = true;
            }
            if (isNew) {
                bucketObject = new MediaBucketObject();
                isNew = false;
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    case "TotalCount":
                        response.setTotalCount(getText());
                        break;
                    case "PageNumber":
                        response.setPageNumber(getText());
                        break;
                    case "PageSize":
                        response.setPageSize(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "MediaBucketList")) {
                switch (name) {
                    case "BucketId":
                        bucketObject.setBucketId(getText());
                        break;
                    case "Region":
                        bucketObject.setRegion(getText());
                        break;
                    case "CreateTime":
                        bucketObject.setCreateTime(getText());
                        break;
                    case "Name":
                        bucketObject.setName(getText());
                        break;
                    case "AliasBucketId":
                        bucketObject.setAliasBucketId(getText());
                        break;
                    default:
                        break;
                }
            }
            if ("MediaBucketList".equals(name) && !isNew) {
                response.getMediaBucketList().add(bucketObject);
                bucketObject = null;
            }

        }

        public MediaBucketResponse getResponse() {
            return response;
        }
    }

    public static class WorkflowListHandler extends AbstractHandler {
        private MediaWorkflowListResponse response = new MediaWorkflowListResponse();
        MediaWorkflowObject workflowObject;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("MediaWorkflowList".equals(name)) {
                workflowObject = new MediaWorkflowObject();
            }
            if (in("Response", "MediaWorkflowList", "Topology", "Nodes")) {
                Map<String, MediaWorkflowNode> workflowNodes = workflowObject.getTopology().getMediaWorkflowNodes();
                workflowNodes.put(name, new MediaWorkflowNode());
            }

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    case "TotalCount":
                        response.setTotalCount(getText());
                        break;
                    case "PageNumber":
                        response.setPageNumber(getText());
                        break;
                    case "PageSize":
                        response.setPageSize(getText());
                        break;

                    default:
                        break;
                }
            } else if (in("Response", "MediaWorkflowList")) {
                switch (name) {
                    case "Name":
                        workflowObject.setName(getText());
                        break;
                    case "WorkflowId":
                        workflowObject.setWorkflowId(getText());
                        break;
                    case "State":
                        workflowObject.setState(getText());
                        break;
                    case "CreateTime":
                        workflowObject.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        workflowObject.setUpdateTime(getText());
                        break;
                    case "BucketId":
                        workflowObject.setBucketId(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "MediaWorkflowList", "Topology", "Dependencies")) {
                Map<String, MediaWorkflowDependency> dependencyMap = workflowObject.getTopology().getMediaWorkflowDependency();
                MediaWorkflowDependency dependency = dependencyMap.get(name);
                if (dependency == null) {
                    dependency = new MediaWorkflowDependency();
                }
                dependency.setValue(getText());
                dependencyMap.put(name, dependency);

            }

            Map<String, MediaWorkflowNode> nodesMap = workflowObject.getTopology().getMediaWorkflowNodes();
            for (String key : nodesMap.keySet()) {
                MediaWorkflowNode workflowNode = nodesMap.get(key);
                if (in("Response", "MediaWorkflowList", "Topology", "Nodes", key, "Operation")) {
                    if ("TemplateId".equals(name)) {
                        workflowNode.getOperation().setTemplateId(getText());
                    }
                } else if (in("Response", "MediaWorkflowList", "Topology", "Nodes", key, "Operation", "Output")) {
                    MediaOutputObject output = workflowNode.getOperation().getOutput();
                    switch (name) {
                        case "Bucket":
                            output.setBucket(getText());
                            return;
                        case "Object":
                            output.setObject(getText());
                            return;
                        case "Region":
                            output.setRegion(getText());
                            return;
                        default:
                            return;
                    }
                } else if (in("Response", "MediaWorkflowList", "Topology", "Nodes", key)) {
                    if ("Type".equals(name)) {
                        workflowNode.setType(getText());
                    }
                } else if (in("Response", "MediaWorkflowList", "Topology", "Nodes", key, "Input")) {
                    MediaWorkflowInput input = workflowNode.getInput();
                    switch (name) {
                        case "ObjectPrefix":
                            input.setObjectPrefix(getText());
                            return;
                        case "QueueId":
                            input.setQueueId(getText());
                            return;
                        default:
                            return;
                    }
                }
            }

            if ("MediaWorkflowList".equals(name)) {
                response.getMediaWorkflowList().add(workflowObject);
            }
        }

        public MediaWorkflowListResponse getResponse() {
            return response;
        }

        public void setResponse(MediaWorkflowListResponse response) {
            this.response = response;
        }
    }

    public static class WorkflowHandler extends AbstractHandler {
        private MediaWorkflowResponse response = new MediaWorkflowResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            switch (name) {
                case "Name":
                    response.setName(getText());
                    break;
                case "WorkflowId":
                    response.setWorkflowId(getText());
                    break;
                case "State":
                    response.setState(getText());
                    break;
                case "CreateTime":
                    response.setCreateTime(getText());
                    break;
                case "UpdateTime":
                    response.setUpdateTime(getText());
                    break;
                case "BucketId":
                    response.setBucketId(getText());
                    break;
                default:
                    break;
            }
        }

        public MediaWorkflowResponse getResponse() {
            return response;
        }

        public void setResponse(MediaWorkflowResponse response) {
            this.response = response;
        }
    }


    public static class WorkflowExecutionHandler extends AbstractHandler {
        private MediaWorkflowExecutionResponse response = new MediaWorkflowExecutionResponse();
        MediaWorkflowExecutionObject workflowObject = response.getWorkflowExecution();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("Tasks".equals(name)) {
                workflowObject.getTasks().addLast(new MediaTasks());
            }

            if (in("Response", "WorkflowExecution", "Topology", "Nodes")) {
                Map<String, MediaWorkflowNode> workflowNodes = workflowObject.getTopology().getMediaWorkflowNodes();
                workflowNodes.put(name, new MediaWorkflowNode());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "WorkflowExecution")) {
                switch (name) {
                    case "Object":
                        workflowObject.setObject(getText());
                        break;
                    case "WorkflowId":
                        workflowObject.setWorkflowId(getText());
                        break;
                    case "State":
                        workflowObject.setState(getText());
                        break;
                    case "CreateTime":
                        workflowObject.setCreateTime(getText());
                        break;
                    case "RunId":
                        workflowObject.setRunId(getText());
                        break;
                    case "WorkflowName":
                        workflowObject.setWorkflowName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "WorkflowExecution", "Tasks")) {
                MediaTasks tasks = workflowObject.getTasks().getLast();
                switch (name) {
                    case "Type":
                        tasks.setType(getText());
                        break;
                    case "CreateTime":
                        tasks.setCreateTime(getText());
                        break;
                    case "EndTime":
                        tasks.setEndTime(getText());
                        break;
                    case "State":
                        tasks.setState(getText());
                        break;
                    case "JobId":
                        tasks.setJobId(getText());
                        break;
                    case "Name":
                        tasks.setName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "WorkflowExecution", "Topology", "Dependencies")) {
                Map<String, MediaWorkflowDependency> dependencyMap = workflowObject.getTopology().getMediaWorkflowDependency();
                MediaWorkflowDependency dependency = dependencyMap.get(name);
                if (dependency == null) {
                    dependency = new MediaWorkflowDependency();
                }
                dependency.setValue(getText());
                dependencyMap.put(name, dependency);
            }

            Map<String, MediaWorkflowNode> nodesMap = workflowObject.getTopology().getMediaWorkflowNodes();
            for (String key : nodesMap.keySet()) {
                MediaWorkflowNode workflowNode = nodesMap.get(key);
                if (in("Response", "WorkflowExecution", "Topology", "Nodes", key, "Operation")) {
                    if ("TemplateId".equals(name)) {
                        workflowNode.getOperation().setTemplateId(getText());
                    }
                } else if (in("Response", "WorkflowExecution", "Topology", "Nodes", key, "Operation", "Output")) {
                    MediaOutputObject output = workflowNode.getOperation().getOutput();
                    switch (name) {
                        case "Bucket":
                            output.setBucket(getText());
                            return;
                        case "Object":
                            output.setObject(getText());
                            return;
                        case "Region":
                            output.setRegion(getText());
                            return;
                        default:
                            return;
                    }
                } else if (in("Response", "WorkflowExecution", "Topology", "Nodes", key)) {
                    if ("Type".equals(name)) {
                        workflowNode.setType(getText());
                    }
                } else if (in("Response", "WorkflowExecution", "Topology", "Nodes", key, "Input")) {
                    MediaWorkflowInput input = workflowNode.getInput();
                    switch (name) {
                        case "ObjectPrefix":
                            input.setObjectPrefix(getText());
                            return;
                        case "QueueId":
                            input.setQueueId(getText());
                            return;
                        default:
                            return;
                    }
                }
            }
        }

        public MediaWorkflowExecutionResponse getResponse() {
            return response;
        }

        public void setResponse(MediaWorkflowExecutionResponse response) {
            this.response = response;
        }
    }

    public static class MediaJobCreatHandler extends AbstractHandler {
        MediaJobResponse response = new MediaJobResponse();
        List<MediaConcatFragmentObject> concatFragmentList = response.getJobsDetail().getOperation().getMediaConcatTemplate().getConcatFragmentList();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("ConcatFragment".equals(name)){
                concatFragmentList.add(new MediaConcatFragmentObject());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            MediaJobObject jobsDetail = response.getJobsDetail();
            if (in("Response", "JobsDetail")) {
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    case "BucketName":
                        jobsDetail.setBucketName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                jobsDetail.getInput().setObject(getText());
            } else if (in("Response", "JobsDetail", "Operation")) {
                if ("TemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().setTemplateId(getText());
                } else if ("WatermarkTemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().getWatermarkTemplateId().add(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Format")) {
                MediaFormat format = jobsDetail.getOperation().getMediaInfo().getFormat();
                ParserMediaInfoUtils.ParsingMediaFormat(format, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Audio")) {
                MediaInfoAudio audio = jobsDetail.getOperation().getMediaInfo().getStream().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Subtitle")) {
                MediaInfoSubtitle subtitle = jobsDetail.getOperation().getMediaInfo().getStream().getSubtitle();
                ParserMediaInfoUtils.ParsingSubtitle(subtitle, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Video")) {
                MediaInfoVideo video = jobsDetail.getOperation().getMediaInfo().getStream().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Watermark")) {
                MediaWatermark watermark = jobsDetail.getOperation().getWatermark();
                ParserMediaInfoUtils.ParsingWatermark(watermark, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "RemoveWatermark")) {
                MediaRemoveWaterMark removeWatermark = jobsDetail.getOperation().getRemoveWatermark();
                ParserMediaInfoUtils.ParsingRemoveWatermark(removeWatermark, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Watermark", "Text")) {
                MediaWaterMarkText text = jobsDetail.getOperation().getWatermark().getText();
                ParserMediaInfoUtils.ParsingWatermarkText(text, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Watermark", "Image")) {
                MediaWaterMarkImage image = jobsDetail.getOperation().getWatermark().getImage();
                ParserMediaInfoUtils.ParsingWatermarkImage(image, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Container")) {
                if ("Format".equalsIgnoreCase(name))
                    response.getJobsDetail().getOperation().getTranscode().getContainer().setFormat(getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Video")) {
                MediaAudioObject audio = jobsDetail.getOperation().getTranscode().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Audio")) {
                MediaTranscodeVideoObject video = jobsDetail.getOperation().getTranscode().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TransConfig")) {
                MediaTransConfigObject transConfig = jobsDetail.getOperation().getTranscode().getTransConfig();
                ParserMediaInfoUtils.ParsingTransConfig(transConfig, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TimeInterval")) {
                MediaTimeIntervalObject timeInterval = jobsDetail.getOperation().getTranscode().getTimeInterval();
                ParserMediaInfoUtils.ParsingMediaTimeInterval(timeInterval, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = jobsDetail.getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                }
            }
            MediaConcatTemplateObject mediaConcatTemplate = response.getJobsDetail().getOperation().getMediaConcatTemplate();
            if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "ConcatFragment")) {
                MediaConcatFragmentObject mediaConcatFragmentObject = concatFragmentList.get(concatFragmentList.size() - 1);
                switch (name) {
                    case "Mode":
                        mediaConcatFragmentObject.setMode(getText());
                        break;
                    case "Url":
                        mediaConcatFragmentObject.setUrl(getText());
                        break;
                    case "StartTime":
                        mediaConcatFragmentObject.setStartTime(getText());
                        break;
                    case "EndTime":
                        mediaConcatFragmentObject.setEndTime(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Audio")) {
                MediaAudioObject audio = mediaConcatTemplate.getAudio();
                ParserMediaInfoUtils.ParsingMediaAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Video")) {
                MediaVideoObject video = mediaConcatTemplate.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Container")) {
                MediaContainerObject container = mediaConcatTemplate.getContainer();
                if ("Format".equals(name)) {
                    container.setFormat(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate")){
                if ("Index".equals(name)) {
                    mediaConcatTemplate.setIndex(getText());
                }
            }
        }

        public MediaJobResponse getResponse() {
            return response;
        }
    }

    public static class DescribeMediaJobHandler extends AbstractHandler {
        MediaJobResponse response = new MediaJobResponse();
        List<MediaConcatFragmentObject> concatFragmentList = response.getJobsDetail().getOperation().getMediaConcatTemplate().getConcatFragmentList();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("ConcatFragment".equals(name)){
                concatFragmentList.add(new MediaConcatFragmentObject());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            MediaJobObject jobsDetail = response.getJobsDetail();
            if (in("Response", "JobsDetail")) {

                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    case "BucketName":
                        jobsDetail.setBucketName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                jobsDetail.getInput().setObject(getText());
            } else if (in("Response", "JobsDetail", "Operation")) {
                if ("TemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().setTemplateId(getText());
                } else if ("WatermarkTemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().getWatermarkTemplateId().add(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Format")) {
                MediaFormat format = jobsDetail.getOperation().getMediaInfo().getFormat();
                ParserMediaInfoUtils.ParsingMediaFormat(format, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Audio")) {
                MediaInfoAudio audio = jobsDetail.getOperation().getMediaInfo().getStream().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Subtitle")) {
                MediaInfoSubtitle subtitle = jobsDetail.getOperation().getMediaInfo().getStream().getSubtitle();
                ParserMediaInfoUtils.ParsingSubtitle(subtitle, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Video")) {
                MediaInfoVideo video = jobsDetail.getOperation().getMediaInfo().getStream().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "RemoveWatermark")) {
                MediaRemoveWaterMark removeWatermark = jobsDetail.getOperation().getRemoveWatermark();
                ParserMediaInfoUtils.ParsingRemoveWatermark(removeWatermark, name, getText());
            }else if (in("Response", "JobsDetail", "Operation", "Transcode", "Container")) {
                if ("Format".equalsIgnoreCase(name))
                    jobsDetail.getOperation().getTranscode().getContainer().setFormat(getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Video")) {
                MediaAudioObject audio = jobsDetail.getOperation().getTranscode().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Audio")) {
                MediaTranscodeVideoObject video = jobsDetail.getOperation().getTranscode().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TransConfig")) {
                MediaTransConfigObject transConfig = jobsDetail.getOperation().getTranscode().getTransConfig();
                ParserMediaInfoUtils.ParsingTransConfig(transConfig, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TimeInterval")) {
                MediaTimeIntervalObject timeInterval = jobsDetail.getOperation().getTranscode().getTimeInterval();
                ParserMediaInfoUtils.ParsingMediaTimeInterval(timeInterval, name, getText());
            }
            else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = jobsDetail.getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                }
            }
            MediaConcatTemplateObject mediaConcatTemplate = response.getJobsDetail().getOperation().getMediaConcatTemplate();
            if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "ConcatFragment")) {
                MediaConcatFragmentObject mediaConcatFragmentObject = concatFragmentList.get(concatFragmentList.size() - 1);
                switch (name) {
                    case "Mode":
                        mediaConcatFragmentObject.setMode(getText());
                        break;
                    case "Url":
                        mediaConcatFragmentObject.setUrl(getText());
                        break;
                    case "StartTime":
                        mediaConcatFragmentObject.setStartTime(getText());
                        break;
                    case "EndTime":
                        mediaConcatFragmentObject.setEndTime(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Audio")) {
                MediaAudioObject audio = mediaConcatTemplate.getAudio();
                ParserMediaInfoUtils.ParsingMediaAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Video")) {
                MediaVideoObject video = mediaConcatTemplate.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Container")) {
                MediaContainerObject container = mediaConcatTemplate.getContainer();
                if ("Format".equals(name)) {
                    container.setFormat(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate")) {
                if ("Index".equals(name)) {
                    mediaConcatTemplate.setIndex(getText());
                }
            }
        }

        public MediaJobResponse getResponse() {
            return response;
        }
    }

    public static class DescribeMediaJobsHandler extends AbstractHandler {
        MediaListJobResponse response = new MediaListJobResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<MediaJobObject> jobsDetailList = response.getJobsDetailList();
            if ("JobsDetail".equalsIgnoreCase(name)) {
                List<MediaJobObject> jobsDetail = jobsDetailList;
                jobsDetail.add(new MediaJobObject());
            }

            if ("ConcatFragment".equals(name)){
                List<MediaConcatFragmentObject> concatFragmentList = jobsDetailList.get(jobsDetailList.size()-1).getOperation().getMediaConcatTemplate().getConcatFragmentList();
                concatFragmentList.add(new MediaConcatFragmentObject());
            }

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<MediaJobObject> jobsDetailList = response.getJobsDetailList();
            MediaJobObject jobsDetail = jobsDetailList.get(jobsDetailList.size() - 1);
            if (in("Response", "JobsDetail")) {

                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    case "BucketName":
                        jobsDetail.setBucketName(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                jobsDetail.getInput().setObject(getText());
            } else if (in("Response", "JobsDetail", "Operation")) {
                if ("TemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().setTemplateId(getText());
                } else if ("WatermarkTemplateId".equalsIgnoreCase(name)) {
                    jobsDetail.getOperation().getWatermarkTemplateId().add(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Format")) {
                MediaFormat format = jobsDetail.getOperation().getMediaInfo().getFormat();
                ParserMediaInfoUtils.ParsingMediaFormat(format, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Audio")) {
                MediaInfoAudio audio = jobsDetail.getOperation().getMediaInfo().getStream().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Subtitle")) {
                MediaInfoSubtitle subtitle = jobsDetail.getOperation().getMediaInfo().getStream().getSubtitle();
                ParserMediaInfoUtils.ParsingSubtitle(subtitle, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "MediaInfo", "Stream", "Video")) {
                MediaInfoVideo video = jobsDetail.getOperation().getMediaInfo().getStream().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "RemoveWatermark")) {
                MediaRemoveWaterMark removeWatermark = jobsDetail.getOperation().getRemoveWatermark();
                ParserMediaInfoUtils.ParsingRemoveWatermark(removeWatermark, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Container")) {
                if ("Format".equalsIgnoreCase(name))
                    jobsDetail.getOperation().getTranscode().getContainer().setFormat(getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Video")) {
                MediaAudioObject audio = jobsDetail.getOperation().getTranscode().getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "Audio")) {
                MediaTranscodeVideoObject video = jobsDetail.getOperation().getTranscode().getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TransConfig")) {
                MediaTransConfigObject transConfig = jobsDetail.getOperation().getTranscode().getTransConfig();
                ParserMediaInfoUtils.ParsingTransConfig(transConfig, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Transcode", "TimeInterval")) {
                MediaTimeIntervalObject timeInterval = jobsDetail.getOperation().getTranscode().getTimeInterval();
                ParserMediaInfoUtils.ParsingMediaTimeInterval(timeInterval, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = jobsDetail.getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                }
            }
            MediaConcatTemplateObject mediaConcatTemplate = jobsDetail.getOperation().getMediaConcatTemplate();
            if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "ConcatFragment")) {
                List<MediaConcatFragmentObject> concatFragmentList = mediaConcatTemplate.getConcatFragmentList();
                MediaConcatFragmentObject mediaConcatFragmentObject = concatFragmentList.get(concatFragmentList.size() - 1);
                switch (name) {
                    case "Mode":
                        mediaConcatFragmentObject.setMode(getText());
                        break;
                    case "Url":
                        mediaConcatFragmentObject.setUrl(getText());
                        break;
                    case "StartTime":
                        mediaConcatFragmentObject.setStartTime(getText());
                        break;
                    case "EndTime":
                        mediaConcatFragmentObject.setEndTime(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Audio")) {
                MediaAudioObject audio = mediaConcatTemplate.getAudio();
                ParserMediaInfoUtils.ParsingMediaAudio(audio, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Video")) {
                MediaVideoObject video = mediaConcatTemplate.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate", "Container")) {
                MediaContainerObject container = mediaConcatTemplate.getContainer();
                if ("Format".equals(name)) {
                    container.setFormat(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "ConcatTemplate")) {
                if ("Index".equals(name)) {
                    mediaConcatTemplate.setIndex(getText());
                }
            }
        }

        public MediaListJobResponse getResponse() {
            return response;
        }
    }

    public static class MediaQueueResponseHandler extends AbstractHandler {
        MediaQueueResponse response = new MediaQueueResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            } else if (in("Response", "Queue")) {
                MediaQueueObject queue = response.getQueue();
                switch (name) {
                    case "QueueId":
                        queue.setQueueId(getText());
                        break;
                    case "Name":
                        queue.setName(getText());
                        break;
                    case "State":
                        queue.setState(getText());
                        break;
                    case "MaxSize":
                        queue.setMaxSize(getText());
                        break;
                    case "MaxConcurrent":
                        queue.setMaxConcurrent(getText());
                        break;
                    case "CreateTime":
                        queue.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        queue.setUpdateTime(getText());
                        break;
                    case "BucketId":
                        queue.setBucketId(getText());
                        break;
                    case "Category":
                        queue.setCategory(getText());
                        break;
                }
            } else if (in("Response", "Queue", "NotifyConfig")) {
                MediaNotifyConfig notifyConfig = response.getQueue().getNotifyConfig();
                switch (name) {
                    case "Url":
                        notifyConfig.setUrl(getText());
                        break;
                    case "Event":
                        notifyConfig.setEvent(getText());
                        break;
                    case "Type":
                        notifyConfig.setType(getText());
                        break;
                    case "State":
                        notifyConfig.setState(getText());
                        break;
                }
            }

        }

        public MediaQueueResponse getResponse() {
            return response;
        }
    }

    public static class MediaTemplateHandler extends AbstractHandler {
        MediaTemplateResponse response = new MediaTemplateResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            } else if (in("Response", "Template")) {
                MediaTemplateObject template = response.getTemplate();
                switch (name) {
                    case "TemplateId":
                        template.setTemplateId(getText());
                        break;
                    case "Name":
                        template.setName(getText());
                        break;
                    case "Tag":
                        template.setTag(getText());
                        break;
                    case "State":
                        template.setState(getText());
                        break;
                    case "CreateTime":
                        template.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        template.setUpdateTime(getText());
                        break;
                    case "BucketId":
                        template.setBucketId(getText());
                        break;
                    case "Category":
                        template.setCategory(getText());
                }
            }
            MediaTemplateTransTplObject transTpl = response.getTemplate().getTransTpl();

            if (in("Response", "Template", "TransTpl", "Container")) {
                MediaContainerObject container = transTpl.getContainer();
                if ("Format".equalsIgnoreCase(name)) {
                    container.setFormat(getText());
                }
            } else if (in("Response", "Template", "TransTpl", "Video")) {
                MediaVideoObject video = transTpl.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "Template", "TransTpl", "TimeInterval")) {
                MediaTimeIntervalObject timeInterval = transTpl.getTimeInterval();
                ParserMediaInfoUtils.ParsingMediaTimeInterval(timeInterval, name, getText());
            } else if (in("Response", "Template", "TransTpl", "Audio")) {
                MediaAudioObject audio = transTpl.getAudio();
                ParserMediaInfoUtils.ParsingMediaAudio(audio, name, getText());
            } else if (in("Response", "Template", "TransTpl", "TransConfig")) {
                MediaTransConfigObject transConfig = transTpl.getTransConfig();
                ParserMediaInfoUtils.ParsingTransConfig(transConfig, name, getText());
            } else if (in("Response", "Template", "Snapshot")) {
                MediaSnapshotObject snapshot = response.getTemplate().getSnapshot();
                ParserMediaInfoUtils.ParsingSnapshot(snapshot, name, getText());
            } else if (in("Response", "Template", "Watermark")) {
                MediaWatermark watermark = response.getTemplate().getWatermark();
                ParserMediaInfoUtils.ParsingWatermark(watermark, name, getText());
            } else if (in("Response", "Template", "Watermark", "Text")) {
                MediaWaterMarkText text = response.getTemplate().getWatermark().getText();
                ParserMediaInfoUtils.ParsingWatermarkText(text, name, getText());
            } else if (in("Response", "Template", "Watermark", "Image")) {
                MediaWaterMarkImage image = response.getTemplate().getWatermark().getImage();
                ParserMediaInfoUtils.ParsingWatermarkImage(image, name, getText());
            }
        }

        public MediaTemplateResponse getResponse() {
            return response;
        }
    }

    public static class SnapshotHandler extends AbstractHandler {
        SnapshotResponse response = new SnapshotResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "Output")) {
                MediaOutputObject output = response.getOutput();
                if ("Bucket".equalsIgnoreCase(name)) {
                    output.setBucket(getText());
                } else if ("Object".equalsIgnoreCase(name)) {
                    output.setObject(getText());
                } else if ("Region".equalsIgnoreCase(name)) {
                    output.setRegion(getText());
                }
            }
        }

        public SnapshotResponse getResponse() {
            return response;
        }
    }

    public static class MediaTemplatesHandler extends AbstractHandler {
        MediaListTemplateResponse response = new MediaListTemplateResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("TemplateList".equalsIgnoreCase(name)) {
                List<MediaTemplateObject> templateList = response.getTemplateList();
                templateList.add(new MediaTemplateObject());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
                if ("TotalCount".equalsIgnoreCase(name)) {
                    response.setTotalCount(getText());
                }
                if ("PageNumber".equalsIgnoreCase(name)) {
                    response.setPageNumber(getText());
                }
                if ("PageSize".equalsIgnoreCase(name)) {
                    response.setPageSize(getText());
                }
            }
            List<MediaTemplateObject> templateList = response.getTemplateList();
            MediaTemplateObject template;
            if (templateList.size() != 0) {
                template = templateList.get(templateList.size() - 1);
            } else {
                template = new MediaTemplateObject();
            }
            if (in("Response", "TemplateList")) {
                switch (name) {
                    case "TemplateId":
                        template.setTemplateId(getText());
                        break;
                    case "Name":
                        template.setName(getText());
                        break;
                    case "Tag":
                        template.setTag(getText());
                        break;
                    case "State":
                        template.setState(getText());
                        break;
                    case "CreateTime":
                        template.setCreateTime(getText());
                        break;
                    case "UpdateTime":
                        template.setUpdateTime(getText());
                        break;
                    case "BucketId":
                        template.setBucketId(getText());
                        break;
                    case "Category":
                        template.setCategory(getText());
                }
            }

            MediaTemplateTransTplObject transTpl = template.getTransTpl();

            if (in("Response", "TemplateList", "TransTpl", "Container")) {
                MediaContainerObject container = transTpl.getContainer();
                if ("Format".equalsIgnoreCase(name)) {
                    container.setFormat(getText());
                }
            } else if (in("Response", "TemplateList", "TransTpl", "Video")) {
                MediaVideoObject video = transTpl.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            } else if (in("Response", "TemplateList", "TransTpl", "TimeInterval")) {
                MediaTimeIntervalObject timeInterval = transTpl.getTimeInterval();
                ParserMediaInfoUtils.ParsingMediaTimeInterval(timeInterval, name, getText());
            } else if (in("Response", "TemplateList", "TransTpl", "Audio")) {
                MediaAudioObject audio = transTpl.getAudio();
                ParserMediaInfoUtils.ParsingMediaAudio(audio, name, getText());
            } else if (in("Response", "TemplateList", "TransTpl", "TransConfig")) {
                MediaTransConfigObject transConfig = transTpl.getTransConfig();
                ParserMediaInfoUtils.ParsingTransConfig(transConfig, name, getText());
            } else if (in("Response", "TemplateList", "Snapshot")) {
                MediaSnapshotObject snapshot = template.getSnapshot();
                ParserMediaInfoUtils.ParsingSnapshot(snapshot, name, getText());
            } else if (in("Response", "TemplateList", "Watermark")) {
                MediaWatermark watermark = template.getWatermark();
                ParserMediaInfoUtils.ParsingWatermark(watermark, name, getText());
            } else if (in("Response", "TemplateList", "Watermark", "Text")) {
                MediaWaterMarkText text = template.getWatermark().getText();
                ParserMediaInfoUtils.ParsingWatermarkText(text, name, getText());
            } else if (in("Response", "TemplateList", "Watermark", "Image")) {
                MediaWaterMarkImage image = template.getWatermark().getImage();
                ParserMediaInfoUtils.ParsingWatermarkImage(image, name, getText());
            }
        }

        public MediaListTemplateResponse getResponse() {
            return response;
        }
    }

    public static class GenerateMediainfoHandler extends AbstractHandler {
        MediaInfoResponse response = new MediaInfoResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            MediaStream stream = response.getMediaInfo().getStream();
            if (in("Response", "MediaInfo", "Format")) {
                MediaFormat format = response.getMediaInfo().getFormat();
                ParserMediaInfoUtils.ParsingMediaFormat(format, name, getText());
            } else if (in("Response", "MediaInfo", "Stream", "Audio")) {
                MediaInfoAudio audio = stream.getAudio();
                ParserMediaInfoUtils.ParsingStreamAudio(audio, name, getText());
            } else if (in("Response", "MediaInfo", "Stream", "Subtitle")) {
                MediaInfoSubtitle subtitle = stream.getSubtitle();
                ParserMediaInfoUtils.ParsingSubtitle(subtitle, name, getText());
            } else if (in("Response", "MediaInfo", "Stream", "Video")) {
                MediaInfoVideo video = stream.getVideo();
                ParserMediaInfoUtils.ParsingMediaVideo(video, name, getText());
            }
        }

        public MediaInfoResponse getResponse() {
            return response;
        }
    }

    public static class WorkflowExecutionsHandler extends AbstractHandler {
        MediaWorkflowExecutionsResponse response = new MediaWorkflowExecutionsResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("WorkflowExecutionList".equalsIgnoreCase(name)) {
                List<MediaWorkflowExecutionObject> list = response.getWorkflowExecutionList();
                list.add(new MediaWorkflowExecutionObject());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                } else if ("NextToken".equalsIgnoreCase(name)) {
                    response.setNextToken(getText());
                }
            } else if (in("Response", "WorkflowExecutionList")) {
                List<MediaWorkflowExecutionObject> list = response.getWorkflowExecutionList();
                MediaWorkflowExecutionObject mediaWorkflowExecutionObject;
                if (list.size() != 0) {
                    mediaWorkflowExecutionObject = list.get(list.size() - 1);
                } else {
                    mediaWorkflowExecutionObject = new MediaWorkflowExecutionObject();
                }
                switch (name) {
                    case "RunId":
                        mediaWorkflowExecutionObject.setRunId(getText());
                        break;
                    case "WorkflowId":
                        mediaWorkflowExecutionObject.setWorkflowId(getText());
                        break;
                    case "Object":
                        mediaWorkflowExecutionObject.setObject(getText());
                        break;
                    case "CreateTime":
                        mediaWorkflowExecutionObject.setCreateTime(getText());
                        break;
                    case "State":
                        mediaWorkflowExecutionObject.setState(getText());
                        break;
                }
            }
        }

        public MediaWorkflowExecutionsResponse getResponse() {
            return response;
        }
    }

    public static class DocJobHandler extends AbstractHandler {
        DocJobResponse response = new DocJobResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                DocJobDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                if ("Object".equalsIgnoreCase(name)) {
                    response.getJobsDetail().getInput().setObject(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = response.getJobsDetail().getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Operation", "DocProcess")) {
                DocProcessObject docProcess = response.getJobsDetail().getOperation().getDocProcessObject();
                switch (name) {
                    case "Comments":
                        docProcess.setComments(getText());
                        break;
                    case "DocPassword":
                        docProcess.setDocPassword(getText());
                        break;
                    case "EndPage":
                        docProcess.setEndPage(getText());
                        break;
                    case "ImageParams":
                        docProcess.setImageParams(getText());
                        break;
                    case "PaperDirection":
                        docProcess.setPaperDirection(getText());
                        break;
                    case "Quality":
                        docProcess.setQuality(getText());
                        break;
                    case "SrcType":
                        docProcess.setSrcType(getText());
                        break;
                    case "StartPage":
                        docProcess.setStartPage(getText());
                        break;
                    case "TgtType":
                        docProcess.setTgtType(getText());
                        break;
                    case "Zoom":
                        docProcess.setZoom(getText());
                        break;
                    case "SheetId":
                        docProcess.setSheetId(getText());
                        break;
                    default:
                        break;
                }
            }
        }

        public DocJobResponse getResponse() {
            return response;
        }
    }

    public static class DescribeDocProcessJobHandler extends AbstractHandler {
        DocJobResponse response = new DocJobResponse();
        DocProcessPageInfo pageInfo = new DocProcessPageInfo();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("PageInfo".equalsIgnoreCase(name)) {
                pageInfo = new DocProcessPageInfo();
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if(in( "Response")) {
                if(name.equals("NonExistJobIds")) {
                    response.setNonExistJobIds(getText());
                }
            } else if (in("Response", "JobsDetail")) {
                DocJobDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                if ("Object".equalsIgnoreCase(name)) {
                    response.getJobsDetail().getInput().setObject(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "DocProcess")) {
                DocProcessObject docProcess = response.getJobsDetail().getOperation().getDocProcessObject();
                switch (name) {
                    case "Comments":
                        docProcess.setComments(getText());
                        break;
                    case "DocPassword":
                        docProcess.setDocPassword(getText());
                        break;
                    case "EndPage":
                        docProcess.setEndPage(getText());
                        break;
                    case "ImageParams":
                        docProcess.setImageParams(getText());
                        break;
                    case "PaperDirection":
                        docProcess.setPaperDirection(getText());
                        break;
                    case "Quality":
                        docProcess.setQuality(getText());
                        break;
                    case "SrcType":
                        docProcess.setSrcType(getText());
                        break;
                    case "StartPage":
                        docProcess.setStartPage(getText());
                        break;
                    case "TgtType":
                        docProcess.setTgtType(getText());
                        break;
                    case "Zoom":
                        docProcess.setZoom(getText());
                        break;
                    case "SheetId":
                        docProcess.setSheetId(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "DocProcessResult")) {
                DocProcessResult docProcessResult = response.getJobsDetail().getOperation().getDocProcessResult();
                switch (name) {
                    case "FailPageCount":
                        docProcessResult.setFailPageCount(getText());
                        break;
                    case "SuccPageCount":
                        docProcessResult.setSuccPageCount(getText());
                        break;
                    case "TgtType":
                        docProcessResult.setTgtType(getText());
                        break;
                    case "TotalPageCount":
                        docProcessResult.setTotalPageCount(getText());
                        break;
                    case "TotalSheetCount":
                        docProcessResult.setTotalSheetCount(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = response.getJobsDetail().getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Operation", "DocProcessResult", "PageInfo")) {
                switch (name) {
                    case "PageNo":
                        pageInfo.setPageNo(getText());
                        break;
                    case "PicIndex":
                        pageInfo.setPicIndex(getText());
                        break;
                    case "PicNum":
                        pageInfo.setPicNum(getText());
                        break;
                    case "TgtUri":
                        pageInfo.setTgtUri(getText());
                        break;
                    case "X-SheetPics":
                        pageInfo.setxSheetPics(getText());
                        break;
                    default:
                        break;
                }

            }

            if ("PageInfo".equalsIgnoreCase(name)) {
                List<DocProcessPageInfo> pageInfoList = response.getJobsDetail().getOperation().getDocProcessResult().getDocProcessPageInfoList();
                pageInfoList.add(pageInfo);
            }
        }

        public DocJobResponse getResponse() {
            return response;
        }
    }

    public static class DescribeDocProcessJobsHandler extends AbstractHandler {
        DocJobListResponse response = new DocJobListResponse();
        DocJobDetail jobsDetail;
        DocProcessPageInfo pageInfo;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("PageInfo".equalsIgnoreCase(name)) {
                pageInfo = new DocProcessPageInfo();
            } else if ("JobsDetail".equalsIgnoreCase(name)) {
                jobsDetail = new DocJobDetail();
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if(in("Response", "NextToken")) {
                response.setNextToken(getText());
            } else if (in("Response", "JobsDetail")) {
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "EndTime":
                        jobsDetail.setEndTime(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "QueueId":
                        jobsDetail.setQueueId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "Tag":
                        jobsDetail.setTag(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Input")) {
                if ("Object".equalsIgnoreCase(name)) {
                    jobsDetail.getInput().setObject(getText());
                }
            } else if (in("Response", "JobsDetail", "Operation", "DocProcess")) {
                DocProcessObject docProcess = jobsDetail.getOperation().getDocProcessObject();
                switch (name) {
                    case "Comments":
                        docProcess.setComments(getText());
                        break;
                    case "DocPassword":
                        docProcess.setDocPassword(getText());
                        break;
                    case "EndPage":
                        docProcess.setEndPage(getText());
                        break;
                    case "ImageParams":
                        docProcess.setImageParams(getText());
                        break;
                    case "PaperDirection":
                        docProcess.setPaperDirection(getText());
                        break;
                    case "Quality":
                        docProcess.setQuality(getText());
                        break;
                    case "SrcType":
                        docProcess.setSrcType(getText());
                        break;
                    case "StartPage":
                        docProcess.setStartPage(getText());
                        break;
                    case "TgtType":
                        docProcess.setTgtType(getText());
                        break;
                    case "Zoom":
                        docProcess.setZoom(getText());
                        break;
                    case "SheetId":
                        docProcess.setSheetId(getText());
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "DocProcessResult")) {
                DocProcessResult docProcessResult = jobsDetail.getOperation().getDocProcessResult();
                switch (name) {
                    case "FailPageCount":
                        docProcessResult.setFailPageCount(getText());
                        break;
                    case "SuccPageCount":
                        docProcessResult.setSuccPageCount(getText());
                        break;
                    case "TgtType":
                        docProcessResult.setTgtType(getText());
                        break;
                    case "TotalPageCount":
                        docProcessResult.setTotalPageCount(getText());
                        break;
                    case "TotalSheetCount":
                        docProcessResult.setTotalSheetCount(getText());
                        break;
                    default:
                        break;
                }

            } else if (in("Response", "JobsDetail", "Operation", "Output")) {
                MediaOutputObject output = jobsDetail.getOperation().getOutput();
                switch (name) {
                    case "Bucket":
                        output.setBucket(getText());
                        break;
                    case "Object":
                        output.setObject(getText());
                        break;
                    case "Region":
                        output.setRegion(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Operation", "DocProcessResult", "PageInfo")) {
                switch (name) {
                    case "PageNo":
                        pageInfo.setPageNo(getText());
                        break;
                    case "PicIndex":
                        pageInfo.setPicIndex(getText());
                        break;
                    case "PicNum":
                        pageInfo.setPicNum(getText());
                        break;
                    case "TgtUri":
                        pageInfo.setTgtUri(getText());
                        break;
                    case "X-SheetPics":
                        pageInfo.setxSheetPics(getText());
                        break;
                    default:
                        break;
                }

            }

            if ("PageInfo".equalsIgnoreCase(name)) {
                List<DocProcessPageInfo> pageInfoList = jobsDetail.getOperation().getDocProcessResult().getDocProcessPageInfoList();
                pageInfoList.add(pageInfo);
            } else if ("JobsDetail".equalsIgnoreCase(name)) {
                response.getDocJobDetailList().add(jobsDetail);
            }
        }

        public DocJobListResponse getResponse() {
            return response;
        }
    }

    public static class ListDocBucketHandler extends AbstractHandler {
        private DocBucketResponse response = new DocBucketResponse();
        boolean isNew = true;
        DocBucketObject bucketObject;

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

            if ("DocBucketList".equals(name)) {
                isNew = true;
            }
            if (isNew) {
                bucketObject = new DocBucketObject();
                isNew = false;
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {

            if (in("Response")) {
                switch (name) {
                    case "RequestId":
                        response.setRequestId(getText());
                        break;
                    case "TotalCount":
                        response.setTotalCount(getText());
                        break;
                    case "PageNumber":
                        response.setPageNumber(getText());
                        break;
                    case "PageSize":
                        response.setPageSize(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "DocBucketList")) {
                switch (name) {
                    case "BucketId":
                        bucketObject.setBucketId(getText());
                        break;
                    case "Region":
                        bucketObject.setRegion(getText());
                        break;
                    case "CreateTime":
                        bucketObject.setCreateTime(getText());
                        break;
                    case "AliasBucketId":
                        bucketObject.setAliasBucketId(getText());
                        break;
                    case "Name":
                        bucketObject.setName(getText());
                        break;
                    default:
                        break;
                }
            }
            if ("DocBucketList".equals(name) && !isNew) {
                response.getDocBucketObjectList().add(bucketObject);
                bucketObject = null;
            }
        }

        public DocBucketResponse getResponse() {
            return response;
        }
    }

    public static class ImageAuditingHandler extends AbstractHandler {
        private ImageAuditingResponse response = new ImageAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("RecognitionResult", "PornInfo")) {
                parseInfo(response.getPornInfo(), name, getText());
            } else if (in("RecognitionResult", "PoliticsInfo")) {
                parseInfo(response.getPoliticsInfo(), name, getText());
            } else if (in("RecognitionResult", "TerroristInfo")) {
                parseInfo(response.getTerroristInfo(), name, getText());
            } else if (in("RecognitionResult", "AdsInfo")) {
                parseInfo(response.getAdsInfo(), name, getText());
            }
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "Msg":
                    obj.setMsg(getText());
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Label":
                    obj.setLabel(getText());
                    break;
                default:
                    break;
            }
        }

        public ImageAuditingResponse getResponse() {
            return response;
        }
    }

    public static class DescribeVideoAuditingJobHandler extends AbstractHandler {

        private VideoAuditingResponse response = new VideoAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<SnapshotInfo> snapshotList = response.getJobsDetail().getSnapshotList();
            if (in("Response", "JobsDetail") && "Snapshot".equals(name)) {
                snapshotList.add(new SnapshotInfo());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<SnapshotInfo> snapshotList = response.getJobsDetail().getSnapshotList();
            if (in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "SnapshotCount":
                        jobsDetail.setSnapshotCount(getText());
                        break;
                    case "Result":
                        jobsDetail.setResult(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Label":
                        jobsDetail.setLabel(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "PornInfo")) {
                parseInfo(response.getJobsDetail().getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PoliticsInfo")) {
                parseInfo(response.getJobsDetail().getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TerrorismInfo")) {
                parseInfo(response.getJobsDetail().getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "AdsInfo")) {
                parseInfo(response.getJobsDetail().getAdsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Snapshot")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                if ("Url".equals(name))
                    snapshotInfo.setUrl(URLDecoder.decode(getText()));
            } else if (in("Response", "JobsDetail", "Snapshot", "PornInfo")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                parseInfo(snapshotInfo.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Snapshot", "PoliticsInfo")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                parseInfo(snapshotInfo.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Snapshot", "TerrorismInfo")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                parseInfo(snapshotInfo.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Snapshot", "AdsInfo")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                parseInfo(snapshotInfo.getAdsInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "Snapshot")) {
                SnapshotInfo snapshotInfo = snapshotList.get(snapshotList.size() - 1);
                if ("Text".equalsIgnoreCase(name)){
                    snapshotInfo.setText(getText());
                }else if ("Url".equalsIgnoreCase(name)){
                    snapshotInfo.setUrl(getText());
                }
            }
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "Msg":
                    obj.setMsg(getText());
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Label":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                default:
                    break;
            }
        }

        public VideoAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(VideoAuditingResponse response) {
            this.response = response;
        }
    }

    public static class CreateVideoAuditingJobHandler extends AbstractHandler {
        private VideoAuditingResponse response = new VideoAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            }
        }

        public VideoAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(VideoAuditingResponse response) {
            this.response = response;
        }
    }

    public static class DescribeAudioAuditingJobHandler extends AbstractHandler {
        private AudioAuditingResponse response = new AudioAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
            if (in("Response", "JobsDetail") && "Section".equals(name)) {
                sectionList.add(new SectionInfo());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "Result":
                        jobsDetail.setResult(getText());
                        break;
                    case "AudioText":
                        jobsDetail.setAudioText(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Label":
                        jobsDetail.setLabel(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "PornInfo")) {
                parseInfo(response.getJobsDetail().getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PoliticsInfo")) {
                parseInfo(response.getJobsDetail().getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TerrorismInfo")) {
                parseInfo(response.getJobsDetail().getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "AdsInfo")) {
                parseInfo(response.getJobsDetail().getAdsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "PornInfo")) {
                List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "PoliticsInfo")) {
                List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "TerrorismInfo")) {
                List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "AdsInfo")) {
                List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getAdsInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "Section")){
                List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                switch (name){
                    case "Text":
                        sectionInfo.setText(getText());
                        break;
                    case "Url":
                        sectionInfo.setUrl(getText());
                        break;
                    case "Duration":
                        sectionInfo.setDuration(getText());
                        break;
                    case "OffsetTime":
                        sectionInfo.setOffsetTime(getText());
                    default:
                        break;
                }
            }
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "Msg":
                    obj.setMsg(getText());
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Label":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                default:
                    break;
            }
        }

        public AudioAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(AudioAuditingResponse response) {
            this.response = response;
        }
    }

    public static class CreateAudioAuditingJobHandler extends AbstractHandler {
        private AudioAuditingResponse response = new AudioAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            }
        }

        public AudioAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(AudioAuditingResponse response) {
            this.response = response;
        }
    }

    public static class ImageLabelHandler extends AbstractHandler {
        private ImageLabelResponse response = new ImageLabelResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if ("Labels".equalsIgnoreCase(name)) {
                response.getRecognitionResult().add(new Lobel());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<Lobel> list = response.getRecognitionResult();
            Lobel lobel = list.get(list.size() - 1);
            if (in("RecognitionResult", "Labels")) {
                switch (name) {
                    case "Confidence":
                        lobel.setConfidence(getText());
                        break;
                    case "Name":
                        lobel.setName(getText());
                        break;
                    default:
                        break;
                }
            }
        }

        public ImageLabelResponse getResponse() {
            return response;
        }

        public void setResponse(ImageLabelResponse response) {
            this.response = response;
        }
    }

    public static class ImageLabelV2Handler extends AbstractHandler {
        private ImageLabelV2Response response = new ImageLabelV2Response();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            if (in("RecognitionResult", "AlbumLabels") && "Label".equalsIgnoreCase(name)) {
                response.getAlbumLabels().add(new LobelV2());
            } else if (in("RecognitionResult", "CameraLabels") && "Label".equalsIgnoreCase(name)) {
                response.getCameraLabels().add(new LobelV2());
            } else if (in("RecognitionResult", "WebLabels") && "Label".equalsIgnoreCase(name)) {
                response.getWebLabels().add(new LobelV2());
            } else if (in("RecognitionResult", "NewsLabels") && "Label".equalsIgnoreCase(name)) {
                response.getNewsLabels().add(new LobelV2());
            } else if (in("RecognitionResult", "NoneCamLabels") && "Label".equalsIgnoreCase(name)) {
                response.getNoneCamLabels().add(new LobelV2());
            } else if (in("RecognitionResult", "ProductLabels") && "Label".equalsIgnoreCase(name)) {
                response.getProductLabels().add(new LocationLabel());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            LobelV2 lobel = null;
            if (in("RecognitionResult", "AlbumLabels","Label")) {
                List<LobelV2> lobels = response.getAlbumLabels();
                lobel = getListLast(lobels);
            } else if (in("RecognitionResult", "CameraLabels","Label")) {
                List<LobelV2> lobels = response.getCameraLabels();
                lobel = getListLast(lobels);
            } else if (in("RecognitionResult", "WebLabels","Label")) {
                List<LobelV2> lobels = response.getWebLabels();
                lobel = getListLast(lobels);
            } else if (in("RecognitionResult", "ProductLabels","Label")) {
                List<LocationLabel> ProductLabels = response.getProductLabels();
                LocationLabel locationLabel = ProductLabels.get(ProductLabels.size() - 1);
                addLocationLabel(locationLabel, name, getText());
            } else if (in("RecognitionResult", "NewsLabels","Label")) {
                List<LobelV2> lobels = response.getNewsLabels();
                lobel = getListLast(lobels);
            } else if (in("RecognitionResult", "NoneCamLabels","Label")) {
                List<LobelV2> lobels = response.getNoneCamLabels();
                lobel = getListLast(lobels);
            }
            if (lobel != null) {
                addLabel(lobel, name, getText());
            }
        }

        public ImageLabelV2Response getResponse() {
            return response;
        }

        public void setResponse(ImageLabelV2Response response) {
            this.response = response;
        }

        private void addLabel(LobelV2 lobel, String name, String value) {
            switch (name) {
                case "Confidence":
                    lobel.setConfidence(value);
                    break;
                case "Name":
                    lobel.setName(value);
                    break;
                case "FirstCategory":
                    lobel.setFirstCategory(value);
                    break;
                case "SecondCategory":
                    lobel.setSecondCategory(value);
                    break;
                default:
                    break;
            }
        }

        private void addLocationLabel(LocationLabel lobel, String name, String value) {
            switch (name) {
                case "Confidence":
                    lobel.setConfidence(value);
                    break;
                case "Name":
                    lobel.setName(value);
                    break;
                case "Parents":
                    lobel.setParents(value);
                    break;
                case "XMax":
                    lobel.setxMax(value);
                    break;
                case "XMin":
                    lobel.setxMin(value);
                    break;
                case "YMax":
                    lobel.setyMax(value);
                    break;
                case "YMin":
                    lobel.setyMin(value);
                    break;
                default:
                    break;
            }
        }

        private LobelV2 getListLast(List<LobelV2> list) {
            return list.get(list.size() - 1);
        }
    }

    public static class TextAuditingJobHandler extends AbstractHandler {
        private TextAuditingResponse response = new TextAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
            if ((in("Response", "Detail") || in("Response", "JobsDetail")) && "Section".equals(name)) {
                sectionList.add(new SectionInfo());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
            if (in("Response", "Detail") || in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "SectionCount":
                        jobsDetail.setSectionCount(getText());
                        break;
                    case "Result":
                        jobsDetail.setResult(getText());
                        break;
                    case "Content":
                        jobsDetail.setContent(getText());
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "PornInfo")) {
                parseInfo(response.getJobsDetail().getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PoliticsInfo")) {
                parseInfo(response.getJobsDetail().getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TerrorismInfo")) {
                parseInfo(response.getJobsDetail().getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "AdsInfo")) {
                parseInfo(response.getJobsDetail().getAdsInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "AbuseInfo")) {
                parseInfo(response.getJobsDetail().getAbuseInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "IllegalInfo")) {
                parseInfo(response.getJobsDetail().getIllegalInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                if ("StartByte".equals(name))
                    sectionInfo.setStartByte(getText());
            } else if (in("Response", "JobsDetail", "Section", "PornInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "PoliticsInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "TerrorismInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Section", "AdsInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getAdsInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "Section", "AbuseInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getAbuseInfo(), name, getText());
            }else if (in("Response", "JobsDetail", "Section", "IllegalInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getIllegalInfo(), name, getText());
            }
        }

        public TextAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(TextAuditingResponse response) {
            this.response = response;
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Keywords":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                default:
                    break;
            }
        }
    }

    public static class TextAuditingDescribeJobHandler extends AbstractHandler {
        private TextAuditingResponse response = new TextAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
            if ((in("Response", "Detail") || in("Response", "JobsDetail")) && "Section".equals(name)) {
                sectionList.add(new SectionInfo());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<SectionInfo> sectionList = response.getJobsDetail().getSectionList();
            if (in("Response", "Detail") || in("Response", "JobsDetail")) {
                AuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "SectionCount":
                        jobsDetail.setSectionCount(getText());
                        break;
                    case "Result":
                        jobsDetail.setResult(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Label":
                        jobsDetail.setLabel(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "Detail", "PornInfo") || in("Response", "JobsDetail", "PornInfo")) {
                parseInfo(response.getJobsDetail().getPornInfo(), name, getText());
            } else if (in("Response", "Detail", "PoliticsInfo") || in("Response", "JobsDetail", "PoliticsInfo")) {
                parseInfo(response.getJobsDetail().getPoliticsInfo(), name, getText());
            } else if (in("Response", "Detail", "TerrorismInfo") || in("Response", "JobsDetail", "TerrorismInfo")) {
                parseInfo(response.getJobsDetail().getTerroristInfo(), name, getText());
            } else if (in("Response", "Detail", "AdsInfo") || in("Response", "JobsDetail", "AdsInfo")) {
                parseInfo(response.getJobsDetail().getAdsInfo(), name, getText());
            } else if (in("Response", "Detail", "Section") || in("Response", "JobsDetail", "Section")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                if ("StartByte".equals(name))
                    sectionInfo.setStartByte(getText());
            } else if (in("Response", "Detail", "Section", "PornInfo") || in("Response", "JobsDetail", "Section", "PornInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPornInfo(), name, getText());
            } else if (in("Response", "Detail", "Section", "PoliticsInfo") || in("Response", "JobsDetail", "Section", "PoliticsInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getPoliticsInfo(), name, getText());
            } else if (in("Response", "Detail", "Section", "TerrorismInfo") || in("Response", "JobsDetail", "Section", "TerrorismInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getTerroristInfo(), name, getText());
            } else if (in("Response", "Detail", "Section", "AdsInfo") || in("Response", "JobsDetail", "Section", "AdsInfo")) {
                SectionInfo sectionInfo = sectionList.get(sectionList.size() - 1);
                parseInfo(sectionInfo.getAdsInfo(), name, getText());
            }
        }

        public TextAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(TextAuditingResponse response) {
            this.response = response;
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Keywords":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                default:
                    break;
            }
        }
    }
    public static class DocumentAuditingJobHandler extends AbstractHandler {
        private DocumentAuditingResponse response = new DocumentAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                DocumentAuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            }
        }

        public DocumentAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(DocumentAuditingResponse response) {
            this.response = response;
        }
    }

    public static class DocumentAuditingDescribeJobHandler extends AbstractHandler {
        private DocumentAuditingResponse response = new DocumentAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<DocumentResultInfo> pageSegment = response.getJobsDetail().getPageSegment();
            if (in("Response", "JobsDetail", "PageSegment") && "Results".equals(name)) {
                pageSegment.add(new DocumentResultInfo());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results","PoliticsInfo") && "ObjectResults".equals(name)) {
                pageSegment.get(pageSegment.size() - 1).getPoliticsInfo().getObjectResults().add(new ObjectResults());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results","PornInfo") && "ObjectResults".equals(name)) {
                pageSegment.get(pageSegment.size() - 1).getPornInfo().getObjectResults().add(new ObjectResults());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results","AdsInfo") && "ObjectResults".equals(name)) {
                pageSegment.get(pageSegment.size() - 1).getAdsInfo().getObjectResults().add(new ObjectResults());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results","TerroristInfo") && "ObjectResults".equals(name)) {
                pageSegment.get(pageSegment.size() - 1).getTerroristInfo().getObjectResults().add(new ObjectResults());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            DocumentAuditingJobsDetail jobsDetail = response.getJobsDetail();
            List<DocumentResultInfo> pageSegment = jobsDetail.getPageSegment();
            DocumentResultInfo resultDetail = new DocumentResultInfo();
            if (pageSegment.size() != 0) {
                resultDetail = pageSegment.get(pageSegment.size() - 1);
            }

            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            } else if (in("Response", "JobsDetail")) {
                switch (name) {
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Suggestion":
                        jobsDetail.setSuggestion(getText());
                        break;
                    case "PageCount":
                        jobsDetail.setPageCount(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "Labels", "AdsInfo")) {
                parseInfo(response.getJobsDetail().getLabels().getAdsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Labels", "PoliticsInfo")) {
                parseInfo(response.getJobsDetail().getLabels().getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Labels", "PornInfo")) {
                parseInfo(response.getJobsDetail().getLabels().getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "Labels", "TerrorismInfo")) {
                parseInfo(response.getJobsDetail().getLabels().getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results", "AdsInfo")) {
                parseInfo(resultDetail.getAdsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results", "PoliticsInfo")) {
                parseInfo(resultDetail.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results", "PornInfo")) {
                parseInfo(resultDetail.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results", "TerrorismInfo")) {
                parseInfo(resultDetail.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "PageSegment", "Results")) {
                if ("Text".equalsIgnoreCase(name)) {
                    resultDetail.setText(getText());
                } else if ("Url".equalsIgnoreCase(name)) {
                    resultDetail.setUrl(getText());
                } else if ("Label".equalsIgnoreCase(name)) {
                    resultDetail.setLabel(getText());
                } else if ("Suggestion".equalsIgnoreCase(name)) {
                    resultDetail.setSuggestion(getText());
                } else if ("PageNumber".equalsIgnoreCase(name)) {
                    resultDetail.setPageNumber(getText());
                } else if ("SheetNumber".equalsIgnoreCase(name)) {
                    resultDetail.setSheetNumber(getText());
                }
            } else if (in("Response", "JobsDetail", "PageSegment", "Results", "PornInfo","OcrResults")) {
                parseResultInfo(resultDetail.getPornInfo().getOcrResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "PoliticsInfo","OcrResults")) {
                parseResultInfo(resultDetail.getPoliticsInfo().getOcrResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "TerrorismInfo","OcrResults")) {
                parseResultInfo(resultDetail.getTerroristInfo().getOcrResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "AdsInfo","OcrResults")) {
                parseResultInfo(resultDetail.getAdsInfo().getOcrResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "PornInfo","ObjectResults")) {
                parseResultInfo(resultDetail.getPornInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "PoliticsInfo","ObjectResults")) {
                parseResultInfo(resultDetail.getPoliticsInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "TerrorismInfo","ObjectResults")) {
                parseResultInfo(resultDetail.getTerroristInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "AdsInfo","ObjectResults")) {
                parseResultInfo(resultDetail.getAdsInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "PornInfo","ObjectResults","Location")) {
                parseResultInfo(resultDetail.getPornInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "PoliticsInfo","ObjectResults","Location")) {
                parseResultInfo(resultDetail.getPoliticsInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "TerrorismInfo","ObjectResults","Location")) {
                parseResultInfo(resultDetail.getTerroristInfo().getObjectResults(), name, getText());
            }else if (in("Response", "JobsDetail", "PageSegment", "Results", "AdsInfo","ObjectResults","Location")) {
                parseResultInfo(resultDetail.getAdsInfo().getObjectResults(), name, getText());
            }
        }

        public DocumentAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(DocumentAuditingResponse response) {
            this.response = response;
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Keywords":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                default:
                    break;
            }

        }

        private void parseResultInfo(List<ObjectResults> obj, String name, String value) {
            if (!obj.isEmpty()) {
                ObjectResults objectResult = obj.get(obj.size() - 1);
                ObjectResults.Location location = objectResult.getLocation();
                switch (name) {
                    case "Name":
                        objectResult.setName(value);
                        break;
                    case "Height":
                        location.setHeight(value);
                        break;
                    case "Rotate":
                        location.setRotate(value);
                        break;
                    case "Width":
                        location.setWidth(value);
                        break;
                    case "X":
                        location.setX(value);
                        break;
                    case "Y":
                        location.setY(value);
                        break;
                    default:
                        break;
                }
            }
        }

        private void parseResultInfo(OcrResults obj, String name, String value) {
            switch (name) {
                case "Text":
                    obj.setText(value);
                    break;
                case "Keywords":
                    obj.setKeywords(getText());
                    break;
                default:
                    break;
            }
        }
    }

    public static class BatchImageAuditingHandler extends AbstractHandler {
        private BatchImageAuditingResponse response = new BatchImageAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<BatchImageJobDetail> jobList = response.getJobList();
            if (in("Response") && "JobsDetail".equals(name)) {
                jobList.add(new BatchImageJobDetail());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            List<BatchImageJobDetail> jobList = response.getJobList();
            BatchImageJobDetail jobsDetail = null;
            if (jobList != null && jobList.size() > 0) {
                jobsDetail = jobList.get(jobList.size() - 1);
            } else {
                jobsDetail = new BatchImageJobDetail();
            }

            if (in("Response", "JobsDetail")) {
                switch (name) {
                    case "Object":
                        jobsDetail.setObject(getText());
                        break;
                    case "DataId":
                        jobsDetail.setDataId(getText());
                        break;
                    case "Label":
                        jobsDetail.setLabel(getText());
                        break;
                    case "Result":
                        jobsDetail.setResult(getText());
                        break;
                    case "Score":
                        jobsDetail.setScore(getText());
                    case "Text":
                        jobsDetail.setText(getText());
                    case "SubLabel":
                        jobsDetail.setSubLabel(getText());
                        break;
                    case "Code":
                        jobsDetail.setCode(getText());
                        break;
                    case "Message":
                        jobsDetail.setMessage(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            }else if (in("Response","JobsDetail", "PornInfo")) {
                parseInfo(jobsDetail.getPornInfo(), name, getText());
            } else if (in("Response","JobsDetail", "PoliticsInfo")) {
                parseInfo(jobsDetail.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail","TerroristInfo")) {
                parseInfo(jobsDetail.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail","AdsInfo")) {
                parseInfo(jobsDetail.getAdsInfo(), name, getText());
            }
        }

        public BatchImageAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(BatchImageAuditingResponse response) {
            this.response = response;
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "Msg":
                    obj.setMsg(getText());
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Label":
                    obj.setLabel(getText());
                    break;
                default:
                    break;
            }
        }
    }

    public static class WebpageAuditingJobHandler extends AbstractHandler {
        private WebpageAuditingResponse response = new WebpageAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {

        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            if (in("Response", "JobsDetail")) {
                WebpageAuditingJobsDetail jobsDetail = response.getJobsDetail();
                switch (name) {
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            }
        }

        public WebpageAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(WebpageAuditingResponse response) {
            this.response = response;
        }
    }

    public static class WebpageAuditingDescribeJobHandler extends AbstractHandler {
        private WebpageAuditingResponse response = new WebpageAuditingResponse();

        @Override
        protected void doStartElement(String uri, String name, String qName, Attributes attrs) {
            List<ResultsImageAuditingDetail> imageResults = response.getJobsDetail().getImageResults();
            List<ResultsTextAuditingDetail> textResults = response.getJobsDetail().getTextResults();
            if (in("Response", "JobsDetail", "ImageResults") && "Results".equals(name)) {
                imageResults.add(new ResultsImageAuditingDetail());
            } else if (in("Response", "JobsDetail", "TextResults") && "Results".equals(name)) {
                textResults.add(new ResultsTextAuditingDetail());
            }
        }

        @Override
        protected void doEndElement(String uri, String name, String qName) {
            WebpageAuditingJobsDetail jobsDetail = response.getJobsDetail();
            List<ResultsImageAuditingDetail> imageResults = jobsDetail.getImageResults();
            List<ResultsTextAuditingDetail> textResults = jobsDetail.getTextResults();
            ResultsImageAuditingDetail imageAuditingDetail = null;
            ResultsTextAuditingDetail textAuditingDetail = null;
            if (imageResults.isEmpty()) {
                imageAuditingDetail = new ResultsImageAuditingDetail();
            } else {
                imageAuditingDetail = imageResults.get(imageResults.size() - 1);
            }
            if (textResults.isEmpty()) {
                textAuditingDetail = new ResultsTextAuditingDetail();
            } else {
                textAuditingDetail = textResults.get(textResults.size() - 1);
            }
            if (in("Response")) {
                if ("RequestId".equalsIgnoreCase(name)) {
                    response.setRequestId(getText());
                }
            } else if (in("Response", "JobsDetail")) {
                switch (name) {
                    case "JobId":
                        jobsDetail.setJobId(getText());
                        break;
                    case "State":
                        jobsDetail.setState(getText());
                        break;
                    case "CreationTime":
                        jobsDetail.setCreationTime(getText());
                        break;
                    case "Suggestion":
                        jobsDetail.setSuggestion(getText());
                        break;
                    case "PageCount":
                        jobsDetail.setPageCount(getText());
                        break;
                    case "Url":
                        jobsDetail.setUrl(getText());
                        break;
                    case "Label":
                        jobsDetail.setLabel(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "ImageResults", "Results")) {
                switch (name) {
                    case "Text":
                        imageAuditingDetail.setText(getText());
                        break;
                    case "Url":
                        imageAuditingDetail.setUrl(getText());
                        break;
                    default:
                        break;
                }
            } else if (in("Response", "JobsDetail", "TextResults", "Results")) {
                if ("Text".equals(name)) {
                    textAuditingDetail.setText(getText());
                }
            } else if (in("Response", "JobsDetail", "ImageResults", "Results","PoliticsInfo")) {
                parseInfo(imageAuditingDetail.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "ImageResults", "Results", "PornInfo")) {
                parseInfo(imageAuditingDetail.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "ImageResults", "Results", "TerrorismInfo")) {
                parseInfo(imageAuditingDetail.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "ImageResults", "Results", "AdsInfo")) {
                parseInfo(imageAuditingDetail.getAdsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TextResults", "Results","PoliticsInfo")) {
                parseInfo(textAuditingDetail.getPoliticsInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TextResults", "Results", "PornInfo")) {
                parseInfo(textAuditingDetail.getPornInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TextResults", "Results", "TerrorismInfo")) {
                parseInfo(textAuditingDetail.getTerroristInfo(), name, getText());
            } else if (in("Response", "JobsDetail", "TextResults", "Results", "AdsInfo")) {
                parseInfo(textAuditingDetail.getAdsInfo(), name, getText());
            }
        }

        private void parseInfo(AudtingCommonInfo obj, String name, String value) {
            switch (name) {
                case "Code":
                    obj.setCode(value);
                    break;
                case "HitFlag":
                    obj.setHitFlag(getText());
                    break;
                case "Score":
                    obj.setScore(getText());
                    break;
                case "Keywords":
                    obj.setLabel(getText());
                    break;
                case "Count":
                    obj.setCount(getText());
                    break;
                case "SubLabel":
                    obj.setSubLabel(getText());
                    break;
                default:
                    break;
            }
        }

        public WebpageAuditingResponse getResponse() {
            return response;
        }

        public void setResponse(WebpageAuditingResponse response) {
            this.response = response;
        }

    }
}

