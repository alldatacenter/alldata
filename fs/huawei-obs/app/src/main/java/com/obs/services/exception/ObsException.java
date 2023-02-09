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

package com.obs.services.exception;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This exception is thrown when the interface fails to be called or access to
 * OBS fails.
 */
public class ObsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String xmlMessage = null;

    private String errorCode = null;

    private String errorMessage = null;

    private String errorRequestId = null;

    private String errorHostId = null;

    private Map<String, String> responseHeaders = null;

    private int responseCode = -1;

    private String responseStatus = null;

    private String errorIndicator = null;

    public ObsException(String message) {
        this(message, null, null);
    }

    public ObsException(String message, Throwable e) {
        this(message, null, e);
    }

    public ObsException(String message, String xmlMessage) {
        this(message, xmlMessage, null);
    }

    public ObsException(String message, String xmlMessage, Throwable cause) {
        super(message, cause);
        if (xmlMessage != null) {
            parseXmlMessage(xmlMessage);
        }
    }

    @Override
    public String toString() {
        String myString = super.toString();

        if (responseCode != -1) {
            myString += " -- ResponseCode: " + responseCode + ", ResponseStatus: " + responseStatus;
        }
        if (isParsedFromXmlMessage()) {
            myString += ", XML Error Message: " + xmlMessage;
        } else if (errorRequestId != null) {
            myString += ", RequestId: " + errorRequestId + ", HostId: " + errorHostId;
        }
        return myString;
    }

    private boolean isParsedFromXmlMessage() {
        return xmlMessage != null;
    }

    private String findXmlElementText(String xmlMsg, String elementName) {
        Pattern pattern = Pattern.compile(".*<" + elementName + ">(.*)</" + elementName + ">.*");
        Matcher matcher = pattern.matcher(xmlMsg);
        if (matcher.matches() && matcher.groupCount() == 1) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private void parseXmlMessage(String xmlMsg) {
        xmlMsg = xmlMsg.replaceAll("\n", "");
        this.xmlMessage = xmlMsg;

        this.errorCode = findXmlElementText(xmlMsg, "Code");
        this.errorMessage = findXmlElementText(xmlMsg, "Message");
        this.errorRequestId = findXmlElementText(xmlMsg, "RequestId");
        this.errorHostId = findXmlElementText(xmlMsg, "HostId");
    }

    public String getXmlMessage() {
        return xmlMessage;
    }

    public void setXmlMessage(String xmlMessage) {
        this.xmlMessage = xmlMessage;
    }

    /**
     * Obtain the error code returned by the OBS server.
     * 
     * @return Error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Obtain the error details returned by the OBS server.
     * 
     * @return Error details
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Obtain the request ID returned by the OBS server.
     * 
     * @return Request ID
     */
    public String getErrorRequestId() {
        return errorRequestId;
    }

    public void setErrorRequestId(String errorRequestId) {
        this.errorRequestId = errorRequestId;
    }

    /**
     * Obtain the server ID.
     * 
     * @return Server ID
     */
    public String getErrorHostId() {
        return errorHostId;
    }

    public void setErrorHostId(String errorHostId) {
        this.errorHostId = errorHostId;
    }

    /**
     * Obtain the response headers of the HTTP request returned by the OBS
     * server.
     * 
     * @return HTTP response headers
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Obtain the HTTP status code returned by the OBS server.
     * 
     * @return HTTP status code
     */
    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Obtain the HTTP response description returned by the OBS server.
     * 
     * @return HTTP response description
     */
    public String getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(String responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * Obtain the detailed error indicators returned by the OBS server.
     * 
     * @return OBS Detailed error indicators returned by the server
     */
    public String getErrorIndicator() {
        return errorIndicator;
    }

    public void setErrorIndicator(String errorIndicator) {
        this.errorIndicator = errorIndicator;
    }

}
