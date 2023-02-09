/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.internal;

import static com.aliyun.oss.common.utils.CodingUtils.isNullOrEmpty;
import static com.aliyun.oss.internal.OSSHeaders.OSS_HEADER_WORM_ID;
import static com.aliyun.oss.internal.OSSUtils.safeCloseResponse;
import static com.aliyun.oss.internal.OSSUtils.trimQuotes;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.CheckedInputStream;

import com.aliyun.oss.internal.model.OSSErrorResult;
import com.aliyun.oss.model.*;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;

import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.parser.ResponseParseException;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.common.utils.StringUtils;
import com.aliyun.oss.model.AddBucketReplicationRequest.ReplicationAction;
import com.aliyun.oss.model.DeleteVersionsResult.DeletedVersion;
import com.aliyun.oss.model.LiveChannelStat.AudioStat;
import com.aliyun.oss.model.LiveChannelStat.VideoStat;
import com.aliyun.oss.model.LifecycleRule.RuleStatus;
import com.aliyun.oss.model.LifecycleRule.StorageTransition;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;
import com.aliyun.oss.model.LifecycleRule.NoncurrentVersionStorageTransition;
import com.aliyun.oss.model.LifecycleRule.NoncurrentVersionExpiration;

/*
 * A collection of parsers that parse HTTP reponses into corresponding human-readable results.
 */
public final class ResponseParsers {

    public static final ListBucketResponseParser listBucketResponseParser = new ListBucketResponseParser();
    public static final ListImageStyleResponseParser listImageStyleResponseParser = new ListImageStyleResponseParser();
    public static final GetBucketRefererResponseParser getBucketRefererResponseParser = new GetBucketRefererResponseParser();
    public static final GetBucketAclResponseParser getBucketAclResponseParser = new GetBucketAclResponseParser();
    public static final GetBucketMetadataResponseParser getBucketMetadataResponseParser = new GetBucketMetadataResponseParser();
    public static final GetBucketLocationResponseParser getBucketLocationResponseParser = new GetBucketLocationResponseParser();
    public static final GetBucketLoggingResponseParser getBucketLoggingResponseParser = new GetBucketLoggingResponseParser();
    public static final GetBucketWebsiteResponseParser getBucketWebsiteResponseParser = new GetBucketWebsiteResponseParser();
    public static final GetBucketLifecycleResponseParser getBucketLifecycleResponseParser = new GetBucketLifecycleResponseParser();
    public static final GetBucketCorsResponseParser getBucketCorsResponseParser = new GetBucketCorsResponseParser();
    public static final GetBucketImageResponseParser getBucketImageResponseParser = new GetBucketImageResponseParser();
    public static final GetImageStyleResponseParser getImageStyleResponseParser = new GetImageStyleResponseParser();
    public static final GetBucketImageProcessConfResponseParser getBucketImageProcessConfResponseParser = new GetBucketImageProcessConfResponseParser();
    public static final GetTaggingResponseParser getTaggingResponseParser = new GetTaggingResponseParser();
    public static final GetBucketReplicationResponseParser getBucketReplicationResponseParser = new GetBucketReplicationResponseParser();
    public static final GetBucketReplicationProgressResponseParser getBucketReplicationProgressResponseParser = new GetBucketReplicationProgressResponseParser();
    public static final GetBucketReplicationLocationResponseParser getBucketReplicationLocationResponseParser = new GetBucketReplicationLocationResponseParser();
    public static final AddBucketCnameResponseParser addBucketCnameResponseParser = new AddBucketCnameResponseParser();
    public static final GetBucketCnameResponseParser getBucketCnameResponseParser = new GetBucketCnameResponseParser();
    public static final CreateBucketCnameTokenResponseParser createBucketCnameTokenResponseParser = new CreateBucketCnameTokenResponseParser();
    public static final GetBucketCnameTokenResponseParser getBucketCnameTokenResponseParser = new GetBucketCnameTokenResponseParser();
    public static final GetBucketInfoResponseParser getBucketInfoResponseParser = new GetBucketInfoResponseParser();
    public static final GetBucketStatResponseParser getBucketStatResponseParser = new GetBucketStatResponseParser();
    public static final GetBucketQosResponseParser getBucketQosResponseParser = new GetBucketQosResponseParser();
    public static final GetBucketVersioningResponseParser getBucketVersioningResponseParser = new GetBucketVersioningResponseParser();
    public static final GetBucketEncryptionResponseParser getBucketEncryptionResponseParser = new GetBucketEncryptionResponseParser();
    public static final GetBucketPolicyResponseParser getBucketPolicyResponseParser = new GetBucketPolicyResponseParser();
    public static final GetBucketRequestPaymentResponseParser getBucketRequestPaymentResponseParser = new GetBucketRequestPaymentResponseParser();
    public static final GetUSerQosInfoResponseParser getUSerQosInfoResponseParser = new GetUSerQosInfoResponseParser();
    public static final GetBucketQosInfoResponseParser getBucketQosInfoResponseParser = new GetBucketQosInfoResponseParser();
    public static final SetAsyncFetchTaskResponseParser setAsyncFetchTaskResponseParser = new SetAsyncFetchTaskResponseParser();
    public static final GetAsyncFetchTaskResponseParser getAsyncFetchTaskResponseParser = new GetAsyncFetchTaskResponseParser();
    public static final CreateVpcipResultResponseParser createVpcipResultResponseParser = new CreateVpcipResultResponseParser();
    public static final ListVpcipResultResponseParser listVpcipResultResponseParser = new ListVpcipResultResponseParser();
    public static final ListVpcPolicyResultResponseParser listVpcPolicyResultResponseParser = new ListVpcPolicyResultResponseParser();
    public static final InitiateBucketWormResponseParser initiateBucketWormResponseParser = new InitiateBucketWormResponseParser();
    public static final GetBucketWormResponseParser getBucketWormResponseParser = new GetBucketWormResponseParser();
    public static final GetBucketResourceGroupResponseParser getBucketResourceGroupResponseParser = new GetBucketResourceGroupResponseParser();
    public static final GetBucketTransferAccelerationResponseParser getBucketTransferAccelerationResponseParser = new GetBucketTransferAccelerationResponseParser();

    public static final GetBucketInventoryConfigurationParser getBucketInventoryConfigurationParser = new GetBucketInventoryConfigurationParser();
    public static final ListBucketInventoryConfigurationsParser listBucketInventoryConfigurationsParser = new ListBucketInventoryConfigurationsParser();
    public static final ListObjectsReponseParser listObjectsReponseParser = new ListObjectsReponseParser();
    public static final ListObjectsV2ResponseParser listObjectsV2ResponseParser = new ListObjectsV2ResponseParser();
    public static final ListVersionsReponseParser listVersionsReponseParser = new ListVersionsReponseParser();
    public static final PutObjectReponseParser putObjectReponseParser = new PutObjectReponseParser();
    public static final PutObjectProcessReponseParser putObjectProcessReponseParser = new PutObjectProcessReponseParser();
    public static final AppendObjectResponseParser appendObjectResponseParser = new AppendObjectResponseParser();
    public static final GetObjectMetadataResponseParser getObjectMetadataResponseParser = new GetObjectMetadataResponseParser();
    public static final CopyObjectResponseParser copyObjectResponseParser = new CopyObjectResponseParser();
    public static final DeleteObjectsResponseParser deleteObjectsResponseParser = new DeleteObjectsResponseParser();
    public static final DeleteVersionsResponseParser deleteVersionsResponseParser = new DeleteVersionsResponseParser();
    public static final GetObjectAclResponseParser getObjectAclResponseParser = new GetObjectAclResponseParser();
    public static final GetSimplifiedObjectMetaResponseParser getSimplifiedObjectMetaResponseParser = new GetSimplifiedObjectMetaResponseParser();
    public static final RestoreObjectResponseParser restoreObjectResponseParser = new RestoreObjectResponseParser();
    public static final ProcessObjectResponseParser processObjectResponseParser = new ProcessObjectResponseParser();
    public static final HeadObjectResponseParser headObjectResponseParser = new HeadObjectResponseParser();

    public static final CompleteMultipartUploadResponseParser completeMultipartUploadResponseParser = new CompleteMultipartUploadResponseParser();
    public static final CompleteMultipartUploadProcessResponseParser completeMultipartUploadProcessResponseParser = new CompleteMultipartUploadProcessResponseParser();
    public static final InitiateMultipartUploadResponseParser initiateMultipartUploadResponseParser = new InitiateMultipartUploadResponseParser();
    public static final ListMultipartUploadsResponseParser listMultipartUploadsResponseParser = new ListMultipartUploadsResponseParser();
    public static final ListPartsResponseParser listPartsResponseParser = new ListPartsResponseParser();

    public static final CreateLiveChannelResponseParser createLiveChannelResponseParser = new CreateLiveChannelResponseParser();
    public static final GetLiveChannelInfoResponseParser getLiveChannelInfoResponseParser = new GetLiveChannelInfoResponseParser();
    public static final GetLiveChannelStatResponseParser getLiveChannelStatResponseParser = new GetLiveChannelStatResponseParser();
    public static final GetLiveChannelHistoryResponseParser getLiveChannelHistoryResponseParser = new GetLiveChannelHistoryResponseParser();
    public static final ListLiveChannelsReponseParser listLiveChannelsReponseParser = new ListLiveChannelsReponseParser();

    public static final GetSymbolicLinkResponseParser getSymbolicLinkResponseParser = new GetSymbolicLinkResponseParser();

    public static final DeleteDirectoryResponseParser deleteDirectoryResponseParser = new DeleteDirectoryResponseParser();
    public static final GetBucketAccessMonitorResponseParser getBucketAccessMonitorResponseParser = new GetBucketAccessMonitorResponseParser();
    public static final GetMetaQueryStatusResponseParser getMetaQueryStatusResponseParser = new GetMetaQueryStatusResponseParser();
    public static final DoMetaQueryResponseParser doMetaQueryResponseParser = new DoMetaQueryResponseParser();

    public static Long parseLongWithDefault(String defaultValue){
        if(defaultValue == null || "".equals(defaultValue)){
            return 0L;
        }
        return Long.parseLong(defaultValue);
    }

    public static final class EmptyResponseParser implements ResponseParser<ResponseMessage> {

        @Override
        public ResponseMessage parse(ResponseMessage response) throws ResponseParseException {
            // Close response and return it directly without parsing.
            safeCloseResponse(response);
            return response;
        }

    }


    public static final class ErrorResponseParser implements ResponseParser<OSSErrorResult> {
        @Override
        public OSSErrorResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseErrorResponse(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

        private OSSErrorResult parseErrorResponse(InputStream inputStream) throws ResponseParseException {
            OSSErrorResult ossErrorResult = new OSSErrorResult();
            if (inputStream == null) {
                return ossErrorResult;
            }
            try {
                Element root = getXmlRootElement(inputStream);
                ossErrorResult.Code = root.getChildText("Code");
                ossErrorResult.Message = root.getChildText("Message");
                ossErrorResult.RequestId = root.getChildText("RequestId");
                ossErrorResult.HostId = root.getChildText("HostId");
                ossErrorResult.ResourceType = root.getChildText("ResourceType");
                ossErrorResult.Method = root.getChildText("Method");
                ossErrorResult.Header = root.getChildText("Header");
                return ossErrorResult;
            } catch (JDOMParseException e) {
                throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseParseException(e.getMessage(), e);
            }
        }
    }

    public static void setResultParameter(GenericResult result, ResponseMessage response){
        result.setRequestId(response.getRequestId());
        setCRC(result, response);
        result.setResponse(response);
    }

    public static final class RequestIdResponseParser implements ResponseParser<VoidResult> {

