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

import static com.aliyun.oss.common.parser.RequestMarshallers.createLiveChannelRequestMarshaller;
import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;
import static com.aliyun.oss.internal.OSSUtils.ensureBucketNameValid;
import static com.aliyun.oss.internal.OSSUtils.ensureLiveChannelNameValid;
import static com.aliyun.oss.internal.RequestParameters.OSS_ACCESS_KEY_ID;
import static com.aliyun.oss.internal.RequestParameters.SECURITY_TOKEN;
import static com.aliyun.oss.internal.RequestParameters.SIGNATURE;
import static com.aliyun.oss.internal.ResponseParsers.createLiveChannelResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.getLiveChannelHistoryResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.getLiveChannelInfoResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.getLiveChannelStatResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.listLiveChannelsReponseParser;
import static com.aliyun.oss.internal.ResponseParsers.GetObjectResponseParser;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.model.CreateLiveChannelRequest;
import com.aliyun.oss.model.CreateLiveChannelResult;
import com.aliyun.oss.model.GenerateRtmpUriRequest;
import com.aliyun.oss.model.GenerateVodPlaylistRequest;
import com.aliyun.oss.model.ListLiveChannelsRequest;
import com.aliyun.oss.model.LiveChannel;
import com.aliyun.oss.model.LiveChannelGenericRequest;
import com.aliyun.oss.model.LiveChannelInfo;
import com.aliyun.oss.model.LiveChannelListing;
import com.aliyun.oss.model.LiveChannelStat;
import com.aliyun.oss.model.LiveRecord;
import com.aliyun.oss.model.SetLiveChannelRequest;
import com.aliyun.oss.model.GetVodPlaylistRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.VoidResult;

/**
 * Live channel operation.
 */
public class LiveChannelOperation extends OSSOperation {

    public LiveChannelOperation(ServiceClient client, CredentialsProvider credsProvider) {
        super(client, credsProvider);
    }

    public CreateLiveChannelResult createLiveChannel(CreateLiveChannelRequest createLiveChannelRequest)
            throws OSSException, ClientException {
        assertParameterNotNull(createLiveChannelRequest, "createLiveChannelRequest");

        String bucketName = createLiveChannelRequest.getBucketName();
        String liveChannelName = createLiveChannelRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);

