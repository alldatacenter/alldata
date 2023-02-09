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


package com.qcloud.cos.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.internal.Constants;

public class UrlEncoderUtils {

    private static final String PATH_DELIMITER = "/";
    private static final String ENCODE_DELIMITER = "%2F";
    private static final Logger log = LoggerFactory.getLogger(UrlEncoderUtils.class);

    public static String encode(String originUrl) {
        try {
            return URLEncoder.encode(originUrl, Constants.DEFAULT_ENCODING).replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            log.error("URLEncoder error, exception: {}", e);
        }
        return null;
    }

    // encode路径, 不包括分隔符
    public static String encodeEscapeDelimiter(String urlPath) {
        StringBuilder pathBuilder = new StringBuilder();
        String[] pathSegmentsArr = urlPath.split(PATH_DELIMITER);

        boolean isFirstSegMent = true;
        for (String pathSegment : pathSegmentsArr) {
            if (isFirstSegMent) {
                pathBuilder.append(encode(pathSegment));
                isFirstSegMent = false;
            } else {
                pathBuilder.append(PATH_DELIMITER).append(encode(pathSegment));
            }
        }
        if (urlPath.endsWith(PATH_DELIMITER)) {
            pathBuilder.append(PATH_DELIMITER);
        }
        return pathBuilder.toString();
    }
    
    // encode url path, replace the continuous slash with %2F except the first slash
    public static String encodeUrlPath(String urlPath) {
        if(urlPath.length() <= 1) {
            return urlPath;
        }

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(PATH_DELIMITER);
        int start = 1, end = 1;
        while(end < urlPath.length()) {
            if('/' == urlPath.charAt(end)) {
                if('/' == urlPath.charAt(end - 1)) {
                    pathBuilder.append(ENCODE_DELIMITER);
                } else {
                    pathBuilder.append(encode(urlPath.substring(start, end))).append(PATH_DELIMITER);
                }
                start = end + 1;
            }
            end++;
        }
        if(start < end) {
            pathBuilder.append(encode(urlPath.substring(start, end)));
        }
        return pathBuilder.toString();
    }
    
    /**
     * Decode a string for use in the path of a URL; uses URLDecoder.decode,
     * which decodes a string for use in the query portion of a URL.
     *
     * @param value The value to decode
     * @return The decoded value if parameter is not null, otherwise, null is returned.
     */
    public static String urlDecode(final String value) {
        if (value == null) {
            return null;
        }

        try {
            return URLDecoder.decode(value, Constants.DEFAULT_ENCODING);

        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

}