        @Override
        public VoidResult parse(ResponseMessage response) throws ResponseParseException {
            try{
                VoidResult result = new VoidResult();
                result.setResponse(response);
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListBucketResponseParser implements ResponseParser<BucketList> {

        @Override
        public BucketList parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketList result = parseListBucket(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListImageStyleResponseParser implements ResponseParser<List<Style>> {
        @Override
        public List<Style> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseListImageStyle(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetBucketRefererResponseParser implements ResponseParser<BucketReferer> {

        @Override
        public BucketReferer parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketReferer result = parseGetBucketReferer(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketAclResponseParser implements ResponseParser<AccessControlList> {

        @Override
        public AccessControlList parse(ResponseMessage response) throws ResponseParseException {
            try {
                AccessControlList result = parseGetBucketAcl(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }
	
    public static final class GetBucketMetadataResponseParser implements ResponseParser<BucketMetadata> {

        @Override
        public BucketMetadata parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseBucketMetadata(response.getHeaders());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketLocationResponseParser implements ResponseParser<String> {

        @Override
        public String parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketLocation(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketLoggingResponseParser implements ResponseParser<BucketLoggingResult> {

        @Override
        public BucketLoggingResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketLoggingResult result = parseBucketLogging(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketImageResponseParser implements ResponseParser<GetBucketImageResult> {
        @Override
        public GetBucketImageResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseBucketImage(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetImageStyleResponseParser implements ResponseParser<GetImageStyleResult> {
        @Override
        public GetImageStyleResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseImageStyle(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetBucketImageProcessConfResponseParser implements ResponseParser<BucketProcess> {

        @Override
        public BucketProcess parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketProcess result = parseGetBucketImageProcessConf(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketWebsiteResponseParser implements ResponseParser<BucketWebsiteResult> {

        @Override
        public BucketWebsiteResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketWebsiteResult result = parseBucketWebsite(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketLifecycleResponseParser implements ResponseParser<List<LifecycleRule>> {

        @Override
        public List<LifecycleRule> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketLifecycle(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class AddBucketCnameResponseParser implements ResponseParser<AddBucketCnameResult> {
        @Override
        public AddBucketCnameResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                AddBucketCnameResult result = new AddBucketCnameResult();
                result.setCertId(response.getHeaders().get(OSSHeaders.OSS_HEADER_CERT_ID));
                result.setRequestId(response.getRequestId());
                result.setResponse(response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetBucketCnameResponseParser implements ResponseParser<List<CnameConfiguration>> {

        @Override
        public List<CnameConfiguration> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketCname(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class CreateBucketCnameTokenResponseParser implements ResponseParser<CreateBucketCnameTokenResult> {

        @Override
        public CreateBucketCnameTokenResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseCreateBucketCnameToken(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetBucketCnameTokenResponseParser implements ResponseParser<GetBucketCnameTokenResult> {

        @Override
        public GetBucketCnameTokenResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketCnameToken(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class GetBucketInfoResponseParser implements ResponseParser<BucketInfo> {

        @Override
        public BucketInfo parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketInfo result = parseGetBucketInfo(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketStatResponseParser implements ResponseParser<BucketStat> {

        @Override
        public BucketStat parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketStat result = parseGetBucketStat(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketQosResponseParser implements ResponseParser<UserQos> {

        @Override
        public UserQos parse(ResponseMessage response) throws ResponseParseException {
            try {
                UserQos result = parseGetUserQos(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }
    
    public static final class GetBucketVersioningResponseParser
        implements ResponseParser<BucketVersioningConfiguration> {

        @Override
        public BucketVersioningConfiguration parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketVersioningConfiguration result = parseGetBucketVersioning(response.getContent());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketEncryptionResponseParser
    implements ResponseParser<ServerSideEncryptionConfiguration> {
    	
    	@Override
    	public ServerSideEncryptionConfiguration parse(ResponseMessage response) throws ResponseParseException {
    		try {
    			ServerSideEncryptionConfiguration result = parseGetBucketEncryption(response.getContent());
    			return result;
    		} finally {
    			safeCloseResponse(response);
    		}
    	}

    }

    public static final class GetBucketPolicyResponseParser implements ResponseParser<GetBucketPolicyResult> {
    	
        @Override
        public GetBucketPolicyResult parse(ResponseMessage response) throws ResponseParseException {
        	try {
        		GetBucketPolicyResult result = parseGetBucketPolicy(response.getContent());
        		result.setRequestId(response.getRequestId());
        		return result;
        	} finally {
        		safeCloseResponse(response);
        	}
        }

    }

    public static final class GetBucketRequestPaymentResponseParser implements ResponseParser<GetBucketRequestPaymentResult> {

        @Override
        public GetBucketRequestPaymentResult parse(ResponseMessage response) throws ResponseParseException {
            try {
            	GetBucketRequestPaymentResult result = parseGetBucketRequestPayment(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetUSerQosInfoResponseParser implements ResponseParser<UserQosInfo> {

        @Override
        public UserQosInfo parse(ResponseMessage response) throws ResponseParseException {
            try {
                UserQosInfo result = parseGetUserQosInfo(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketQosInfoResponseParser implements ResponseParser<BucketQosInfo> {

        @Override
        public BucketQosInfo parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketQosInfo result = parseGetBucketQosInfo(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class SetAsyncFetchTaskResponseParser implements ResponseParser<SetAsyncFetchTaskResult> {

        @Override
        public SetAsyncFetchTaskResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                SetAsyncFetchTaskResult result = parseSetAsyncFetchTaskResult(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetAsyncFetchTaskResponseParser implements ResponseParser<GetAsyncFetchTaskResult> {

        @Override
        public GetAsyncFetchTaskResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                GetAsyncFetchTaskResult result = parseGetAsyncFetchTaskResult(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketInventoryConfigurationParser implements ResponseParser<GetBucketInventoryConfigurationResult> {

        @Override
        public GetBucketInventoryConfigurationResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                GetBucketInventoryConfigurationResult result = parseGetBucketInventoryConfig(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListBucketInventoryConfigurationsParser implements ResponseParser<ListBucketInventoryConfigurationsResult> {

        @Override
        public ListBucketInventoryConfigurationsResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                ListBucketInventoryConfigurationsResult result = parseListBucketInventoryConfigurations(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class CreateLiveChannelResponseParser implements ResponseParser<CreateLiveChannelResult> {

        @Override
        public CreateLiveChannelResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                CreateLiveChannelResult result = parseCreateLiveChannel(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetLiveChannelInfoResponseParser implements ResponseParser<LiveChannelInfo> {

        @Override
        public LiveChannelInfo parse(ResponseMessage response) throws ResponseParseException {
            try {
                LiveChannelInfo result = parseGetLiveChannelInfo(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetLiveChannelStatResponseParser implements ResponseParser<LiveChannelStat> {

        @Override
        public LiveChannelStat parse(ResponseMessage response) throws ResponseParseException {
            try {
                LiveChannelStat result = parseGetLiveChannelStat(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetLiveChannelHistoryResponseParser implements ResponseParser<List<LiveRecord>> {

        @Override
        public List<LiveRecord> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetLiveChannelHistory(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListLiveChannelsReponseParser implements ResponseParser<LiveChannelListing> {

        @Override
        public LiveChannelListing parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseListLiveChannels(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketCorsResponseParser implements ResponseParser<CORSConfiguration> {

        @Override
        public CORSConfiguration parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseListBucketCORS(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetTaggingResponseParser implements ResponseParser<TagSet> {

        @Override
        public TagSet parse(ResponseMessage response) throws ResponseParseException {
            try {
                TagSet result = parseGetBucketTagging(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketReplicationResponseParser implements ResponseParser<List<ReplicationRule>> {

        @Override
        public List<ReplicationRule> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketReplication(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketReplicationProgressResponseParser
            implements ResponseParser<BucketReplicationProgress> {

        @Override
        public BucketReplicationProgress parse(ResponseMessage response) throws ResponseParseException {
            try {
                BucketReplicationProgress result = parseGetBucketReplicationProgress(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketReplicationLocationResponseParser implements ResponseParser<List<String>> {

        @Override
        public List<String> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseGetBucketReplicationLocation(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListObjectsReponseParser implements ResponseParser<ObjectListing> {

        @Override
        public ObjectListing parse(ResponseMessage response) throws ResponseParseException {
            try {
                ObjectListing result = parseListObjects(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListObjectsV2ResponseParser implements ResponseParser<ListObjectsV2Result> {

        @Override
        public ListObjectsV2Result parse(ResponseMessage response) throws ResponseParseException {
            try {
                ListObjectsV2Result result = parseListObjectsV2(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }
    
    public static final class ListVersionsReponseParser implements ResponseParser<VersionListing> {

        @Override
        public VersionListing parse(ResponseMessage response) throws ResponseParseException {
            try {
                VersionListing result = parseListVersions(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class PutObjectReponseParser implements ResponseParser<PutObjectResult> {

        @Override
        public PutObjectResult parse(ResponseMessage response) throws ResponseParseException {
            PutObjectResult result = new PutObjectResult();
            try {
                result.setETag(trimQuotes(response.getHeaders().get(OSSHeaders.ETAG)));
                result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
                result.setRequestId(response.getRequestId());
                setCRC(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class PutObjectProcessReponseParser implements ResponseParser<PutObjectResult> {

        @Override
        public PutObjectResult parse(ResponseMessage response) throws ResponseParseException {
            PutObjectResult result = new PutObjectResult();
            result.setRequestId(response.getRequestId());
            result.setETag(trimQuotes(response.getHeaders().get(OSSHeaders.ETAG)));
            result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
            result.setCallbackResponseBody(response.getContent());
            result.setResponse(response);
            return result;
        }

    }

    public static final class AppendObjectResponseParser implements ResponseParser<AppendObjectResult> {

        @Override
        public AppendObjectResult parse(ResponseMessage response) throws ResponseParseException {
            AppendObjectResult result = new AppendObjectResult();
            result.setRequestId(response.getRequestId());
            try {
                String nextPosition = response.getHeaders().get(OSSHeaders.OSS_NEXT_APPEND_POSITION);
                if (nextPosition != null) {
                    result.setNextPosition(Long.valueOf(nextPosition));
                }
                result.setObjectCRC(response.getHeaders().get(OSSHeaders.OSS_HASH_CRC64_ECMA));
                result.setResponse(response);
                setCRC(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetObjectResponseParser implements ResponseParser<OSSObject> {
        private String bucketName;
        private String key;

        public GetObjectResponseParser(final String bucketName, final String key) {
            this.bucketName = bucketName;
            this.key = key;
        }

        @Override
        public OSSObject parse(ResponseMessage response) throws ResponseParseException {
            OSSObject ossObject = new OSSObject();
            ossObject.setBucketName(this.bucketName);
            ossObject.setKey(this.key);
            ossObject.setObjectContent(response.getContent());
            ossObject.setRequestId(response.getRequestId());
            ossObject.setResponse(response);
            try {
                ossObject.setObjectMetadata(parseObjectMetadata(response.getHeaders()));
                setServerCRC(ossObject, response);
                return ossObject;
            } catch (ResponseParseException e) {
                // Close response only when parsing exception thrown. Otherwise,
                // just hand over to SDK users and remain them close it when no
                // longer in use.
                safeCloseResponse(response);

                // Rethrow
                throw e;
            }
        }

    }

    public static final class GetObjectAclResponseParser implements ResponseParser<ObjectAcl> {

        @Override
        public ObjectAcl parse(ResponseMessage response) throws ResponseParseException {
            try {
                ObjectAcl result = parseGetObjectAcl(response.getContent());
                result.setRequestId(response.getRequestId());
                result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetSimplifiedObjectMetaResponseParser implements ResponseParser<SimplifiedObjectMeta> {

        @Override
        public SimplifiedObjectMeta parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseSimplifiedObjectMeta(response.getHeaders());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class RestoreObjectResponseParser implements ResponseParser<RestoreObjectResult> {

        @Override
        public RestoreObjectResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                RestoreObjectResult result = new RestoreObjectResult(response.getStatusCode());
                result.setRequestId(response.getRequestId());
                result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ProcessObjectResponseParser implements ResponseParser<GenericResult> {

        @Override
        public GenericResult parse(ResponseMessage response) throws ResponseParseException {
            GenericResult result = new PutObjectResult();
            result.setRequestId(response.getRequestId());
            result.setResponse(response);
            return result;
        }

    }

    public static final class GetObjectMetadataResponseParser implements ResponseParser<ObjectMetadata> {

        @Override
        public ObjectMetadata parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseObjectMetadata(response.getHeaders());
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class HeadObjectResponseParser implements ResponseParser<ObjectMetadata> {

        @Override
        public ObjectMetadata parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseObjectMetadata(response.getHeaders());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class CopyObjectResponseParser implements ResponseParser<CopyObjectResult> {

        @Override
        public CopyObjectResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                CopyObjectResult result = parseCopyObjectResult(response.getContent());
                result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
                result.setRequestId(response.getRequestId());
                result.setResponse(response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class DeleteObjectsResponseParser implements ResponseParser<DeleteObjectsResult> {

        @Override
        public DeleteObjectsResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                DeleteObjectsResult result = null;

                // Occurs when deleting multiple objects in quiet mode.
                if (response.getContentLength() == 0) {
                    result = new DeleteObjectsResult(null);
                } else {
                    result = parseDeleteObjectsResult(response.getContent());
                }
                result.setRequestId(response.getRequestId());

                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }
    
    public static final class DeleteVersionsResponseParser implements ResponseParser<DeleteVersionsResult> {

        @Override
        public DeleteVersionsResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                DeleteVersionsResult result = null;

                // Occurs when deleting multiple objects in quiet mode.
                if (response.getContentLength() == 0) {
                    result = new DeleteVersionsResult(new ArrayList<DeletedVersion>());
                } else {
                    result = parseDeleteVersionsResult(response.getContent());
                }
                result.setRequestId(response.getRequestId());

                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class CompleteMultipartUploadResponseParser
            implements ResponseParser<CompleteMultipartUploadResult> {

        @Override
        public CompleteMultipartUploadResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                CompleteMultipartUploadResult result = parseCompleteMultipartUpload(response.getContent());
                result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
                result.setRequestId(response.getRequestId());
                setServerCRC(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class CompleteMultipartUploadProcessResponseParser
            implements ResponseParser<CompleteMultipartUploadResult> {

        @Override
        public CompleteMultipartUploadResult parse(ResponseMessage response) throws ResponseParseException {
            CompleteMultipartUploadResult result = new CompleteMultipartUploadResult();
            result.setVersionId(response.getHeaders().get(OSSHeaders.OSS_HEADER_VERSION_ID));
            result.setRequestId(response.getRequestId());
            result.setCallbackResponseBody(response.getContent());
            result.setResponse(response);
            return result;
        }

    }

    public static final class InitiateMultipartUploadResponseParser
            implements ResponseParser<InitiateMultipartUploadResult> {

        @Override
        public InitiateMultipartUploadResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                InitiateMultipartUploadResult result = parseInitiateMultipartUpload(response.getContent());
                result.setRequestId(response.getRequestId());
                result.setResponse(response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListMultipartUploadsResponseParser implements ResponseParser<MultipartUploadListing> {

        @Override
        public MultipartUploadListing parse(ResponseMessage response) throws ResponseParseException {
            try {
                MultipartUploadListing result = parseListMultipartUploads(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListPartsResponseParser implements ResponseParser<PartListing> {

        @Override
        public PartListing parse(ResponseMessage response) throws ResponseParseException {
            try {
                PartListing result = parseListParts(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class UploadPartCopyResponseParser implements ResponseParser<UploadPartCopyResult> {

        private int partNumber;

        public UploadPartCopyResponseParser(int partNumber) {
            this.partNumber = partNumber;
        }

        @Override
        public UploadPartCopyResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                UploadPartCopyResult result = new UploadPartCopyResult();
                result.setPartNumber(partNumber);
                result.setETag(trimQuotes(parseUploadPartCopy(response.getContent())));
                result.setRequestId(response.getRequestId());
                result.setResponse(response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetSymbolicLinkResponseParser implements ResponseParser<OSSSymlink> {

        @Override
        public OSSSymlink parse(ResponseMessage response) throws ResponseParseException {
            try {
                OSSSymlink result = parseSymbolicLink(response);
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                OSSUtils.mandatoryCloseResponse(response);
            }
        }

    }

    public static final class InitiateBucketWormResponseParser implements ResponseParser<InitiateBucketWormResult> {

        @Override
        public InitiateBucketWormResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                InitiateBucketWormResult result = parseInitiateBucketWormResponseHeader(response.getHeaders());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketWormResponseParser implements ResponseParser<GetBucketWormResult> {

        @Override
        public GetBucketWormResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                GetBucketWormResult result = parseWormConfiguration(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class GetBucketResourceGroupResponseParser implements ResponseParser<GetBucketResourceGroupResult> {

        @Override
        public GetBucketResourceGroupResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                GetBucketResourceGroupResult result = parseResourceGroupConfiguration(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static <ResultType extends GenericResult> void setCRC(ResultType result, ResponseMessage response) {
        if(response.getRequest() != null) {
            InputStream inputStream = response.getRequest().getContent();
            if (inputStream instanceof CheckedInputStream) {
                CheckedInputStream checkedInputStream = (CheckedInputStream) inputStream;
                result.setClientCRC(checkedInputStream.getChecksum().getValue());
            }

            String strSrvCrc = response.getHeaders().get(OSSHeaders.OSS_HASH_CRC64_ECMA);
            if (strSrvCrc != null) {
                BigInteger bi = new BigInteger(strSrvCrc);
                result.setServerCRC(bi.longValue());
            }
        }
    }

    public static <ResultType extends GenericResult> void setServerCRC(ResultType result, ResponseMessage response) {
        String strSrvCrc = response.getHeaders().get(OSSHeaders.OSS_HASH_CRC64_ECMA);
        if (strSrvCrc != null) {
            BigInteger bi = new BigInteger(strSrvCrc);
            result.setServerCRC(bi.longValue());
        }
    }

    private static Element getXmlRootElement(InputStream responseBody) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        builder.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        builder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        builder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        builder.setExpandEntities(false);
        Document doc = builder.build(responseBody);
        return doc.getRootElement();
    }

    /**
     * Unmarshall list objects response body to object listing.
     */
    @SuppressWarnings("unchecked")
    public static ObjectListing parseListObjects(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            ObjectListing objectListing = new ObjectListing();
            objectListing.setBucketName(root.getChildText("Name"));
            objectListing.setMaxKeys(Integer.valueOf(root.getChildText("MaxKeys")));
            objectListing.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));

            if (root.getChild("Prefix") != null) {
                String prefix = root.getChildText("Prefix");
                objectListing.setPrefix(isNullOrEmpty(prefix) ? null : prefix);
            }

            if (root.getChild("Marker") != null) {
                String marker = root.getChildText("Marker");
                objectListing.setMarker(isNullOrEmpty(marker) ? null : marker);
            }

            if (root.getChild("Delimiter") != null) {
                String delimiter = root.getChildText("Delimiter");
                objectListing.setDelimiter(isNullOrEmpty(delimiter) ? null : delimiter);
            }

            if (root.getChild("NextMarker") != null) {
                String nextMarker = root.getChildText("NextMarker");
                objectListing.setNextMarker(isNullOrEmpty(nextMarker) ? null : nextMarker);
            }

            if (root.getChild("EncodingType") != null) {
                String encodingType = root.getChildText("EncodingType");
                objectListing.setEncodingType(isNullOrEmpty(encodingType) ? null : encodingType);
            }

            List<Element> objectSummaryElems = root.getChildren("Contents");
            for (Element elem : objectSummaryElems) {
                OSSObjectSummary ossObjectSummary = new OSSObjectSummary();

                ossObjectSummary.setKey(elem.getChildText("Key"));
                ossObjectSummary.setETag(trimQuotes(elem.getChildText("ETag")));
                ossObjectSummary.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));
                ossObjectSummary.setSize(Long.valueOf(elem.getChildText("Size")));
                ossObjectSummary.setStorageClass(elem.getChildText("StorageClass"));
                ossObjectSummary.setRestoreInfo(elem.getChildText("RestoreInfo"));
                ossObjectSummary.setBucketName(objectListing.getBucketName());

                if (elem.getChild("Type") != null) {
                    ossObjectSummary.setType(elem.getChildText("Type"));
                }

                String id = elem.getChild("Owner").getChildText("ID");
                String displayName = elem.getChild("Owner").getChildText("DisplayName");
                ossObjectSummary.setOwner(new Owner(id, displayName));

                objectListing.addObjectSummary(ossObjectSummary);
            }

            List<Element> commonPrefixesElems = root.getChildren("CommonPrefixes");
            for (Element elem : commonPrefixesElems) {
                String prefix = elem.getChildText("Prefix");
                if (!isNullOrEmpty(prefix)) {
                    objectListing.addCommonPrefix(prefix);
                }
            }

            return objectListing;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall list objects response body to ListObjectsV2Result.
     */
    @SuppressWarnings("unchecked")
    public static ListObjectsV2Result parseListObjectsV2(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            ListObjectsV2Result result = new ListObjectsV2Result();
            result.setBucketName(root.getChildText("Name"));
            result.setMaxKeys(Integer.valueOf(root.getChildText("MaxKeys")));
            result.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));
            result.setKeyCount(Integer.valueOf(root.getChildText("KeyCount")));

            if (root.getChild("Prefix") != null) {
                String prefix = root.getChildText("Prefix");
                result.setPrefix(isNullOrEmpty(prefix) ? null : prefix);
            }

            if (root.getChild("Delimiter") != null) {
                String delimiter = root.getChildText("Delimiter");
                result.setDelimiter(isNullOrEmpty(delimiter) ? null : delimiter);
            }

            if (root.getChild("ContinuationToken") != null) {
                String continuationToken = root.getChildText("ContinuationToken");
                result.setContinuationToken(isNullOrEmpty(continuationToken) ? null : continuationToken);
            }

            if (root.getChild("NextContinuationToken") != null) {
                String nextContinuationToken = root.getChildText("NextContinuationToken");
                result.setNextContinuationToken(isNullOrEmpty(nextContinuationToken) ? null : nextContinuationToken);
            }

            if (root.getChild("EncodingType") != null) {
                String encodeType = root.getChildText("EncodingType");
                result.setEncodingType(isNullOrEmpty(encodeType) ? null : encodeType);
            }

            if (root.getChild("StartAfter") != null) {
                String startAfter = root.getChildText("StartAfter");
                result.setStartAfter(isNullOrEmpty(startAfter) ? null : startAfter);
            }

            List<Element> objectSummaryElems = root.getChildren("Contents");
            for (Element elem : objectSummaryElems) {
                OSSObjectSummary ossObjectSummary = new OSSObjectSummary();

                ossObjectSummary.setKey(elem.getChildText("Key"));
                ossObjectSummary.setETag(trimQuotes(elem.getChildText("ETag")));
                ossObjectSummary.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));
                ossObjectSummary.setSize(Long.valueOf(elem.getChildText("Size")));
                ossObjectSummary.setStorageClass(elem.getChildText("StorageClass"));
                ossObjectSummary.setRestoreInfo(elem.getChildText("RestoreInfo"));
                ossObjectSummary.setBucketName(result.getBucketName());

                if (elem.getChild("Type") != null) {
                    ossObjectSummary.setType(elem.getChildText("Type"));
                }

                if (elem.getChild("Owner") != null) {
                    String id = elem.getChild("Owner").getChildText("ID");
                    String displayName = elem.getChild("Owner").getChildText("DisplayName");
                    ossObjectSummary.setOwner(new Owner(id, displayName));
                }

                result.addObjectSummary(ossObjectSummary);
            }

            List<Element> commonPrefixesElems = root.getChildren("CommonPrefixes");

            for (Element elem : commonPrefixesElems) {
                String prefix = elem.getChildText("Prefix");
                if (!isNullOrEmpty(prefix)) {
                    result.addCommonPrefix(prefix);
                }
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }
    
    /**
     * Unmarshall list objects response body to object listing.
     */
    @SuppressWarnings("unchecked")
    public static VersionListing parseListVersions(InputStream responseBody) throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);

            boolean shouldSDKDecode = false;
            VersionListing versionListing = new VersionListing();
            versionListing.setBucketName(root.getChildText("Name"));
            versionListing.setMaxKeys(Integer.valueOf(root.getChildText("MaxKeys")));
            versionListing.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));

            if (root.getChild("EncodingType") != null) {
                String encodingType = root.getChildText("EncodingType");
                if (encodingType.equals(OSSConstants.URL_ENCODING)) {
                    shouldSDKDecode = true;
                    versionListing.setEncodingType(null);
                } else {
                    versionListing.setEncodingType(isNullOrEmpty(encodingType) ? null : encodingType);
                }
            }

            if (root.getChild("Prefix") != null) {
                String prefix = root.getChildText("Prefix");
                versionListing.setPrefix(isNullOrEmpty(prefix) ? null : decodeIfSpecified(prefix, shouldSDKDecode));
            }

            if (root.getChild("KeyMarker") != null) {
                String marker = root.getChildText("KeyMarker");
                versionListing.setKeyMarker(isNullOrEmpty(marker) ? null : decodeIfSpecified(marker, shouldSDKDecode));
            }

            if (root.getChild("VersionIdMarker") != null) {
                String marker = root.getChildText("VersionIdMarker");
                versionListing.setVersionIdMarker(isNullOrEmpty(marker) ? null : marker);
            }

            if (root.getChild("Delimiter") != null) {
                String delimiter = root.getChildText("Delimiter");
                versionListing
                    .setDelimiter(isNullOrEmpty(delimiter) ? null : decodeIfSpecified(delimiter, shouldSDKDecode));
            }

            if (root.getChild("NextKeyMarker") != null) {
                String nextMarker = root.getChildText("NextKeyMarker");
                versionListing.setNextKeyMarker(
                    isNullOrEmpty(nextMarker) ? null : decodeIfSpecified(nextMarker, shouldSDKDecode));
            }

            if (root.getChild("NextVersionIdMarker") != null) {
                String nextMarker = root.getChildText("NextVersionIdMarker");
                versionListing.setNextVersionIdMarker(isNullOrEmpty(nextMarker) ? null : nextMarker);
            }

            List<Element> objectSummaryElems = root.getChildren("Version");
            for (Element elem : objectSummaryElems) {
                OSSVersionSummary ossVersionSummary = new OSSVersionSummary();

                ossVersionSummary.setKey(decodeIfSpecified(elem.getChildText("Key"), shouldSDKDecode));
                ossVersionSummary.setVersionId(elem.getChildText("VersionId"));
                ossVersionSummary.setIsLatest("true".equals(elem.getChildText("IsLatest")));
                ossVersionSummary.setETag(trimQuotes(elem.getChildText("ETag")));
                ossVersionSummary.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));
                ossVersionSummary.setSize(Long.valueOf(elem.getChildText("Size")));
                ossVersionSummary.setStorageClass(elem.getChildText("StorageClass"));
                ossVersionSummary.setRestoreInfo(elem.getChildText("RestoreInfo"));
                ossVersionSummary.setBucketName(versionListing.getBucketName());
                ossVersionSummary.setIsDeleteMarker(false);

                String id = elem.getChild("Owner").getChildText("ID");
                String displayName = elem.getChild("Owner").getChildText("DisplayName");
                ossVersionSummary.setOwner(new Owner(id, displayName));

                versionListing.getVersionSummaries().add(ossVersionSummary);
            }

            List<Element> delSummaryElems = root.getChildren("DeleteMarker");
            for (Element elem : delSummaryElems) {
                OSSVersionSummary ossVersionSummary = new OSSVersionSummary();

                ossVersionSummary.setKey(decodeIfSpecified(elem.getChildText("Key"), shouldSDKDecode));
                ossVersionSummary.setVersionId(elem.getChildText("VersionId"));
                ossVersionSummary.setIsLatest("true".equals(elem.getChildText("IsLatest")));
                ossVersionSummary.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));
                ossVersionSummary.setBucketName(versionListing.getBucketName());
                ossVersionSummary.setIsDeleteMarker(true);

                String id = elem.getChild("Owner").getChildText("ID");
                String displayName = elem.getChild("Owner").getChildText("DisplayName");
                ossVersionSummary.setOwner(new Owner(id, displayName));

                versionListing.getVersionSummaries().add(ossVersionSummary);
            }

            List<Element> commonPrefixesElems = root.getChildren("CommonPrefixes");
            for (Element elem : commonPrefixesElems) {
                String prefix = elem.getChildText("Prefix");
                if (!isNullOrEmpty(prefix)) {
                    versionListing.getCommonPrefixes().add(decodeIfSpecified(prefix, shouldSDKDecode));
                }
            }

            return versionListing;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }
    
    /**
     * Perform an url decode on the given value if specified.
     */
    private static String decodeIfSpecified(String value, boolean decode) {
        return decode ? HttpUtil.urlDecode(value, StringUtils.DEFAULT_ENCODING) : value;
    }

    /**
     * Unmarshall get bucket acl response body to ACL.
     */
    public static AccessControlList parseGetBucketAcl(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            AccessControlList acl = new AccessControlList();

            String id = root.getChild("Owner").getChildText("ID");
            String displayName = root.getChild("Owner").getChildText("DisplayName");
            Owner owner = new Owner(id, displayName);
            acl.setOwner(owner);

            String aclString = root.getChild("AccessControlList").getChildText("Grant");
            CannedAccessControlList cacl = CannedAccessControlList.parse(aclString);
            acl.setCannedACL(cacl);

            switch (cacl) {
            case PublicRead:
                acl.grantPermission(GroupGrantee.AllUsers, Permission.Read);
                break;
            case PublicReadWrite:
                acl.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
                break;
            default:
                break;
            }

            return acl;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall object acl response body to object ACL.
     */
    public static ObjectAcl parseGetObjectAcl(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            ObjectAcl acl = new ObjectAcl();

            String id = root.getChild("Owner").getChildText("ID");
            String displayName = root.getChild("Owner").getChildText("DisplayName");
            Owner owner = new Owner(id, displayName);
            acl.setOwner(owner);

            String grantString = root.getChild("AccessControlList").getChildText("Grant");
            acl.setPermission(ObjectPermission.parsePermission(grantString));

            return acl;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket referer response body to bucket referer list.
     */
    @SuppressWarnings("unchecked")
    public static BucketReferer parseGetBucketReferer(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            boolean allowEmptyReferer = Boolean.valueOf(root.getChildText("AllowEmptyReferer"));
            List<String> refererList = new ArrayList<String>();
            if (root.getChild("RefererList") != null) {
                Element refererListElem = root.getChild("RefererList");
                List<Element> refererElems = refererListElem.getChildren("Referer");
                if (refererElems != null && !refererElems.isEmpty()) {
                    for (Element e : refererElems) {
                        refererList.add(e.getText());
                    }
                }
            }
            return new BucketReferer(allowEmptyReferer, refererList);
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall upload part copy response body to uploaded part's ETag.
     */
    public static String parseUploadPartCopy(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            return root.getChildText("ETag");
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall list bucket response body to bucket list.
     */
    @SuppressWarnings("unchecked")
    public static BucketList parseListBucket(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            BucketList bucketList = new BucketList();
            if (root.getChild("Prefix") != null) {
                bucketList.setPrefix(root.getChildText("Prefix"));
            }
            if (root.getChild("Marker") != null) {
                bucketList.setMarker(root.getChildText("Marker"));
            }
            if (root.getChild("MaxKeys") != null) {
                String value = root.getChildText("MaxKeys");
                bucketList.setMaxKeys(isNullOrEmpty(value) ? null : Integer.valueOf(value));
            }
            if (root.getChild("IsTruncated") != null) {
                String value = root.getChildText("IsTruncated");
                bucketList.setTruncated(isNullOrEmpty(value) ? false : Boolean.valueOf(value));
            }
            if (root.getChild("NextMarker") != null) {
                bucketList.setNextMarker(root.getChildText("NextMarker"));
            }

            Element ownerElem = root.getChild("Owner");
            String id = ownerElem.getChildText("ID");
            String displayName = ownerElem.getChildText("DisplayName");
            Owner owner = new Owner(id, displayName);

            List<Bucket> buckets = new ArrayList<Bucket>();
            if (root.getChild("Buckets") != null) {
                List<Element> bucketElems = root.getChild("Buckets").getChildren("Bucket");
                for (Element e : bucketElems) {
                    Bucket bucket = new Bucket();
                    bucket.setOwner(owner);
                    bucket.setName(e.getChildText("Name"));
                    bucket.setLocation(e.getChildText("Location"));
                    bucket.setCreationDate(DateUtil.parseIso8601Date(e.getChildText("CreationDate")));
                    if (e.getChild("StorageClass") != null) {
                        bucket.setStorageClass(StorageClass.parse(e.getChildText("StorageClass")));
                    }
                    bucket.setExtranetEndpoint(e.getChildText("ExtranetEndpoint"));
                    bucket.setIntranetEndpoint(e.getChildText("IntranetEndpoint"));
                    if (e.getChild("Region") != null) {
                        bucket.setRegion(e.getChildText("Region"));
                    }
                    if (e.getChild("HierarchicalNamespace") != null) {
                        bucket.setHnsStatus(e.getChildText("HierarchicalNamespace"));
                    }
					if (e.getChild("ResourceGroupId") != null) {
						bucket.setResourceGroupId(e.getChildText("ResourceGroupId"));
					}
                    buckets.add(bucket);
                }
            }
            bucketList.setBucketList(buckets);

            return bucketList;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall list image style response body to style list.
     */
    @SuppressWarnings("unchecked")
    public static List<Style> parseListImageStyle(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            List<Style> styleList = new ArrayList<Style>();
            List<Element> styleElems = root.getChildren("Style");
            for (Element e : styleElems) {
                Style style = new Style();
                style.SetStyleName(e.getChildText("Name"));
                style.SetStyle(e.getChildText("Content"));
                style.SetLastModifyTime(DateUtil.parseRfc822Date(e.getChildText("LastModifyTime")));
                style.SetCreationDate(DateUtil.parseRfc822Date(e.getChildText("CreateTime")));
                styleList.add(style);
            }
            return styleList;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucket location response body to bucket location.
     */
    public static String parseGetBucketLocation(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            return root.getText();
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall bucket metadata from response headers.
     */
    public static BucketMetadata parseBucketMetadata(Map<String, String> headers) throws ResponseParseException {

        try {
            BucketMetadata bucketMetadata = new BucketMetadata();

            for (Iterator<String> it = headers.keySet().iterator(); it.hasNext();) {
                String key = it.next();

                if (key.equalsIgnoreCase(OSSHeaders.OSS_BUCKET_REGION)) {
                    bucketMetadata.setBucketRegion(headers.get(key));
                } else {
                    bucketMetadata.addHttpMetadata(key, headers.get(key));
                }
            }

            return bucketMetadata;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall simplified object meta from response headers.
     */
    public static SimplifiedObjectMeta parseSimplifiedObjectMeta(Map<String, String> headers)
            throws ResponseParseException {

        try {
            SimplifiedObjectMeta objectMeta = new SimplifiedObjectMeta();

            for (Iterator<String> it = headers.keySet().iterator(); it.hasNext();) {
                String key = it.next();

                if (key.equalsIgnoreCase(OSSHeaders.LAST_MODIFIED)) {
                    try {
                        objectMeta.setLastModified(DateUtil.parseRfc822Date(headers.get(key)));
                    } catch (ParseException pe) {
                        throw new ResponseParseException(pe.getMessage(), pe);
                    }
                } else if (key.equalsIgnoreCase(OSSHeaders.CONTENT_LENGTH)) {
                    Long value = Long.valueOf(headers.get(key));
                    objectMeta.setSize(value);
                } else if (key.equalsIgnoreCase(OSSHeaders.ETAG)) {
                    objectMeta.setETag(trimQuotes(headers.get(key)));
                } else if (key.equalsIgnoreCase(OSSHeaders.OSS_HEADER_REQUEST_ID)) {
                    objectMeta.setRequestId(headers.get(key));
                } else if (key.equalsIgnoreCase(OSSHeaders.OSS_HEADER_VERSION_ID)) {
                    objectMeta.setVersionId(headers.get(key));
                }
                objectMeta.setHeader(key, headers.get(key));
            }

            return objectMeta;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall symlink link from response headers.
     */
    public static OSSSymlink parseSymbolicLink(ResponseMessage response) throws ResponseParseException {

        try {
            OSSSymlink smyLink = null;

            String targetObject = response.getHeaders().get(OSSHeaders.OSS_HEADER_SYMLINK_TARGET);
            if (targetObject != null) {
                targetObject = HttpUtil.urlDecode(targetObject, "UTF-8");
                smyLink = new OSSSymlink(null, targetObject);
            }

            smyLink.setMetadata(parseObjectMetadata(response.getHeaders()));

            return smyLink;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall object metadata from response headers.
     */
    public static ObjectMetadata parseObjectMetadata(Map<String, String> headers) throws ResponseParseException {

        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();

            for (Iterator<String> it = headers.keySet().iterator(); it.hasNext();) {
                String key = it.next();

                if (key.indexOf(OSSHeaders.OSS_USER_METADATA_PREFIX) >= 0) {
                    key = key.substring(OSSHeaders.OSS_USER_METADATA_PREFIX.length());
                    objectMetadata.addUserMetadata(key, headers.get(OSSHeaders.OSS_USER_METADATA_PREFIX + key));
                } else if (key.equalsIgnoreCase(OSSHeaders.LAST_MODIFIED)||key.equalsIgnoreCase(OSSHeaders.DATE)) {
                    try {
                        objectMetadata.setHeader(key, DateUtil.parseRfc822Date(headers.get(key)));
                    } catch (ParseException pe) {
                        throw new ResponseParseException(pe.getMessage(), pe);
                    }
                } else if (key.equalsIgnoreCase(OSSHeaders.CONTENT_LENGTH)) {
                    Long value = Long.valueOf(headers.get(key));
                    objectMetadata.setHeader(key, value);
                } else if (key.equalsIgnoreCase(OSSHeaders.ETAG)) {
                    objectMetadata.setHeader(key, trimQuotes(headers.get(key)));
                } else {
                    objectMetadata.setHeader(key, headers.get(key));
                }
            }

            return objectMetadata;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall initiate multipart upload response body to corresponding
     * result.
     */
    public static InitiateMultipartUploadResult parseInitiateMultipartUpload(InputStream responseBody)
            throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
            if (root.getChild("Bucket") != null) {
                result.setBucketName(root.getChildText("Bucket"));
            }

            if (root.getChild("Key") != null) {
                result.setKey(root.getChildText("Key"));
            }

            if (root.getChild("UploadId") != null) {
                result.setUploadId(root.getChildText("UploadId"));
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall list multipart uploads response body to multipart upload
     * listing.
     */
    @SuppressWarnings("unchecked")
    public static MultipartUploadListing parseListMultipartUploads(InputStream responseBody)
            throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            MultipartUploadListing multipartUploadListing = new MultipartUploadListing();
            multipartUploadListing.setBucketName(root.getChildText("Bucket"));
            multipartUploadListing.setMaxUploads(Integer.valueOf(root.getChildText("MaxUploads")));
            multipartUploadListing.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));

            if (root.getChild("Delimiter") != null) {
                String delimiter = root.getChildText("Delimiter");
                if (!isNullOrEmpty(delimiter)) {
                    multipartUploadListing.setDelimiter(delimiter);
                }
            }

            if (root.getChild("Prefix") != null) {
                String prefix = root.getChildText("Prefix");
                if (!isNullOrEmpty(prefix)) {
                    multipartUploadListing.setPrefix(prefix);
                }
            }

            if (root.getChild("KeyMarker") != null) {
                String keyMarker = root.getChildText("KeyMarker");
                if (!isNullOrEmpty(keyMarker)) {
                    multipartUploadListing.setKeyMarker(keyMarker);
                }
            }

            if (root.getChild("UploadIdMarker") != null) {
                String uploadIdMarker = root.getChildText("UploadIdMarker");
                if (!isNullOrEmpty(uploadIdMarker)) {
                    multipartUploadListing.setUploadIdMarker(uploadIdMarker);
                }
            }

            if (root.getChild("NextKeyMarker") != null) {
                String nextKeyMarker = root.getChildText("NextKeyMarker");
                if (!isNullOrEmpty(nextKeyMarker)) {
                    multipartUploadListing.setNextKeyMarker(nextKeyMarker);
                }
            }

            if (root.getChild("NextUploadIdMarker") != null) {
                String nextUploadIdMarker = root.getChildText("NextUploadIdMarker");
                if (!isNullOrEmpty(nextUploadIdMarker)) {
                    multipartUploadListing.setNextUploadIdMarker(nextUploadIdMarker);
                }
            }

            List<Element> uploadElems = root.getChildren("Upload");
            for (Element elem : uploadElems) {
                if (elem.getChild("Initiated") == null) {
                    continue;
                }

                MultipartUpload mu = new MultipartUpload();
                mu.setKey(elem.getChildText("Key"));
                mu.setUploadId(elem.getChildText("UploadId"));
                mu.setStorageClass(elem.getChildText("StorageClass"));
                mu.setInitiated(DateUtil.parseIso8601Date(elem.getChildText("Initiated")));
                multipartUploadListing.addMultipartUpload(mu);
            }

            List<Element> commonPrefixesElems = root.getChildren("CommonPrefixes");
            for (Element elem : commonPrefixesElems) {
                String prefix = elem.getChildText("Prefix");
                if (!isNullOrEmpty(prefix)) {
                    multipartUploadListing.addCommonPrefix(prefix);
                }
            }

            return multipartUploadListing;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall list parts response body to part listing.
     */
    @SuppressWarnings("unchecked")
    public static PartListing parseListParts(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            PartListing partListing = new PartListing();
            partListing.setBucketName(root.getChildText("Bucket"));
            partListing.setKey(root.getChildText("Key"));
            partListing.setUploadId(root.getChildText("UploadId"));
            partListing.setStorageClass(root.getChildText("StorageClass"));
            partListing.setMaxParts(Integer.valueOf(root.getChildText("MaxParts")));
            partListing.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));

            if (root.getChild("PartNumberMarker") != null) {
                String partNumberMarker = root.getChildText("PartNumberMarker");
                if (!isNullOrEmpty(partNumberMarker)) {
                    partListing.setPartNumberMarker(Integer.valueOf(partNumberMarker));
                }
            }

            if (root.getChild("NextPartNumberMarker") != null) {
                String nextPartNumberMarker = root.getChildText("NextPartNumberMarker");
                if (!isNullOrEmpty(nextPartNumberMarker)) {
                    partListing.setNextPartNumberMarker(Integer.valueOf(nextPartNumberMarker));
                }
            }

            List<Element> partElems = root.getChildren("Part");
            for (Element elem : partElems) {
                PartSummary ps = new PartSummary();

                ps.setPartNumber(Integer.valueOf(elem.getChildText("PartNumber")));
                ps.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));
                ps.setETag(trimQuotes(elem.getChildText("ETag")));
                ps.setSize(Integer.valueOf(elem.getChildText("Size")));

                partListing.addPart(ps);
            }

            return partListing;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall complete multipart upload response body to corresponding
     * result.
     */
    public static CompleteMultipartUploadResult parseCompleteMultipartUpload(InputStream responseBody)
            throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            CompleteMultipartUploadResult result = new CompleteMultipartUploadResult();
            result.setBucketName(root.getChildText("Bucket"));
            result.setETag(trimQuotes(root.getChildText("ETag")));
            result.setKey(root.getChildText("Key"));
            result.setLocation(root.getChildText("Location"));

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket logging response body to corresponding result.
     */
    public static BucketLoggingResult parseBucketLogging(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            BucketLoggingResult result = new BucketLoggingResult();
            if (root.getChild("LoggingEnabled") != null) {
                result.setTargetBucket(root.getChild("LoggingEnabled").getChildText("TargetBucket"));
            }
            if (root.getChild("LoggingEnabled") != null) {
                result.setTargetPrefix(root.getChild("LoggingEnabled").getChildText("TargetPrefix"));
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket image response body to corresponding result.
     */
    public static GetBucketImageResult parseBucketImage(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetBucketImageResult result = new GetBucketImageResult();
            result.SetBucketName(root.getChildText("Name"));
            result.SetDefault404Pic(root.getChildText("Default404Pic"));
            result.SetStyleDelimiters(root.getChildText("StyleDelimiters"));
            result.SetStatus(root.getChildText("Status"));
            result.SetIsAutoSetContentType(root.getChildText("AutoSetContentType").equals("True"));
            result.SetIsForbidOrigPicAccess(root.getChildText("OrigPicForbidden").equals("True"));
            result.SetIsSetAttachName(root.getChildText("SetAttachName").equals("True"));
            result.SetIsUseStyleOnly(root.getChildText("UseStyleOnly").equals("True"));
            result.SetIsUseSrcFormat(root.getChildText("UseSrcFormat").equals("True"));
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get image style response body to corresponding result.
     */
    public static GetImageStyleResult parseImageStyle(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetImageStyleResult result = new GetImageStyleResult();
            result.SetStyleName(root.getChildText("Name"));
            result.SetStyle(root.getChildText("Content"));
            result.SetLastModifyTime(DateUtil.parseRfc822Date(root.getChildText("LastModifyTime")));
            result.SetCreationDate(DateUtil.parseRfc822Date(root.getChildText("CreateTime")));
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket process response body to bucket process.
     */
    public static BucketProcess parseGetBucketImageProcessConf(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            String compliedHost = root.getChildText("CompliedHost");
            boolean sourceFileProtect = false;
            if (root.getChildText("SourceFileProtect").equals("Enabled")) {
                sourceFileProtect = true;
            }
            String sourceFileProtectSuffix = root.getChildText("SourceFileProtectSuffix");
            String styleDelimiters = root.getChildText("StyleDelimiters");

            ImageProcess imageProcess = new ImageProcess(compliedHost, sourceFileProtect, sourceFileProtectSuffix,
                    styleDelimiters);
            if (root.getChildText("Version") != null) {
                imageProcess.setVersion(Integer.parseInt(root.getChildText("Version")));
            }

            return new BucketProcess(imageProcess);
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket website response body to corresponding result.
     */
    @SuppressWarnings("unchecked")
    public static BucketWebsiteResult parseBucketWebsite(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            BucketWebsiteResult result = new BucketWebsiteResult();
            Element indexDocument = root.getChild("IndexDocument");
            if (indexDocument != null) {
                result.setIndexDocument(root.getChild("IndexDocument").getChildText("Suffix"));
                if (indexDocument.getChild("SupportSubDir") != null) {
                    result.setSupportSubDir(Boolean.valueOf(indexDocument.getChildText("SupportSubDir")));
                }

                if (indexDocument.getChild("Type") != null) {
                    result.setSubDirType(indexDocument.getChildText("Type"));
                }
            }
            if (root.getChild("ErrorDocument") != null) {
                result.setErrorDocument(root.getChild("ErrorDocument").getChildText("Key"));
            }
            if (root.getChild("RoutingRules") != null) {
                List<Element> ruleElements = root.getChild("RoutingRules").getChildren("RoutingRule");
                for (Element ruleElem : ruleElements) {
                    RoutingRule rule = new RoutingRule();

                    rule.setNumber(Integer.parseInt(ruleElem.getChildText("RuleNumber")));

                    Element condElem = ruleElem.getChild("Condition");
                    if (condElem != null) {
                        rule.getCondition().setKeyPrefixEquals(condElem.getChildText("KeyPrefixEquals"));
                        //rule set KeySuffixEquals
                        if (condElem.getChild("KeySuffixEquals") != null) {
                            rule.getCondition().setKeySuffixEquals(condElem.getChildText("KeySuffixEquals"));
                        }
                        if (condElem.getChild("HttpErrorCodeReturnedEquals") != null) {
                            rule.getCondition().setHttpErrorCodeReturnedEquals(
                                    Integer.parseInt(condElem.getChildText("HttpErrorCodeReturnedEquals")));
                        }
                        List<Element> includeHeadersElem = condElem.getChildren("IncludeHeader");
                        if (includeHeadersElem != null && includeHeadersElem.size() > 0) {
                            List<RoutingRule.IncludeHeader> includeHeaders = new ArrayList<RoutingRule.IncludeHeader>();
                            for (Element includeHeaderElem : includeHeadersElem) {
                                RoutingRule.IncludeHeader includeHeader = new RoutingRule.IncludeHeader();
                                includeHeader.setKey(includeHeaderElem.getChildText("Key"));
                                includeHeader.setEquals(includeHeaderElem.getChildText("Equals"));
                                includeHeader.setStartsWith(includeHeaderElem.getChildText("StartsWith"));
                                includeHeader.setEndsWith(includeHeaderElem.getChildText("EndsWith"));
                                includeHeaders.add(includeHeader);
                            }
                            rule.getCondition().setIncludeHeaders(includeHeaders);
                        }
                    }

                    Element redirectElem = ruleElem.getChild("Redirect");
                    if (redirectElem.getChild("RedirectType") != null) {
                        rule.getRedirect().setRedirectType(
                                RoutingRule.RedirectType.parse(redirectElem.getChildText("RedirectType")));
                    }
                    rule.getRedirect().setHostName(redirectElem.getChildText("HostName"));
                    if (redirectElem.getChild("Protocol") != null) {
                        rule.getRedirect()
                                .setProtocol(RoutingRule.Protocol.parse(redirectElem.getChildText("Protocol")));
                    }
                    rule.getRedirect().setReplaceKeyPrefixWith(redirectElem.getChildText("ReplaceKeyPrefixWith"));
                    rule.getRedirect().setReplaceKeyWith(redirectElem.getChildText("ReplaceKeyWith"));
                    if (redirectElem.getChild("HttpRedirectCode") != null) {
                        rule.getRedirect()
                                .setHttpRedirectCode(Integer.parseInt(redirectElem.getChildText("HttpRedirectCode")));
                    }
                    rule.getRedirect().setMirrorURL(redirectElem.getChildText("MirrorURL"));
                    if (redirectElem.getChildText("MirrorTunnelId") != null) {
                        rule.getRedirect().setMirrorTunnelId(redirectElem.getChildText("MirrorTunnelId"));
                    }
                    rule.getRedirect().setMirrorSecondaryURL(redirectElem.getChildText("MirrorURLSlave"));
                    rule.getRedirect().setMirrorProbeURL(redirectElem.getChildText("MirrorURLProbe"));
                    if (redirectElem.getChildText("MirrorPassQueryString") != null) {
                        rule.getRedirect().setMirrorPassQueryString(
                                Boolean.valueOf(redirectElem.getChildText("MirrorPassQueryString")));
                    }
                    if (redirectElem.getChildText("MirrorPassOriginalSlashes") != null) {
                        rule.getRedirect().setPassOriginalSlashes(
                                Boolean.valueOf(redirectElem.getChildText("MirrorPassOriginalSlashes")));
                    }

                    if (redirectElem.getChildText("PassQueryString") != null) {
                        rule.getRedirect().setPassQueryString(
                                Boolean.valueOf(redirectElem.getChildText("PassQueryString")));
                    }
                    if (redirectElem.getChildText("MirrorFollowRedirect") != null) {
                        rule.getRedirect().setMirrorFollowRedirect(
                                Boolean.valueOf(redirectElem.getChildText("MirrorFollowRedirect")));
                    }
                    if (redirectElem.getChildText("MirrorUserLastModified") != null) {
                        rule.getRedirect().setMirrorUserLastModified(
                                Boolean.valueOf(redirectElem.getChildText("MirrorUserLastModified")));
                    }
                    if (redirectElem.getChildText("MirrorIsExpressTunnel") != null) {
                        rule.getRedirect().setMirrorIsExpressTunnel(
                                Boolean.valueOf(redirectElem.getChildText("MirrorIsExpressTunnel")));
                    }
                    if (redirectElem.getChildText("MirrorDstRegion") != null) {
                        rule.getRedirect().setMirrorDstRegion(
                                redirectElem.getChildText("MirrorDstRegion"));
                    }
                    if (redirectElem.getChildText("MirrorDstVpcId") != null) {
                        rule.getRedirect().setMirrorDstVpcId(
                                redirectElem.getChildText("MirrorDstVpcId"));
                    }
                    if (redirectElem.getChildText("MirrorUsingRole") != null) {
                        rule.getRedirect().setMirrorUsingRole(Boolean.valueOf(redirectElem.getChildText("MirrorUsingRole")));
                    }

                    if (redirectElem.getChildText("MirrorRole") != null) {
                        rule.getRedirect().setMirrorRole(redirectElem.getChildText("MirrorRole"));
                    }

                    // EnableReplacePrefix and MirrorSwitchAllErrors
                    if (redirectElem.getChild("EnableReplacePrefix") != null) {
                        rule.getRedirect().setEnableReplacePrefix(Boolean.valueOf(redirectElem.getChildText("EnableReplacePrefix")));
                    }

                    if (redirectElem.getChild("MirrorSwitchAllErrors") != null) {
                        rule.getRedirect().setMirrorSwitchAllErrors(Boolean.valueOf(redirectElem.getChildText("MirrorSwitchAllErrors")));
                    }

                    if (redirectElem.getChild("MirrorCheckMd5") != null) {
                        rule.getRedirect().setMirrorCheckMd5(Boolean.valueOf(redirectElem.getChildText("MirrorCheckMd5")));
                    }

                    Element mirrorURLsElem = redirectElem.getChild("MirrorMultiAlternates");
                    if (mirrorURLsElem != null) {
                        List<Element> mirrorURLElementList = mirrorURLsElem.getChildren("MirrorMultiAlternate");
                        if (mirrorURLElementList != null && mirrorURLElementList.size() > 0) {
                            List<RoutingRule.Redirect.MirrorMultiAlternate> mirrorURLsList = new ArrayList<RoutingRule.Redirect.MirrorMultiAlternate>();
                            for (Element setElement : mirrorURLElementList) {
                                RoutingRule.Redirect.MirrorMultiAlternate mirrorMultiAlternate = new RoutingRule.Redirect.MirrorMultiAlternate();
                                mirrorMultiAlternate.setPrior(Integer.parseInt(setElement.getChildText("MirrorMultiAlternateNumber")));
                                mirrorMultiAlternate.setUrl(setElement.getChildText("MirrorMultiAlternateURL"));
                                mirrorURLsList.add(mirrorMultiAlternate);
                            }
                            rule.getRedirect().setMirrorMultiAlternates(mirrorURLsList);
                        }
                    }

                    Element mirrorHeadersElem = redirectElem.getChild("MirrorHeaders");
                    if (mirrorHeadersElem != null) {
                        RoutingRule.MirrorHeaders mirrorHeaders = new RoutingRule.MirrorHeaders();
                        mirrorHeaders.setPassAll(Boolean.valueOf(mirrorHeadersElem.getChildText("PassAll")));
                        mirrorHeaders.setPass(parseStringListFromElemet(mirrorHeadersElem.getChildren("Pass")));
                        mirrorHeaders.setRemove(parseStringListFromElemet(mirrorHeadersElem.getChildren("Remove")));
                        List<Element> setElementList = mirrorHeadersElem.getChildren("Set");
                        if (setElementList != null && setElementList.size() > 0) {
                            List<Map<String, String>> setList = new ArrayList<Map<String, String>>();
                            for (Element setElement : setElementList) {
                                Map<String, String> map = new HashMap<String, String>();
                                map.put("Key", setElement.getChildText("Key"));
                                map.put("Value", setElement.getChildText("Value"));
                                setList.add(map);
                            }
                            mirrorHeaders.setSet(setList);
                        }

                        rule.getRedirect().setMirrorHeaders(mirrorHeaders);
                    }

                    result.AddRoutingRule(rule);
                }
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * 转换ElementList to StringList
     *
     * @param elementList
     * @return
     */
    private static List<String> parseStringListFromElemet(List<Element> elementList) {
        if (elementList != null && elementList.size() > 0) {
            List<String> list = new ArrayList<String>();
            for (Element element : elementList) {
                list.add(element.getText());
            }
            return list;
        }
        return null;
    }
    /**
     * Unmarshall copy object response body to corresponding result.
     */
    public static CopyObjectResult parseCopyObjectResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            CopyObjectResult result = new CopyObjectResult();
            result.setLastModified(DateUtil.parseIso8601Date(root.getChildText("LastModified")));
            result.setEtag(trimQuotes(root.getChildText("ETag")));

            return result;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall delete objects response body to corresponding result.
     */
    @SuppressWarnings("unchecked")
    public static DeleteObjectsResult parseDeleteObjectsResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            DeleteObjectsResult deleteObjectsResult = new DeleteObjectsResult();
            if (root.getChild("EncodingType") != null) {
                String encodingType = root.getChildText("EncodingType");
                deleteObjectsResult.setEncodingType(isNullOrEmpty(encodingType) ? null : encodingType);
            }

            List<String> deletedObjects = new ArrayList<String>();
            List<Element> deletedElements = root.getChildren("Deleted");
            for (Element elem : deletedElements) {
                deletedObjects.add(elem.getChildText("Key"));
            }
            deleteObjectsResult.setDeletedObjects(deletedObjects);

            return deleteObjectsResult;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }
    
    /**
     * Unmarshall delete versions response body to corresponding result.
     */
    @SuppressWarnings("unchecked")
    public static DeleteVersionsResult parseDeleteVersionsResult(InputStream responseBody)
        throws ResponseParseException {
        boolean shouldSDKDecodeResponse = false;

        try {
            Element root = getXmlRootElement(responseBody);

            if (root.getChild("EncodingType") != null) {
                String encodingType = root.getChildText("EncodingType");
                shouldSDKDecodeResponse = OSSConstants.URL_ENCODING.equals(encodingType);
            }

            List<DeletedVersion> deletedVersions = new ArrayList<DeletedVersion>();
            List<Element> deletedElements = root.getChildren("Deleted");
            for (Element elem : deletedElements) {
                DeletedVersion key = new DeletedVersion();

                if (shouldSDKDecodeResponse) {
                    key.setKey(HttpUtil.urlDecode(elem.getChildText("Key"), StringUtils.DEFAULT_ENCODING));
                } else {
                    key.setKey(elem.getChildText("Key"));
                }

                if (elem.getChild("VersionId") != null) {
                    key.setVersionId(elem.getChildText("VersionId"));
                }

                if (elem.getChild("DeleteMarker") != null) {
                    key.setDeleteMarker(Boolean.parseBoolean(elem.getChildText("DeleteMarker")));
                }

                if (elem.getChild("DeleteMarkerVersionId") != null) {
                    key.setDeleteMarkerVersionId(elem.getChildText("DeleteMarkerVersionId"));
                }

                deletedVersions.add(key);
            }

            return new DeleteVersionsResult(deletedVersions);
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket cors response body to cors rules.
     */
    @SuppressWarnings("unchecked")
    public static CORSConfiguration parseListBucketCORS(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            CORSConfiguration result = new CORSConfiguration();
            List<Element> corsRuleElems = root.getChildren("CORSRule");

            for (Element corsRuleElem : corsRuleElems) {
                CORSRule rule = new CORSRule();

                List<Element> allowedOriginElems = corsRuleElem.getChildren("AllowedOrigin");
                for (Element allowedOriginElement : allowedOriginElems) {
                    rule.getAllowedOrigins().add(allowedOriginElement.getValue());
                }

                List<Element> allowedMethodElems = corsRuleElem.getChildren("AllowedMethod");
                for (Element allowedMethodElement : allowedMethodElems) {
                    rule.getAllowedMethods().add(allowedMethodElement.getValue());
                }

                List<Element> allowedHeaderElems = corsRuleElem.getChildren("AllowedHeader");
                for (Element allowedHeaderElement : allowedHeaderElems) {
                    rule.getAllowedHeaders().add(allowedHeaderElement.getValue());
                }

                List<Element> exposeHeaderElems = corsRuleElem.getChildren("ExposeHeader");
                for (Element exposeHeaderElement : exposeHeaderElems) {
                    rule.getExposeHeaders().add(exposeHeaderElement.getValue());
                }

                Element maxAgeSecondsElem = corsRuleElem.getChild("MaxAgeSeconds");
                if (maxAgeSecondsElem != null) {
                    rule.setMaxAgeSeconds(Integer.parseInt(maxAgeSecondsElem.getValue()));
                }
                result.getCorsRules().add(rule);
            }

            Element responseVaryElems = root.getChild("ResponseVary");
            if (responseVaryElems != null) {
                result.setResponseVary(Boolean.parseBoolean(responseVaryElems.getValue()));
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket tagging response body to cors rules.
     */
    @SuppressWarnings("unchecked")
    public static TagSet parseGetBucketTagging(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            TagSet tagSet = new TagSet();
            List<Element> tagElems = root.getChild("TagSet").getChildren("Tag");

            for (Element tagElem : tagElems) {
                String key = null;
                String value = null;

                if (tagElem.getChild("Key") != null) {
                    key = tagElem.getChildText("Key");
                }

                if (tagElem.getChild("Value") != null) {
                    value = tagElem.getChildText("Value");
                }

                tagSet.setTag(key, value);
            }

            return tagSet;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket replication response body to replication result.
     */
    @SuppressWarnings("unchecked")
    public static List<ReplicationRule> parseGetBucketReplication(InputStream responseBody)
            throws ResponseParseException {

        try {
            List<ReplicationRule> repRules = new ArrayList<ReplicationRule>();

            Element root = getXmlRootElement(responseBody);
            List<Element> ruleElems = root.getChildren("Rule");

            for (Element ruleElem : ruleElems) {
                ReplicationRule repRule = new ReplicationRule();

                repRule.setReplicationRuleID(ruleElem.getChildText("ID"));

                Element destination = ruleElem.getChild("Destination");
                repRule.setTargetBucketName(destination.getChildText("Bucket"));
                repRule.setTargetBucketLocation(destination.getChildText("Location"));

                repRule.setTargetCloud(destination.getChildText("Cloud"));
                repRule.setTargetCloudLocation(destination.getChildText("CloudLocation"));

                repRule.setReplicationStatus(ReplicationStatus.parse(ruleElem.getChildText("Status")));

                if (ruleElem.getChildText("HistoricalObjectReplication").equals("enabled")) {
                    repRule.setEnableHistoricalObjectReplication(true);
                } else {
                    repRule.setEnableHistoricalObjectReplication(false);
                }

                if (ruleElem.getChild("PrefixSet") != null) {
                    List<String> objectPrefixes = new ArrayList<String>();
                    List<Element> prefixElems = ruleElem.getChild("PrefixSet").getChildren("Prefix");
                    for (Element prefixElem : prefixElems) {
                        objectPrefixes.add(prefixElem.getText());
                    }
                    repRule.setObjectPrefixList(objectPrefixes);
                }

                if (ruleElem.getChild("Action") != null) {
                    String[] actionStrs = ruleElem.getChildText("Action").split(",");
                    List<ReplicationAction> repActions = new ArrayList<ReplicationAction>();
                    for (String actionStr : actionStrs) {
                        repActions.add(ReplicationAction.parse(actionStr));
                    }
                    repRule.setReplicationActionList(repActions);
                }

                repRule.setSyncRole(ruleElem.getChildText("SyncRole"));

                if (ruleElem.getChild("EncryptionConfiguration") != null){
                    repRule.setReplicaKmsKeyID(ruleElem.getChild("EncryptionConfiguration").getChildText("ReplicaKmsKeyID"));
                }

                if (ruleElem.getChild("SourceSelectionCriteria") != null &&
                    ruleElem.getChild("SourceSelectionCriteria").getChild("SseKmsEncryptedObjects") != null) {
                    repRule.setSseKmsEncryptedObjectsStatus(ruleElem.getChild("SourceSelectionCriteria").
                            getChild("SseKmsEncryptedObjects").getChildText("Status"));
                }

                if (ruleElem.getChild("Source") != null){
                    repRule.setSourceBucketLocation(ruleElem.getChild("Source").getChildText("Location"));
                }

                repRules.add(repRule);
            }

            return repRules;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket replication response body to replication progress.
     */
    public static BucketReplicationProgress parseGetBucketReplicationProgress(InputStream responseBody)
            throws ResponseParseException {
        try {
            BucketReplicationProgress progress = new BucketReplicationProgress();

            Element root = getXmlRootElement(responseBody);
            Element ruleElem = root.getChild("Rule");

            progress.setReplicationRuleID(ruleElem.getChildText("ID"));

            Element destination = ruleElem.getChild("Destination");
            progress.setTargetBucketName(destination.getChildText("Bucket"));
            progress.setTargetBucketLocation(destination.getChildText("Location"));
            progress.setTargetCloud(destination.getChildText("Cloud"));
            progress.setTargetCloudLocation(destination.getChildText("CloudLocation"));

            progress.setReplicationStatus(ReplicationStatus.parse(ruleElem.getChildText("Status")));

            if (ruleElem.getChildText("HistoricalObjectReplication").equals("enabled")) {
                progress.setEnableHistoricalObjectReplication(true);
            } else {
                progress.setEnableHistoricalObjectReplication(false);
            }

            Element progressElem = ruleElem.getChild("Progress");
            if (progressElem != null) {
                if (progressElem.getChild("HistoricalObject") != null) {
                    progress.setHistoricalObjectProgress(
                            Float.parseFloat(progressElem.getChildText("HistoricalObject")));
                }
                progress.setNewObjectProgress(DateUtil.parseIso8601Date(progressElem.getChildText("NewObject")));
            }

            return progress;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket replication response body to replication location.
     */
    @SuppressWarnings("unchecked")
    public static List<String> parseGetBucketReplicationLocation(InputStream responseBody)
            throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);

            List<String> locationList = new ArrayList<String>();
            List<Element> locElements = root.getChildren("Location");

            for (Element locElem : locElements) {
                locationList.add(locElem.getText());
            }

            return locationList;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket info response body to bucket info.
     */
    public static BucketInfo parseGetBucketInfo(InputStream responseBody) throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);
            Element bucketElem = root.getChild("Bucket");
            BucketInfo bucketInfo = new BucketInfo();

            // owner
            Bucket bucket = new Bucket();
            String id = bucketElem.getChild("Owner").getChildText("ID");
            String displayName = bucketElem.getChild("Owner").getChildText("DisplayName");
            Owner owner = new Owner(id, displayName);
            bucket.setOwner(owner);

            // bucket
            bucket.setName(bucketElem.getChildText("Name"));
            bucket.setLocation(bucketElem.getChildText("Location"));
            bucket.setExtranetEndpoint(bucketElem.getChildText("ExtranetEndpoint"));
            bucket.setIntranetEndpoint(bucketElem.getChildText("IntranetEndpoint"));
            bucket.setCreationDate(DateUtil.parseIso8601Date(bucketElem.getChildText("CreationDate")));

            if (bucketElem.getChild("HierarchicalNamespace") != null) {
                String hnsStatus = bucketElem.getChildText("HierarchicalNamespace");
                bucket.setHnsStatus(hnsStatus);
            }

			if (bucketElem.getChild("ResourceGroupId") != null) {
                String resourceGroupId = bucketElem.getChildText("ResourceGroupId");
                bucket.setResourceGroupId(resourceGroupId);
            }

            if (bucketElem.getChild("StorageClass") != null) {
                bucket.setStorageClass(StorageClass.parse(bucketElem.getChildText("StorageClass")));
            }
            if (bucketElem.getChild("AccessMonitor") != null) {
                bucket.setAccessMonitor(bucketElem.getChildText("AccessMonitor"));
            }

            if (bucketElem.getChild("BucketPolicy") != null) {
                Element policyElem = bucketElem.getChild("BucketPolicy");
                if (policyElem.getChild("XCType") != null) {
                    bucket.setXcType(policyElem.getChildText("XCType"));
                }
            }
            bucketInfo.setBucket(bucket);

            // comment
            if (bucketElem.getChild("Comment") != null) {
                String comment = bucketElem.getChildText("Comment");
                bucketInfo.setComment(comment);
            }

            // data redundancy type
            if (bucketElem.getChild("DataRedundancyType") != null) {
                String dataRedundancyString = bucketElem.getChildText("DataRedundancyType");
                DataRedundancyType dataRedundancyType = DataRedundancyType.parse(dataRedundancyString);
                bucketInfo.setDataRedundancyType(dataRedundancyType);
            }

            // acl
            String aclString = bucketElem.getChild("AccessControlList").getChildText("Grant");
            CannedAccessControlList acl = CannedAccessControlList.parse(aclString);
            bucketInfo.setCannedACL(acl);
            switch (acl) {
            case PublicRead:
                bucketInfo.grantPermission(GroupGrantee.AllUsers, Permission.Read);
                break;
            case PublicReadWrite:
                bucketInfo.grantPermission(GroupGrantee.AllUsers, Permission.FullControl);
                break;
            default:
                break;
            }

            // sse
            Element sseElem = bucketElem.getChild("ServerSideEncryptionRule");
            if (sseElem != null) {
                ServerSideEncryptionConfiguration serverSideEncryptionConfiguration =
                    new ServerSideEncryptionConfiguration();
                ServerSideEncryptionByDefault applyServerSideEncryptionByDefault = new ServerSideEncryptionByDefault();

                applyServerSideEncryptionByDefault.setSSEAlgorithm(sseElem.getChildText("SSEAlgorithm"));
                if (sseElem.getChild("KMSMasterKeyID") != null) {
                    applyServerSideEncryptionByDefault.setKMSMasterKeyID(sseElem.getChildText("KMSMasterKeyID"));
                }
                if (sseElem.getChild("KMSDataEncryption") != null) {
                    applyServerSideEncryptionByDefault.setKMSDataEncryption(sseElem.getChildText("KMSDataEncryption"));
                }
                serverSideEncryptionConfiguration
                    .setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);

                bucketInfo.setServerSideEncryptionConfiguration(serverSideEncryptionConfiguration);
            }

            return bucketInfo;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket info response body to bucket stat.
     */
    public static BucketStat parseGetBucketStat(InputStream responseBody) throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);
            Long storage = Long.parseLong(root.getChildText("Storage"));
            Long objectCount = Long.parseLong(root.getChildText("ObjectCount"));
            Long multipartUploadCount = Long.parseLong(root.getChildText("MultipartUploadCount"));
            Long liveChannelCount = parseLongWithDefault(root.getChildText("LiveChannelCount"));
            Long lastModifiedTime = parseLongWithDefault(root.getChildText("LastModifiedTime"));
            Long standardStorage = parseLongWithDefault(root.getChildText("StandardStorage"));
            Long standardObjectCount = parseLongWithDefault(root.getChildText("StandardObjectCount"));
            Long infrequentAccessStorage = parseLongWithDefault(root.getChildText("InfrequentAccessStorage"));
            Long infrequentAccessRealStorage = parseLongWithDefault(root.getChildText("InfrequentAccessRealStorage"));
            Long infrequentAccessObjectCount = parseLongWithDefault(root.getChildText("InfrequentAccessObjectCount"));
            Long archiveStorage = parseLongWithDefault(root.getChildText("ArchiveStorage"));
            Long archiveRealStorage = parseLongWithDefault(root.getChildText("ArchiveRealStorage"));
            Long archiveObjectCount = parseLongWithDefault(root.getChildText("ArchiveObjectCount"));
            Long coldArchiveStorage = parseLongWithDefault(root.getChildText("ColdArchiveStorage"));
            Long coldArchiveRealStorage = parseLongWithDefault(root.getChildText("ColdArchiveRealStorage"));
            Long coldArchiveObjectCount = parseLongWithDefault(root.getChildText("ColdArchiveObjectCount"));
            BucketStat bucketStat = new BucketStat()
                    .withStorageSize(storage)
                    .withObjectCount(objectCount)
                    .withMultipartUploadCount(multipartUploadCount)
                    .withLiveChannelCount(liveChannelCount)
                    .withLastModifiedTime(lastModifiedTime)
                    .withStandardStorage(standardStorage)
                    .withStandardObjectCount(standardObjectCount)
                    .withInfrequentAccessStorage(infrequentAccessStorage)
                    .withInfrequentAccessRealStorage(infrequentAccessRealStorage)
                    .withInfrequentAccessObjectCount(infrequentAccessObjectCount)
                    .withArchiveStorage(archiveStorage)
                    .withArchiveRealStorage(archiveRealStorage)
                    .withArchiveObjectCount(archiveObjectCount)
                    .withColdArchiveStorage(coldArchiveStorage)
                    .withColdArchiveRealStorage(coldArchiveRealStorage)
                    .withColdArchiveObjectCount(coldArchiveObjectCount);

            return bucketStat;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall create live channel response body to corresponding result.
     */
    @SuppressWarnings("unchecked")
    public static CreateLiveChannelResult parseCreateLiveChannel(InputStream responseBody)
            throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            CreateLiveChannelResult result = new CreateLiveChannelResult();

            List<String> publishUrls = new ArrayList<String>();
            List<Element> publishElems = root.getChild("PublishUrls").getChildren("Url");
            for (Element urlElem : publishElems) {
                publishUrls.add(urlElem.getText());
            }
            result.setPublishUrls(publishUrls);

            List<String> playUrls = new ArrayList<String>();
            List<Element> playElems = root.getChild("PlayUrls").getChildren("Url");
            for (Element urlElem : playElems) {
                playUrls.add(urlElem.getText());
            }
            result.setPlayUrls(playUrls);

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get live channel info response body to corresponding result.
     */
    public static LiveChannelInfo parseGetLiveChannelInfo(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            LiveChannelInfo result = new LiveChannelInfo();

            result.setDescription(root.getChildText("Description"));
            result.setStatus(LiveChannelStatus.parse(root.getChildText("Status")));

            Element targetElem = root.getChild("Target");
            LiveChannelTarget target = new LiveChannelTarget();
            target.setType(targetElem.getChildText("Type"));
            target.setFragDuration(Integer.parseInt(targetElem.getChildText("FragDuration")));
            target.setFragCount(Integer.parseInt(targetElem.getChildText("FragCount")));
            target.setPlaylistName(targetElem.getChildText("PlaylistName"));
            result.setTarget(target);

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get live channel stat response body to corresponding result.
     */
    public static LiveChannelStat parseGetLiveChannelStat(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            LiveChannelStat result = new LiveChannelStat();

            result.setPushflowStatus(PushflowStatus.parse(root.getChildText("Status")));

            if (root.getChild("ConnectedTime") != null) {
                result.setConnectedDate(DateUtil.parseIso8601Date(root.getChildText("ConnectedTime")));
            }

            if (root.getChild("RemoteAddr") != null) {
                result.setRemoteAddress(root.getChildText("RemoteAddr"));
            }

            Element videoElem = root.getChild("Video");
            if (videoElem != null) {
                VideoStat videoStat = new VideoStat();
                videoStat.setWidth(Integer.parseInt(videoElem.getChildText("Width")));
                videoStat.setHeight(Integer.parseInt(videoElem.getChildText("Height")));
                videoStat.setFrameRate(Integer.parseInt(videoElem.getChildText("FrameRate")));
                videoStat.setBandWidth(Integer.parseInt(videoElem.getChildText("Bandwidth")));
                videoStat.setCodec(videoElem.getChildText("Codec"));
                result.setVideoStat(videoStat);
            }

            Element audioElem = root.getChild("Audio");
            if (audioElem != null) {
                AudioStat audioStat = new AudioStat();
                audioStat.setBandWidth(Integer.parseInt(audioElem.getChildText("Bandwidth")));
                audioStat.setSampleRate(Integer.parseInt(audioElem.getChildText("SampleRate")));
                audioStat.setCodec(audioElem.getChildText("Codec"));
                result.setAudioStat(audioStat);
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get live channel history response body to corresponding
     * result.
     */
    @SuppressWarnings("unchecked")
    public static List<LiveRecord> parseGetLiveChannelHistory(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            List<LiveRecord> liveRecords = new ArrayList<LiveRecord>();
            List<Element> recordElements = root.getChildren("LiveRecord");

            for (Element recordElem : recordElements) {
                LiveRecord record = new LiveRecord();
                record.setStartDate(DateUtil.parseIso8601Date(recordElem.getChildText("StartTime")));
                record.setEndDate(DateUtil.parseIso8601Date(recordElem.getChildText("EndTime")));
                record.setRemoteAddress(recordElem.getChildText("RemoteAddr"));
                liveRecords.add(record);
            }

            return liveRecords;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall list live channels response body to live channel listing.
     */
    @SuppressWarnings("unchecked")
    public static LiveChannelListing parseListLiveChannels(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            LiveChannelListing liveChannelListing = new LiveChannelListing();
            liveChannelListing.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));

            if (root.getChild("Prefix") != null) {
                String prefix = root.getChildText("Prefix");
                liveChannelListing.setPrefix(isNullOrEmpty(prefix) ? null : prefix);
            }

            if (root.getChild("Marker") != null) {
                String marker = root.getChildText("Marker");
                liveChannelListing.setMarker(isNullOrEmpty(marker) ? null : marker);
            }

            if (root.getChild("MaxKeys") != null) {
                String maxKeys = root.getChildText("MaxKeys");
                liveChannelListing.setMaxKeys(Integer.valueOf(maxKeys));
            }

            if (root.getChild("NextMarker") != null) {
                String nextMarker = root.getChildText("NextMarker");
                liveChannelListing.setNextMarker(isNullOrEmpty(nextMarker) ? null : nextMarker);
            }

            List<Element> liveChannelElems = root.getChildren("LiveChannel");
            for (Element elem : liveChannelElems) {
                LiveChannel liveChannel = new LiveChannel();

                liveChannel.setName(elem.getChildText("Name"));
                liveChannel.setDescription(elem.getChildText("Description"));
                liveChannel.setStatus(LiveChannelStatus.parse(elem.getChildText("Status")));
                liveChannel.setLastModified(DateUtil.parseIso8601Date(elem.getChildText("LastModified")));

                List<String> publishUrls = new ArrayList<String>();
                List<Element> publishElems = elem.getChild("PublishUrls").getChildren("Url");
                for (Element urlElem : publishElems) {
                    publishUrls.add(urlElem.getText());
                }
                liveChannel.setPublishUrls(publishUrls);

                List<String> playUrls = new ArrayList<String>();
                List<Element> playElems = elem.getChild("PlayUrls").getChildren("Url");
                for (Element urlElem : playElems) {
                    playUrls.add(urlElem.getText());
                }
                liveChannel.setPlayUrls(playUrls);

                liveChannelListing.addLiveChannel(liveChannel);
            }

            return liveChannelListing;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get user qos response body to user qos.
     */
    public static UserQos parseGetUserQos(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            UserQos userQos = new UserQos();

            if (root.getChild("StorageCapacity") != null) {
                userQos.setStorageCapacity(Integer.parseInt(root.getChildText("StorageCapacity")));
            }

            return userQos;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }
    
    /**
     * Unmarshall get bucket versioning response body to versioning configuration.
     */
    public static BucketVersioningConfiguration parseGetBucketVersioning(InputStream responseBody)
        throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            BucketVersioningConfiguration configuration = new BucketVersioningConfiguration();

            configuration.setStatus(root.getChildText("Status"));

            return configuration;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucket encryption response body to encryption configuration.
     */
    public static ServerSideEncryptionConfiguration parseGetBucketEncryption(InputStream responseBody)
        throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            ServerSideEncryptionConfiguration configuration = new ServerSideEncryptionConfiguration();
            ServerSideEncryptionByDefault sseByDefault = new ServerSideEncryptionByDefault();

            Element sseElem = root.getChild("ApplyServerSideEncryptionByDefault");
            sseByDefault.setSSEAlgorithm(sseElem.getChildText("SSEAlgorithm"));
            sseByDefault.setKMSMasterKeyID(sseElem.getChildText("KMSMasterKeyID"));
            if (sseElem.getChild("KMSDataEncryption") != null) {
                sseByDefault.setKMSDataEncryption(sseElem.getChildText("KMSDataEncryption"));
            }
            configuration.setApplyServerSideEncryptionByDefault(sseByDefault);

            return configuration;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucket policy response body .
     */
    public static GetBucketPolicyResult parseGetBucketPolicy(InputStream responseBody) throws ResponseParseException {

        try {
        	GetBucketPolicyResult result = new GetBucketPolicyResult();

    		BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody));
    		StringBuilder sb = new StringBuilder();
    		
    		String s ;
    		while (( s = reader.readLine()) != null) 
    			sb.append(s);
    		
    		result.setPolicy(sb.toString());
    		
            return result;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }
    
    /**
     * Unmarshall get bucuket request payment response body to GetBucketRequestPaymentResult.
     */
    public static GetBucketRequestPaymentResult parseGetBucketRequestPayment(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetBucketRequestPaymentResult paymentResult = new GetBucketRequestPaymentResult();

            if (root.getChild("Payer") != null) {
            	Payer payer = Payer.parse(root.getChildText("Payer"));
            	paymentResult.setPayer(payer);
            }

            return paymentResult;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }
    /**
     * Unmarshall get user qos info response body to UserQosInfo.
     */
    public static UserQosInfo parseGetUserQosInfo(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            UserQosInfo userQosInfo = new UserQosInfo();

            userQosInfo.setRegion(root.getChildText("Region"));
            userQosInfo.setTotalUploadBw(Integer.valueOf(root.getChildText("TotalUploadBandwidth")));
            userQosInfo.setIntranetUploadBw(Integer.valueOf(root.getChildText("IntranetUploadBandwidth")));
            userQosInfo.setExtranetUploadBw(Integer.valueOf(root.getChildText("ExtranetUploadBandwidth")));
            userQosInfo.setTotalDownloadBw(Integer.valueOf(root.getChildText("TotalDownloadBandwidth")));
            userQosInfo.setIntranetDownloadBw(Integer.valueOf(root.getChildText("IntranetDownloadBandwidth")));
            userQosInfo.setExtranetDownloadBw(Integer.valueOf(root.getChildText("ExtranetDownloadBandwidth")));
            userQosInfo.setTotalQps(Integer.valueOf(root.getChildText("TotalQps")));
            userQosInfo.setIntranetQps(Integer.valueOf(root.getChildText("IntranetQps")));
            userQosInfo.setExtranetQps(Integer.valueOf(root.getChildText("ExtranetQps")));

            return userQosInfo;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucuket qos info response body to BucketQosInfo.
     */
    public static BucketQosInfo parseGetBucketQosInfo(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            BucketQosInfo bucketQosInfo = new BucketQosInfo();

            bucketQosInfo.setTotalUploadBw(Integer.valueOf(root.getChildText("TotalUploadBandwidth")));
            bucketQosInfo.setIntranetUploadBw(Integer.valueOf(root.getChildText("IntranetUploadBandwidth")));
            bucketQosInfo.setExtranetUploadBw(Integer.valueOf(root.getChildText("ExtranetUploadBandwidth")));
            bucketQosInfo.setTotalDownloadBw(Integer.valueOf(root.getChildText("TotalDownloadBandwidth")));
            bucketQosInfo.setIntranetDownloadBw(Integer.valueOf(root.getChildText("IntranetDownloadBandwidth")));
            bucketQosInfo.setExtranetDownloadBw(Integer.valueOf(root.getChildText("ExtranetDownloadBandwidth")));
            bucketQosInfo.setTotalQps(Integer.valueOf(root.getChildText("TotalQps")));
            bucketQosInfo.setIntranetQps(Integer.valueOf(root.getChildText("IntranetQps")));
            bucketQosInfo.setExtranetQps(Integer.valueOf(root.getChildText("ExtranetQps")));

            return bucketQosInfo;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    private static InventoryConfiguration parseInventoryConfigurationElem(Element configElem) {
        InventoryConfiguration inventoryConfiguration = new InventoryConfiguration();

        if (configElem.getChildText("Id") != null) {
            inventoryConfiguration.setInventoryId(configElem.getChildText("Id"));
        }

        if (configElem.getChildText("IncludedObjectVersions") != null) {
            inventoryConfiguration.setIncludedObjectVersions(configElem.getChildText("IncludedObjectVersions"));
        }

        if (configElem.getChildText("IsEnabled") != null) {
            inventoryConfiguration.setEnabled(Boolean.valueOf(configElem.getChildText("IsEnabled")));
        }

        if (configElem.getChild("Filter") != null) {
            Element elem = configElem.getChild("Filter");
            InventoryFilter filter = new InventoryFilter();
            if (elem.getChildText("Prefix") != null) {
                filter = new InventoryFilter().withPrefix(elem.getChildText("Prefix"));
            }
            if (elem.getChildText("LastModifyBeginTimeStamp") != null) {
                filter.setLastModifyBeginTimeStamp(Long.valueOf(elem.getChildText("LastModifyBeginTimeStamp")));
            }
            if (elem.getChildText("LastModifyEndTimeStamp") != null) {
                filter.setLastModifyEndTimeStamp(Long.valueOf(elem.getChildText("LastModifyEndTimeStamp")));
            }
            if (elem.getChildText("LowerSizeBound") != null) {
                filter.setLowerSizeBound(Long.valueOf(elem.getChildText("LowerSizeBound")));
            }
            if (elem.getChildText("UpperSizeBound") != null) {
                filter.setUpperSizeBound(Long.valueOf(elem.getChildText("UpperSizeBound")));
            }
            if (elem.getChildText("StorageClass") != null) {
                filter.setStorageClass(elem.getChildText("StorageClass"));
            }
            inventoryConfiguration.setInventoryFilter(filter);
        }

        if (configElem.getChild("Schedule") != null) {
            InventorySchedule schedule = new InventorySchedule();
            Element elem = configElem.getChild("Schedule");
            if (elem.getChild("Frequency") != null) {
                schedule.setFrequency(elem.getChildText("Frequency"));
                inventoryConfiguration.setSchedule(schedule);
            }
        }

        if (configElem.getChild("OptionalFields") != null) {
            Element OptionalFieldsElem = configElem.getChild("OptionalFields");
            List<String> optionalFields = new ArrayList<String>();
            List<Element> fieldElems = OptionalFieldsElem.getChildren("Field");
            for (Element e : fieldElems) {
                optionalFields.add(e.getText());
            }
            inventoryConfiguration.setOptionalFields(optionalFields);
        }

        if (configElem.getChild("Destination") != null) {
            InventoryDestination destination = new InventoryDestination();
            Element destinElem = configElem.getChild("Destination");
            if (destinElem.getChild("OSSBucketDestination") != null) {
                InventoryOSSBucketDestination ossBucketDestion = new InventoryOSSBucketDestination();
                Element bucketDistinElem = destinElem.getChild("OSSBucketDestination");
                if (bucketDistinElem.getChildText("Format") != null) {
                    ossBucketDestion.setFormat(bucketDistinElem.getChildText("Format"));
                }
                if (bucketDistinElem.getChildText("AccountId") != null) {
                    ossBucketDestion.setAccountId(bucketDistinElem.getChildText("AccountId"));
                }
                if (bucketDistinElem.getChildText("RoleArn") != null) {
                    ossBucketDestion.setRoleArn(bucketDistinElem.getChildText("RoleArn"));
                }
                if (bucketDistinElem.getChildText("Bucket") != null) {
                    String tmpBucket = bucketDistinElem.getChildText("Bucket");
                    String bucket = tmpBucket.replaceFirst("acs:oss:::", "");
                    ossBucketDestion.setBucket(bucket);
                }
                if (bucketDistinElem.getChildText("Prefix") != null) {
                    ossBucketDestion.setPrefix(bucketDistinElem.getChildText("Prefix"));
                }

                if (bucketDistinElem.getChild("Encryption") != null) {
                    InventoryEncryption inventoryEncryption = new InventoryEncryption();
                    if (bucketDistinElem.getChild("Encryption").getChild("SSE-KMS") != null) {
                        String keyId = bucketDistinElem.getChild("Encryption").getChild("SSE-KMS").getChildText("KeyId");
                        inventoryEncryption.setServerSideKmsEncryption(new InventoryServerSideEncryptionKMS().withKeyId(keyId));
                    } else if (bucketDistinElem.getChild("Encryption").getChild("SSE-OSS") != null) {
                        inventoryEncryption.setServerSideOssEncryption(new InventoryServerSideEncryptionOSS());
                    }
                    ossBucketDestion.setEncryption(inventoryEncryption);
                }
                destination.setOssBucketDestination(ossBucketDestion);
            }
            inventoryConfiguration.setDestination(destination);
        }

        return inventoryConfiguration;
    }


    /**
     * Unmarshall get bucuket inventory configuration response body to GetBucketInventoryConfigurationResult.
     */
    public static GetBucketInventoryConfigurationResult parseGetBucketInventoryConfig(InputStream responseBody)
            throws ResponseParseException {
        try {
            GetBucketInventoryConfigurationResult result = new GetBucketInventoryConfigurationResult();
            Element root = getXmlRootElement(responseBody);
            InventoryConfiguration configuration = parseInventoryConfigurationElem(root);
            result.setInventoryConfiguration(configuration);
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }


    /**
     * Unmarshall get bucuket qos info response body to BucketQosInfo.
     */
    public static ListBucketInventoryConfigurationsResult parseListBucketInventoryConfigurations(InputStream responseBody)
            throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);
            ListBucketInventoryConfigurationsResult result = new ListBucketInventoryConfigurationsResult();
            List<InventoryConfiguration> inventoryConfigurationList = null;

            if (root.getChild("InventoryConfiguration") != null) {
                inventoryConfigurationList = new ArrayList<InventoryConfiguration>();
                List<Element> configurationElems = root.getChildren("InventoryConfiguration");
                for (Element elem : configurationElems) {
                    InventoryConfiguration configuration = parseInventoryConfigurationElem(elem);
                    inventoryConfigurationList.add(configuration);
                }
                result.setInventoryConfigurationList(inventoryConfigurationList);
            }

            if (root.getChild("ContinuationToken") != null) {
                result.setContinuationToken(root.getChildText("ContinuationToken"));
            }

            if (root.getChild("IsTruncated") != null) {
                result.setTruncated(Boolean.valueOf(root.getChildText("IsTruncated")));
            }

            if (root.getChild("NextContinuationToken") != null) {
                result.setNextContinuationToken(root.getChildText("NextContinuationToken"));
            }

            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket lifecycle response body to lifecycle rules.
     */
    @SuppressWarnings("unchecked")
    public static List<LifecycleRule> parseGetBucketLifecycle(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            List<LifecycleRule> lifecycleRules = new ArrayList<LifecycleRule>();
            List<Element> ruleElements = root.getChildren("Rule");

            for (Element ruleElem : ruleElements) {
                LifecycleRule rule = new LifecycleRule();

                if (ruleElem.getChild("ID") != null) {
                    rule.setId(ruleElem.getChildText("ID"));
                }

                if (ruleElem.getChild("Prefix") != null) {
                    rule.setPrefix(ruleElem.getChildText("Prefix"));
                }

                if (ruleElem.getChild("AtimeBase") != null) {
                    rule.setaTimeBase(ruleElem.getChildText("AtimeBase"));
                }
                
                List<Element> tagElems = ruleElem.getChildren("Tag");
                if (tagElems != null) {
                    for (Element tagElem : tagElems) {
                        String key = null;
                        String value = null;
                        
                        if (tagElem.getChild("Key") != null) {
                            key = tagElem.getChildText("Key");
                        }
                        if (tagElem.getChild("Value") != null) {
                            value = tagElem.getChildText("Value");
                        }
                        
                        rule.addTag(key, value);
                    }
                }

                Element filterElems = ruleElem.getChild("Filter");
                if(filterElems != null){
                    LifecycleFilter lifecycleFilter = new LifecycleFilter();

                    List<LifecycleNot> notList = new ArrayList<LifecycleNot>();
                    for(Element notEle : filterElems.getChildren("Not")){
                        LifecycleNot lifecycleNot = new LifecycleNot();
                        lifecycleNot.setPrefix(notEle.getChildText("Prefix"));
                        if (notEle.getChild("Tag") != null) {
                            Tag tag = new Tag(notEle.getChild("Tag").getChildText("Key"), notEle.getChild("Tag").getChildText("Value"));
                            lifecycleNot.setTag(tag);
                        }
                        notList.add(lifecycleNot);
                    }
                    lifecycleFilter.setNotList(notList);
                    rule.setFilter(lifecycleFilter);
                }

                if (ruleElem.getChild("Status") != null) {
                    rule.setStatus(RuleStatus.valueOf(ruleElem.getChildText("Status")));
                }

                if (ruleElem.getChild("Expiration") != null) {
                    if (ruleElem.getChild("Expiration").getChild("Date") != null) {
                        Date expirationDate = DateUtil
                                .parseIso8601Date(ruleElem.getChild("Expiration").getChildText("Date"));
                        rule.setExpirationTime(expirationDate);
                    } else if (ruleElem.getChild("Expiration").getChild("Days") != null) {
                        rule.setExpirationDays(Integer.parseInt(ruleElem.getChild("Expiration").getChildText("Days")));
                    }  else if (ruleElem.getChild("Expiration").getChild("CreatedBeforeDate") != null) {
                        Date createdBeforeDate = DateUtil
                                .parseIso8601Date(ruleElem.getChild("Expiration").getChildText("CreatedBeforeDate"));
                        rule.setCreatedBeforeDate(createdBeforeDate);
                    } else if (ruleElem.getChild("Expiration").getChild("ExpiredObjectDeleteMarker") != null) {
                        rule.setExpiredDeleteMarker(Boolean.valueOf(ruleElem.getChild("Expiration").getChildText("ExpiredObjectDeleteMarker")));
                    }
                }

                if (ruleElem.getChild("AbortMultipartUpload") != null) {
                    LifecycleRule.AbortMultipartUpload abortMultipartUpload = new LifecycleRule.AbortMultipartUpload();
                    if (ruleElem.getChild("AbortMultipartUpload").getChild("Days") != null) {
                        abortMultipartUpload.setExpirationDays(
                                Integer.parseInt(ruleElem.getChild("AbortMultipartUpload").getChildText("Days")));
                    } else {
                        Date createdBeforeDate = DateUtil.parseIso8601Date(
                                ruleElem.getChild("AbortMultipartUpload").getChildText("CreatedBeforeDate"));
                        abortMultipartUpload.setCreatedBeforeDate(createdBeforeDate);
                    }
                    rule.setAbortMultipartUpload(abortMultipartUpload);
                }

                List<Element> transitionElements = ruleElem.getChildren("Transition");
                List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
                for (Element transitionElem : transitionElements) {
                    LifecycleRule.StorageTransition storageTransition = new LifecycleRule.StorageTransition();
                    if (transitionElem.getChild("Days") != null) {
                        storageTransition.setExpirationDays(Integer.parseInt(transitionElem.getChildText("Days")));
                    } else {
                        Date createdBeforeDate = DateUtil
                                .parseIso8601Date(transitionElem.getChildText("CreatedBeforeDate"));
                        storageTransition.setCreatedBeforeDate(createdBeforeDate);
                    }
                    if (transitionElem.getChild("StorageClass") != null) {
                        storageTransition
                                .setStorageClass(StorageClass.parse(transitionElem.getChildText("StorageClass")));
                    }
                    if (transitionElem.getChild("IsAccessTime") != null) {
                        storageTransition.setIsAccessTime(Boolean.valueOf(transitionElem.getChildText("IsAccessTime")));
                    }
                    if (transitionElem.getChild("ReturnToStdWhenVisit") != null) {
                        storageTransition.setReturnToStdWhenVisit(Boolean.valueOf(transitionElem.getChildText("ReturnToStdWhenVisit")));
                    }
                    if (transitionElem.getChild("AllowSmallFile") != null) {
                        storageTransition.setAllowSmallFile(Boolean.valueOf(transitionElem.getChildText("AllowSmallFile")));
                    }
                    storageTransitions.add(storageTransition);
                }
                rule.setStorageTransition(storageTransitions);

                if (ruleElem.getChild("NoncurrentVersionExpiration") != null) {
                    NoncurrentVersionExpiration expiration = new NoncurrentVersionExpiration();
                    if (ruleElem.getChild("NoncurrentVersionExpiration").getChild("NoncurrentDays") != null) {
                        expiration.setNoncurrentDays(Integer.parseInt(ruleElem.getChild("NoncurrentVersionExpiration").getChildText("NoncurrentDays")));
                        rule.setNoncurrentVersionExpiration(expiration);
                    }
                }

                List<Element> versionTansitionElements = ruleElem.getChildren("NoncurrentVersionTransition");
                List<NoncurrentVersionStorageTransition> noncurrentVersionTransitions = new ArrayList<NoncurrentVersionStorageTransition>();
                for (Element transitionElem : versionTansitionElements) {
                    NoncurrentVersionStorageTransition transition = new NoncurrentVersionStorageTransition();
                    if (transitionElem.getChild("NoncurrentDays") != null) {
                        transition.setNoncurrentDays(Integer.parseInt(transitionElem.getChildText("NoncurrentDays")));
                    }
                    if (transitionElem.getChild("StorageClass") != null) {
                        transition.setStorageClass(StorageClass.parse(transitionElem.getChildText("StorageClass")));
                    }
                    if (transitionElem.getChild("IsAccessTime") != null) {
                        transition.setIsAccessTime(Boolean.valueOf(transitionElem.getChildText("IsAccessTime")));
                    }
                    if (transitionElem.getChild("ReturnToStdWhenVisit") != null) {
                        transition.setReturnToStdWhenVisit(Boolean.valueOf(transitionElem.getChildText("ReturnToStdWhenVisit")));
                    }
                    if (transitionElem.getChild("AllowSmallFile") != null) {
                        transition.setAllowSmallFile(Boolean.valueOf(transitionElem.getChildText("AllowSmallFile")));
                    }
                    noncurrentVersionTransitions.add(transition);
                }
                rule.setNoncurrentVersionStorageTransitions(noncurrentVersionTransitions);

                lifecycleRules.add(rule);
            }

            return lifecycleRules;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall get bucket cname response body to cname configuration.
     */
    @SuppressWarnings("unchecked")
    public static List<CnameConfiguration> parseGetBucketCname(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            List<CnameConfiguration> cnames = new ArrayList<CnameConfiguration>();
            List<Element> cnameElements = root.getChildren("Cname");

            for (Element cnameElem : cnameElements) {
                CnameConfiguration cname = new CnameConfiguration();

                cname.setDomain(cnameElem.getChildText("Domain"));
                cname.setStatus(CnameConfiguration.CnameStatus.valueOf(cnameElem.getChildText("Status")));
                cname.setLastMofiedTime(DateUtil.parseIso8601Date(cnameElem.getChildText("LastModified")));

                if (cnameElem.getChildText("IsPurgeCdnCache") != null) {
                    boolean purgeCdnCache = Boolean.valueOf(cnameElem.getChildText("IsPurgeCdnCache"));
                    cname.setPurgeCdnCache(purgeCdnCache);
                }

                Element certElem = cnameElem.getChild("Certificate");
                if (certElem != null) {
                    cname.setCertType(CnameConfiguration.CertType.parse(certElem.getChildText("Type")));
                    cname.setCertStatus(CnameConfiguration.CertStatus.parse(certElem.getChildText("Status")));
                    cname.setCertId(certElem.getChildText("CertId"));
                }

                cnames.add(cname);
            }

            return cnames;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall create bucket cname token response body to cname configuration.
     */
    @SuppressWarnings("unchecked")
    public static CreateBucketCnameTokenResult parseCreateBucketCnameToken(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            String bucket = root.getChildText("Bucket");
            String cname = root.getChildText("Cname");
            String token = root.getChildText("Token");
            String expireTime = root.getChildText("ExpireTime");
            CreateBucketCnameTokenResult result = new CreateBucketCnameTokenResult();
            result.setBucket(bucket);
            result.setCname(cname);
            result.setToken(token);
            result.setExpireTime(expireTime);
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall create bucket cname token response body to cname configuration.
     */
    @SuppressWarnings("unchecked")
    public static GetBucketCnameTokenResult parseGetBucketCnameToken(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            String bucket = root.getChildText("Bucket");
            String cname = root.getChildText("Cname");
            String token = root.getChildText("Token");
            String expireTime = root.getChildText("ExpireTime");
            GetBucketCnameTokenResult result = new GetBucketCnameTokenResult();
            result.setBucket(bucket);
            result.setCname(cname);
            result.setToken(token);
            result.setExpireTime(expireTime);
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    /**
     * Unmarshall set async fetch task response body to corresponding result.
     */
    public static SetAsyncFetchTaskResult parseSetAsyncFetchTaskResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            SetAsyncFetchTaskResult setAsyncFetchTaskResult = new SetAsyncFetchTaskResult();
            setAsyncFetchTaskResult.setTaskId(root.getChildText("TaskId"));
            return setAsyncFetchTaskResult;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }

    private static AsyncFetchTaskConfiguration parseAsyncFetchTaskInfo(Element taskInfoEle) {
        if (taskInfoEle == null) {
            return null;
        }

        AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration();
        configuration.setUrl(taskInfoEle.getChildText("Url"));
        configuration.setObjectName(taskInfoEle.getChildText("Object"));
        configuration.setHost(taskInfoEle.getChildText("Host"));
        configuration.setContentMd5(taskInfoEle.getChildText("ContentMD5"));
        configuration.setCallback(taskInfoEle.getChildText("Callback"));
        if (taskInfoEle.getChild("IgnoreSameKey") != null) {
            configuration.setIgnoreSameKey(Boolean.valueOf(taskInfoEle.getChildText("IgnoreSameKey")));
        }

        return configuration;
    }

    /**
     * Unmarshall get async fetch task info response body to corresponding result.
     */
    public static GetAsyncFetchTaskResult parseGetAsyncFetchTaskResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetAsyncFetchTaskResult getAsyncFetchTaskResult = new GetAsyncFetchTaskResult();

            getAsyncFetchTaskResult.setTaskId(root.getChildText("TaskId"));
            getAsyncFetchTaskResult.setAsyncFetchTaskState(AsyncFetchTaskState.parse(root.getChildText("State")));
            getAsyncFetchTaskResult.setErrorMsg(root.getChildText("ErrorMsg"));
            AsyncFetchTaskConfiguration configuration = parseAsyncFetchTaskInfo(root.getChild("TaskInfo"));
            getAsyncFetchTaskResult.setAsyncFetchTaskConfiguration(configuration);

            return getAsyncFetchTaskResult;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }
    }


    public static final class CreateVpcipResultResponseParser implements ResponseParser<Vpcip> {

        @Override
        public Vpcip parse(ResponseMessage response) throws ResponseParseException {
            try {
                Vpcip result = parseGetCreateVpcipResult(response.getContent());
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

    }

    public static final class ListVpcipResultResponseParser implements ResponseParser<List<Vpcip>> {
        @Override
        public List<Vpcip> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseListVpcipResult(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    public static final class ListVpcPolicyResultResponseParser implements ResponseParser<List<VpcPolicy>> {
        @Override
        public List<VpcPolicy> parse(ResponseMessage response) throws ResponseParseException {
            try {
                return parseListVpcPolicyResult(response.getContent());
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    /**
     * Unmarshall image Vpcip response body to style list.
     */
    public static Vpcip parseGetCreateVpcipResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            Vpcip vpcip = new Vpcip();

            if (root.getChild("Region") != null) {
                vpcip.setRegion(root.getChildText("Region"));
            }
            if (root.getChild("VpcId") != null) {
                vpcip.setVpcId(root.getChildText("VpcId"));
            }
            if (root.getChild("Vip") != null) {
                vpcip.setVip(root.getChildText("Vip"));
            }
            if (root.getChild("Label") != null) {
                vpcip.setLabel(root.getChildText("Label"));
            }

            return vpcip;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall list image Vpcip response body to style list.
     */
    @SuppressWarnings("unchecked")
    public static List<Vpcip> parseListVpcipResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);

            List<Vpcip> vpcipList = new ArrayList<Vpcip>();
            List<Element> vpcips = root.getChildren("Vpcip");

            for (Element e : vpcips) {
                Vpcip vpcipInfo = new Vpcip();
                vpcipInfo.setRegion(e.getChildText("Region"));
                vpcipInfo.setVpcId(e.getChildText("VpcId"));
                vpcipInfo.setVip(e.getChildText("Vip"));
                vpcipInfo.setLabel(e.getChildText("Label"));
                vpcipList.add(vpcipInfo);
            }

            return vpcipList;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall list image VpcPolicy response body to style list.
     */
    @SuppressWarnings("unchecked")
    public static List<VpcPolicy> parseListVpcPolicyResult(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            List<VpcPolicy> vpcipList = new ArrayList<VpcPolicy>();
            List<Element> vpcips = root.getChildren("Vpcip");

            for (Element e : vpcips) {
                VpcPolicy vpcipInfo = new VpcPolicy();

                vpcipInfo.setRegion(e.getChildText("Region"));
                vpcipInfo.setVpcId(e.getChildText("VpcId"));
                vpcipInfo.setVip(e.getChildText("Vip"));
                vpcipList.add(vpcipInfo);
            }

            return vpcipList;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall initiate bucket worm result from response headers.
     */
    public static InitiateBucketWormResult parseInitiateBucketWormResponseHeader(Map<String, String> headers) throws ResponseParseException {

        try {
            InitiateBucketWormResult result = new InitiateBucketWormResult();
            result.setWormId(headers.get(OSS_HEADER_WORM_ID));
            return result;
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucket worm result.
     */
    public static GetBucketWormResult parseWormConfiguration(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetBucketWormResult result = new GetBucketWormResult();
            result.setWormId(root.getChildText("WormId"));
            result.setWormState(root.getChildText("State"));
            result.setRetentionPeriodInDays(Integer.parseInt(root.getChildText("RetentionPeriodInDays")));
            result.setCreationDate(DateUtil.parseIso8601Date(root.getChildText("CreationDate")));
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    public static final class DeleteDirectoryResponseParser implements ResponseParser<DeleteDirectoryResult> {

        @Override
        public DeleteDirectoryResult parse(ResponseMessage response) throws ResponseParseException {
            try{
                DeleteDirectoryResult result =  parseDeleteDirectoryResult(response.getContent());
                result.setResponse(response);
                result.setRequestId(response.getRequestId());
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }
    }

    /**
     * Unmarshall delete directory result.
     */
    public static DeleteDirectoryResult parseDeleteDirectoryResult(InputStream responseBody) throws ResponseParseException {
        try {
            Element root = getXmlRootElement(responseBody);
            DeleteDirectoryResult result = new DeleteDirectoryResult();
            result.setDeleteNumber(Integer.valueOf(root.getChildText("DeleteNumber")).intValue());
            result.setDirectoryName(root.getChildText("DirectoryName"));
            if(root.getChild("NextDeleteToken") != null) {
                result.setNextDeleteToken(root.getChildText("NextDeleteToken"));
            }
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    /**
     * Unmarshall get bucket resource group.
     */
    public static GetBucketResourceGroupResult parseResourceGroupConfiguration(InputStream responseBody) throws ResponseParseException {

        try {
            Element root = getXmlRootElement(responseBody);
            GetBucketResourceGroupResult result = new GetBucketResourceGroupResult();
            result.setResourceGroupId(root.getChildText("ResourceGroupId"));
            return result;
        } catch (JDOMParseException e) {
            throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseParseException(e.getMessage(), e);
        }

    }

    public static final class GetBucketTransferAccelerationResponseParser implements ResponseParser<TransferAcceleration> {
        @Override
        public TransferAcceleration parse(ResponseMessage response) throws ResponseParseException {
            try {
                TransferAcceleration result = parseTransferAcceleration(response.getContent());
                result.setRequestId(response.getRequestId());

                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

        private TransferAcceleration parseTransferAcceleration(InputStream inputStream) throws ResponseParseException {
            TransferAcceleration transferAcceleration = new TransferAcceleration(Boolean.FALSE);
            if (inputStream == null) {
                return transferAcceleration;
            }

            try {
                Element root = getXmlRootElement(inputStream);

                if (root.getChildText("Enabled") != null) {
                    transferAcceleration.setEnabled(Boolean.valueOf(root.getChildText("Enabled")));
                }

                return transferAcceleration;
            } catch (JDOMParseException e) {
                throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseParseException(e.getMessage(), e);
            }
        }
    }

    /**
     * Unmarshall get bucket access monitor.
     */
    public static final class GetBucketAccessMonitorResponseParser implements ResponseParser<AccessMonitor> {
        @Override
        public AccessMonitor parse(ResponseMessage response) throws ResponseParseException {
            try {
                AccessMonitor result = parseAccessMonitor(response.getContent());
                setResultParameter(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

        private AccessMonitor parseAccessMonitor(InputStream inputStream) throws ResponseParseException {
            AccessMonitor accessMonitor = new AccessMonitor(AccessMonitor.AccessMonitorStatus.Disabled.toString());
            if (inputStream == null) {
                return accessMonitor;
            }

            try {
                Element root = getXmlRootElement(inputStream);

                if (root.getChildText("Status") != null) {
                    accessMonitor.setStatus(root.getChildText("Status"));
                }

                return accessMonitor;
            } catch (JDOMParseException e) {
                throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseParseException(e.getMessage(), e);
            }
        }
    }

    public static final class GetMetaQueryStatusResponseParser implements ResponseParser<GetMetaQueryStatusResult> {
        @Override
        public GetMetaQueryStatusResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                GetMetaQueryStatusResult result = parseGetMetaQueryStatusResult(response.getContent());
                setResultParameter(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

        private GetMetaQueryStatusResult parseGetMetaQueryStatusResult(InputStream inputStream) throws ResponseParseException {
            GetMetaQueryStatusResult getMetaQueryStatusResult = new GetMetaQueryStatusResult();
            if (inputStream == null) {
                return getMetaQueryStatusResult;
            }

            try {
                Element root = getXmlRootElement(inputStream);

                if (root.getChildText("State") != null) {
                    getMetaQueryStatusResult.setState(root.getChildText("State"));
                }
                if (root.getChildText("Phase") != null) {
                    getMetaQueryStatusResult.setPhase(root.getChildText("Phase"));
                }
                if (root.getChildText("CreateTime") != null) {
                    getMetaQueryStatusResult.setCreateTime(root.getChildText("CreateTime"));
                }
                if (root.getChildText("UpdateTime") != null) {
                    getMetaQueryStatusResult.setUpdateTime(root.getChildText("UpdateTime"));
                }
                return getMetaQueryStatusResult;
            } catch (JDOMParseException e) {
                throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseParseException(e.getMessage(), e);
            }
        }
    }

    public static final class DoMetaQueryResponseParser implements ResponseParser<DoMetaQueryResult> {
        @Override
        public DoMetaQueryResult parse(ResponseMessage response) throws ResponseParseException {
            try {
                DoMetaQueryResult result = parseDoMetaQueryResult(response.getContent());
                setResultParameter(result, response);
                return result;
            } finally {
                safeCloseResponse(response);
            }
        }

        private DoMetaQueryResult parseDoMetaQueryResult(InputStream inputStream) throws ResponseParseException {
            DoMetaQueryResult doMetaQueryResult = new DoMetaQueryResult();
            if (inputStream == null) {
                return doMetaQueryResult;
            }

            try {
                Element root = getXmlRootElement(inputStream);

                if (root.getChildText("NextToken") != null) {
                    doMetaQueryResult.setNextToken(root.getChildText("NextToken"));
                }
                Element filesElem = root.getChild("Files");
                if(filesElem != null){
                    ObjectFiles objectFiles = new ObjectFiles();
                    List<Element> fileElem = filesElem.getChildren();
                    List<ObjectFile> fileList = new ArrayList<ObjectFile>();
                    for(Element elem : fileElem){
                        ObjectFile objectFile = new ObjectFile();
                        objectFile.setFilename(elem.getChildText("Filename"));
                        if(!StringUtils.isNullOrEmpty(elem.getChildText("Size"))){
                            objectFile.setSize(Long.parseLong(elem.getChildText("Size")));
                        }
                        objectFile.setFileModifiedTime(elem.getChildText("FileModifiedTime"));
                        objectFile.setFileCreateTime(elem.getChildText("FileCreateTime"));
                        objectFile.setFileAccessTime(elem.getChildText("FileAccessTime"));
                        objectFile.setOssObjectType(elem.getChildText("OSSObjectType"));
                        objectFile.setOssStorageClass(elem.getChildText("OSSStorageClass"));
                        objectFile.setObjectACL(elem.getChildText("ObjectACL"));
                        objectFile.setETag(elem.getChildText("ETag"));
                        objectFile.setOssCRC64(elem.getChildText("OSSCRC64"));
                        if(!StringUtils.isNullOrEmpty(elem.getChildText("OSSTaggingCount"))){
                            objectFile.setOssTaggingCount(Integer.parseInt(elem.getChildText("OSSTaggingCount")));
                        }

                        Element ossTaggingElem = elem.getChild("OSSTagging");
                        if(ossTaggingElem != null){
                            OSSTagging ossTagging = new OSSTagging();
                            List<Element> taggingElem = ossTaggingElem.getChildren();
                            List<Tagging> taggingList = new ArrayList<Tagging>();
                            for(Element ele : taggingElem){
                                Tagging tagging = new Tagging();
                                tagging.setKey(ele.getChildText("Key"));
                                tagging.setValue(ele.getChildText("Value"));
                                taggingList.add(tagging);
                            }
                            ossTagging.setTagging(taggingList);
                            objectFile.setOssTagging(ossTagging);
                        }

                        Element ossUserMetaElem = elem.getChild("OSSUserMeta");
                        if(ossUserMetaElem != null){
                            OSSUserMeta ossUserMeta = new OSSUserMeta();
                            List<Element> userMetaElem = ossUserMetaElem.getChildren();
                            List<UserMeta> userMetaList = new ArrayList<UserMeta>();
                            for(Element ele : userMetaElem){
                                UserMeta userMeta = new UserMeta();
                                userMeta.setKey(ele.getChildText("Key"));
                                userMeta.setValue(ele.getChildText("Value"));
                                userMetaList.add(userMeta);
                            }
                            ossUserMeta.setUserMeta(userMetaList);
                            objectFile.setOssUserMeta(ossUserMeta);
                        }
                        fileList.add(objectFile);
                    }
                    objectFiles.setFile(fileList);
                    doMetaQueryResult.setFiles(objectFiles);
                }

                Element elem = root.getChild("Aggregations");
                if(elem != null){
                    List<Aggregation> aggregationList = new ArrayList<Aggregation>();
                    Aggregations aggregations = new Aggregations();
                    for(Element el : elem.getChildren()){
                        Aggregation aggregation = new Aggregation();
                        aggregation.setField(el.getChildText("Field"));
                        aggregation.setOperation(el.getChildText("Operation"));
                        if(!StringUtils.isNullOrEmpty(el.getChildText("Value"))){
                            aggregation.setValue(Double.parseDouble(el.getChildText("Value")));
                        }

                        Element elemGroup = el.getChild("Groups");
                        if(elemGroup != null){
                            List<AggregationGroup> groupList = new ArrayList<AggregationGroup>();
                            AggregationGroups aggregationGroups = new AggregationGroups();
                            for(Element e : elemGroup.getChildren()){
                                AggregationGroup aggregationGroup = new AggregationGroup();
                                aggregationGroup.setValue(e.getChildText("Value"));
                                if(!StringUtils.isNullOrEmpty(e.getChildText("Count"))){
                                    aggregationGroup.setCount(Integer.parseInt(e.getChildText("Count")));
                                }
                                groupList.add(aggregationGroup);
                            }
                            aggregationGroups.setGroup(groupList);
                            aggregation.setGroups(aggregationGroups);
                        }

                        aggregationList.add(aggregation);
                    }
                    aggregations.setAggregation(aggregationList);
                    doMetaQueryResult.setAggregations(aggregations);
                }

                return doMetaQueryResult;
            } catch (JDOMParseException e) {
                throw new ResponseParseException(e.getPartialDocument() + ": " + e.getMessage(), e);
            } catch (Exception e) {
                throw new ResponseParseException(e.getMessage(), e);
            }
        }
    }
}