        byte[] rawContent = createLiveChannelRequestMarshaller.marshall(createLiveChannelRequest);
        Map<String, String> headers = new HashMap<String, String>();
        addRequestRequiredHeaders(headers, rawContent);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(createLiveChannelRequest))
                .setMethod(HttpMethod.PUT).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setHeaders(headers).setInputSize(rawContent.length)
                .setInputStream(new ByteArrayInputStream(rawContent)).setOriginalRequest(createLiveChannelRequest)
                .build();

        List<ResponseHandler> reponseHandlers = new ArrayList<ResponseHandler>();
        reponseHandlers.add(new OSSCallbackErrorResponseHandler());

        return doOperation(request, createLiveChannelResponseParser, bucketName, liveChannelName, true);
    }

    public VoidResult setLiveChannelStatus(SetLiveChannelRequest setLiveChannelRequest) throws OSSException, ClientException {

        assertParameterNotNull(setLiveChannelRequest, "setLiveChannelRequest");

        String bucketName = setLiveChannelRequest.getBucketName();
        String liveChannelName = setLiveChannelRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);
        parameters.put(RequestParameters.SUBRESOURCE_STATUS, setLiveChannelRequest.getLiveChannelStatus().toString());

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(setLiveChannelRequest))
                .setMethod(HttpMethod.PUT).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(setLiveChannelRequest).build();

        return doOperation(request, requestIdResponseParser, bucketName, liveChannelName);
    }

    public LiveChannelInfo getLiveChannelInfo(LiveChannelGenericRequest liveChannelGenericRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(liveChannelGenericRequest, "liveChannelGenericRequest");

        String bucketName = liveChannelGenericRequest.getBucketName();
        String liveChannelName = liveChannelGenericRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(liveChannelGenericRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(liveChannelGenericRequest).build();

        return doOperation(request, getLiveChannelInfoResponseParser, bucketName, liveChannelName, true);
    }

    public LiveChannelStat getLiveChannelStat(LiveChannelGenericRequest liveChannelGenericRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(liveChannelGenericRequest, "liveChannelGenericRequest");

        String bucketName = liveChannelGenericRequest.getBucketName();
        String liveChannelName = liveChannelGenericRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);
        parameters.put(RequestParameters.SUBRESOURCE_COMP, RequestParameters.STAT);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(liveChannelGenericRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(liveChannelGenericRequest).build();

        return doOperation(request, getLiveChannelStatResponseParser, bucketName, liveChannelName, true);
    }

    public VoidResult deleteLiveChannel(LiveChannelGenericRequest liveChannelGenericRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(liveChannelGenericRequest, "liveChannelGenericRequest");

        String bucketName = liveChannelGenericRequest.getBucketName();
        String liveChannelName = liveChannelGenericRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(liveChannelGenericRequest))
                .setMethod(HttpMethod.DELETE).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(liveChannelGenericRequest).build();

        return doOperation(request, requestIdResponseParser, bucketName, liveChannelName);
    }

    /**
     * List all live channels.
     */
    public List<LiveChannel> listLiveChannels(String bucketName) throws OSSException, ClientException {
        LiveChannelListing liveChannelListing = listLiveChannels(new ListLiveChannelsRequest(bucketName));
        List<LiveChannel> liveChannels = liveChannelListing.getLiveChannels();
        while (liveChannelListing.isTruncated()) {
            liveChannelListing = listLiveChannels(
                    new ListLiveChannelsRequest(bucketName, "", liveChannelListing.getNextMarker()));
            liveChannels.addAll(liveChannelListing.getLiveChannels());
        }
        return liveChannels;
    }

    /**
     * List live channels.
     */
    public LiveChannelListing listLiveChannels(ListLiveChannelsRequest listLiveChannelRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(listLiveChannelRequest, "listObjectsRequest");

        String bucketName = listLiveChannelRequest.getBucketName();
        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);
        populateListLiveChannelsRequestParameters(listLiveChannelRequest, parameters);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(listLiveChannelRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setParameters(parameters)
                .setOriginalRequest(listLiveChannelRequest).build();

        return doOperation(request, listLiveChannelsReponseParser, bucketName, null, true);
    }

    public List<LiveRecord> getLiveChannelHistory(LiveChannelGenericRequest liveChannelGenericRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(liveChannelGenericRequest, "liveChannelGenericRequest");

        String bucketName = liveChannelGenericRequest.getBucketName();
        String liveChannelName = liveChannelGenericRequest.getLiveChannelName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_LIVE, null);
        parameters.put(RequestParameters.SUBRESOURCE_COMP, RequestParameters.HISTORY);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(liveChannelGenericRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(liveChannelGenericRequest).build();

        return doOperation(request, getLiveChannelHistoryResponseParser, bucketName, liveChannelName, true);
    }

    public VoidResult generateVodPlaylist(GenerateVodPlaylistRequest generateVodPlaylistRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(generateVodPlaylistRequest, "generateVodPlaylistRequest");

        String bucketName = generateVodPlaylistRequest.getBucketName();
        String liveChannelName = generateVodPlaylistRequest.getLiveChannelName();
        String playlistName = generateVodPlaylistRequest.getPlaylistName();
        Long startTime = generateVodPlaylistRequest.getStartTime();
        Long endTime = generateVodPlaylistRequest.getEndTime();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);
        assertParameterNotNull(playlistName, "playlistName");
        assertParameterNotNull(startTime, "startTime");
        assertParameterNotNull(endTime, "endTime");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_VOD, null);
        parameters.put(RequestParameters.SUBRESOURCE_START_TIME, startTime.toString());
        parameters.put(RequestParameters.SUBRESOURCE_END_TIME, endTime.toString());

        String key = liveChannelName + "/" + playlistName;
        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(generateVodPlaylistRequest))
                .setMethod(HttpMethod.POST).setBucket(bucketName).setKey(key).setParameters(parameters)
                .setInputStream(new ByteArrayInputStream(new byte[0])).setInputSize(0)
                .setOriginalRequest(generateVodPlaylistRequest).build();

        return doOperation(request, requestIdResponseParser, bucketName, key);
    }

    public OSSObject getVodPlaylist(GetVodPlaylistRequest getVodPlaylistRequest) throws OSSException, ClientException {

        assertParameterNotNull(getVodPlaylistRequest, "getVodPlaylistRequest");

        String bucketName = getVodPlaylistRequest.getBucketName();
        String liveChannelName = getVodPlaylistRequest.getLiveChannelName();
        Long startTime = getVodPlaylistRequest.getStartTime();
        Long endTime = getVodPlaylistRequest.getEndTime();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);
        assertParameterNotNull(startTime, "startTime");
        assertParameterNotNull(endTime, "endTime");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(RequestParameters.SUBRESOURCE_VOD, null);
        parameters.put(RequestParameters.SUBRESOURCE_START_TIME, startTime.toString());
        parameters.put(RequestParameters.SUBRESOURCE_END_TIME, endTime.toString());

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(getVodPlaylistRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setKey(liveChannelName).setParameters(parameters)
                .setOriginalRequest(getVodPlaylistRequest).build();

        return doOperation(request, new GetObjectResponseParser(bucketName, liveChannelName), bucketName, liveChannelName, true);
    }

    public String generateRtmpUri(GenerateRtmpUriRequest request) throws OSSException, ClientException {

        assertParameterNotNull(request, "request");

        String bucketName = request.getBucketName();
        String liveChannelName = request.getLiveChannelName();
        String playlistName = request.getPlaylistName();
        Long expires = request.getExpires();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(liveChannelName, "liveChannelName");
        ensureLiveChannelNameValid(liveChannelName);
        assertParameterNotNull(playlistName, "playlistName");
        assertParameterNotNull(expires, "expires");

        Credentials currentCreds = this.credsProvider.getCredentials();
        String accessId = currentCreds.getAccessKeyId();
        String accessKey = currentCreds.getSecretAccessKey();
        boolean useSecurityToken = currentCreds.useSecurityToken();

        // Endpoint
        RequestMessage requestMessage = new RequestMessage(bucketName, liveChannelName);
        ClientConfiguration config = this.client.getClientConfiguration();
        requestMessage.setEndpoint(OSSUtils.determineFinalEndpoint(this.endpoint, bucketName, config));

        // Headers
        requestMessage.addHeader(HttpHeaders.DATE, expires.toString());

        // Parameters
        requestMessage.addParameter(RequestParameters.PLAYLIST_NAME, playlistName);

        if (useSecurityToken) {
            requestMessage.addParameter(SECURITY_TOKEN, currentCreds.getSecurityToken());
        }

        // Signature
        String canonicalResource = "/" + bucketName + "/" + liveChannelName;
        String canonicalString = SignUtils.buildRtmpCanonicalString(canonicalResource, requestMessage,
                expires.toString());
        String signature = ServiceSignature.create().computeSignature(accessKey, canonicalString);

        // Build query string
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put(HttpHeaders.EXPIRES, expires.toString());
        params.put(OSS_ACCESS_KEY_ID, accessId);
        params.put(SIGNATURE, signature);
        params.putAll(requestMessage.getParameters());

        String queryString = HttpUtil.paramToQueryString(params, DEFAULT_CHARSET_NAME);

        // Compose rtmp request uri
        String uri = requestMessage.getEndpoint().toString();
        String livChan = RequestParameters.SUBRESOURCE_LIVE + "/" + liveChannelName;

        if (uri.startsWith(OSSConstants.PROTOCOL_HTTP)) {
            uri = uri.replaceFirst(OSSConstants.PROTOCOL_HTTP, OSSConstants.PROTOCOL_RTMP);
        } else if (uri.startsWith(OSSConstants.PROTOCOL_HTTPS)) {
            uri = uri.replaceFirst(OSSConstants.PROTOCOL_HTTPS, OSSConstants.PROTOCOL_RTMP);
        } else if (!uri.startsWith(OSSConstants.PROTOCOL_RTMP)) {
            uri = OSSConstants.PROTOCOL_RTMP + uri;
        }

        if (!uri.endsWith("/")) {
            uri += "/";
        }
        uri += livChan + "?" + queryString;

        return uri;
    }

    private static void populateListLiveChannelsRequestParameters(ListLiveChannelsRequest listLiveChannelRequest,
            Map<String, String> params) {

        if (listLiveChannelRequest.getPrefix() != null) {
            params.put(RequestParameters.PREFIX, listLiveChannelRequest.getPrefix());
        }

        if (listLiveChannelRequest.getMarker() != null) {
            params.put(RequestParameters.MARKER, listLiveChannelRequest.getMarker());
        }

        if (listLiveChannelRequest.getMaxKeys() != null) {
            params.put(RequestParameters.MAX_KEYS, Integer.toString(listLiveChannelRequest.getMaxKeys()));
        }
    }

    private static void addRequestRequiredHeaders(Map<String, String> headers, byte[] rawContent) {
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(rawContent.length));

        byte[] md5 = BinaryUtil.calculateMd5(rawContent);
        String md5Base64 = BinaryUtil.toBase64String(md5);
        headers.put(HttpHeaders.CONTENT_MD5, md5Base64);
    }

}
