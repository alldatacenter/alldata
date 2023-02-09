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

import static com.qcloud.cos.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.Headers;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.CosServiceExceptionBuilder;
import com.qcloud.cos.http.CosHttpResponse;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpResponseHandler;
import com.qcloud.cos.utils.IOUtils;

public class CosErrorResponseHandler implements HttpResponseHandler<CosServiceException> {

    private static final Logger log = LoggerFactory.getLogger(CosErrorResponseHandler.class);

    /** Shared factory for creating XML event readers */
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    private static enum COSErrorTags {
        Error, Code, Message, Resource, RequestId, TraceId;
    };

    private CosServiceException createExceptionFromHeaders(CosHttpResponse errorResponse,
            String errorResponseXml) {
        final Map<String, String> headers = errorResponse.getHeaders();
        final int statusCode = errorResponse.getStatusCode();
        final CosServiceExceptionBuilder exceptionBuilder = new CosServiceExceptionBuilder();
        exceptionBuilder.setErrorMessage(errorResponse.getStatusText());
        exceptionBuilder.setErrorResponseXml(errorResponseXml);
        exceptionBuilder.setStatusCode(statusCode);
        exceptionBuilder.setTraceId(headers.get(Headers.TRACE_ID));
        exceptionBuilder.setRequestId(headers.get(Headers.REQUEST_ID));
        exceptionBuilder.setErrorCode(statusCode + " " + errorResponse.getStatusText());
        return exceptionBuilder.build();
    }

    @Override
    public CosServiceException handle(CosHttpResponse httpResponse) throws XMLStreamException {
        final InputStream is = httpResponse.getContent();
        String xmlContent = null;
        /*
         * We don't always get an error response body back from COS. When we send a HEAD request, we
         * don't receive a body, so we'll have to just return what we can.
         */
        if (is == null || httpResponse.getRequest().getHttpMethod() == HttpMethodName.HEAD) {
            return createExceptionFromHeaders(httpResponse, null);
        }

        String content = null;
        try {
            content = IOUtils.toString(is);
        } catch (IOException ioe) {
            log.debug("Failed in parsing the error response : ", ioe);
            return createExceptionFromHeaders(httpResponse, null);
        }

        XMLStreamReader reader;
        synchronized (xmlInputFactory) {
            reader = xmlInputFactory
                    .createXMLStreamReader(new ByteArrayInputStream(content.getBytes(UTF8)));
        }

        try {
            /*
             * target depth is to determine if the XML Error response from the server has any
             * element inside <Error> tag have child tags. Parsing such tags is not supported now.
             * target depth is incremented for every start tag and decremented after every end tag
             * is encountered.
             */
            int targetDepth = 0;
            final CosServiceExceptionBuilder exceptionBuilder = new CosServiceExceptionBuilder();
            exceptionBuilder.setErrorResponseXml(content);
            exceptionBuilder.setStatusCode(httpResponse.getStatusCode());

            boolean hasErrorTagVisited = false;
            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        targetDepth++;
                        String tagName = reader.getLocalName();
                        if (targetDepth == 1 && !COSErrorTags.Error.toString().equals(tagName))
                            return createExceptionFromHeaders(httpResponse,
                                    "Unable to parse error response. Error XML Not in proper format."
                                            + content);
                        if (COSErrorTags.Error.toString().equals(tagName)) {
                            hasErrorTagVisited = true;
                        }
                        continue;
                    case XMLStreamConstants.CHARACTERS:
                        xmlContent = reader.getText();
                        if (xmlContent != null)
                            xmlContent = xmlContent.trim();
                        continue;
                    case XMLStreamConstants.END_ELEMENT:
                        tagName = reader.getLocalName();
                        targetDepth--;
                        if (!(hasErrorTagVisited) || targetDepth > 1) {
                            return createExceptionFromHeaders(httpResponse,
                                    "Unable to parse error response. Error XML Not in proper format."
                                            + content);
                        }
                        if (COSErrorTags.Message.toString().equals(tagName)) {
                            exceptionBuilder.setErrorMessage(xmlContent);
                        } else if (COSErrorTags.Code.toString().equals(tagName)) {
                            exceptionBuilder.setErrorCode(xmlContent);
                        } else if (COSErrorTags.RequestId.toString().equals(tagName)) {
                            exceptionBuilder.setRequestId(xmlContent);
                        } else if (COSErrorTags.TraceId.toString().equals(tagName)) {
                            exceptionBuilder.setTraceId(xmlContent);
                        } else {
                            exceptionBuilder.addAdditionalDetail(tagName, xmlContent);
                        }
                        continue;
                    case XMLStreamConstants.END_DOCUMENT:
                        return exceptionBuilder.build();
                }
            }
        } catch (Exception e) {
            log.debug("Failed in parsing the error response : " + content, e);
        }
        return createExceptionFromHeaders(httpResponse, content);
    }

    @Override
    public boolean needsConnectionLeftOpen() {
        return false;
    }

}
