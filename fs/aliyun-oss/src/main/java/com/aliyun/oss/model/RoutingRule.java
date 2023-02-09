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

package com.aliyun.oss.model;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSErrorCode;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * A rule that identifies a condition and the redirect that is applied when the
 * condition is met.
 * 
 */
public class RoutingRule {

    /**
     * Container for describing a condition that must be met for the specified
     * redirect to be applied. If the routing rule does not include a condition,
     * the rule is applied to all requests.
     */
    public static class Condition {
        public String getKeyPrefixEquals() {
            return keyPrefixEquals;
        }

        public void setKeyPrefixEquals(String keyPrefixEquals) {
            this.keyPrefixEquals = keyPrefixEquals;
        }

        public Integer getHttpErrorCodeReturnedEquals() {
            return httpErrorCodeReturnedEquals;
        }

        public void setHttpErrorCodeReturnedEquals(Integer httpErrorCodeReturnedEquals) {
            if (httpErrorCodeReturnedEquals == null) {
                return;
            }

            if (httpErrorCodeReturnedEquals <= 0) {
                throw new IllegalArgumentException(MessageFormat.format("HttpErrorCodeReturnedEqualsInvalid",
                        "HttpErrorCodeReturnedEquals should be greater than 0"));
            }

            this.httpErrorCodeReturnedEquals = httpErrorCodeReturnedEquals;
        }

        public void ensureConditionValid() {

        }

        public String getKeySuffixEquals() {
            return keySuffixEquals;
        }

        public void setKeySuffixEquals(String keySuffixEquals) {
            this.keySuffixEquals = keySuffixEquals;
        }

        public List<IncludeHeader> getIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(List<IncludeHeader> includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        /**
         * The object key name prefix from which requests will be redirected.
         */
        private String keyPrefixEquals;

        /**
         * The object key name prefix from which requests will be redirected.
         */
        private String keySuffixEquals;

        /**
         * The HTTP error code that must match for the redirect to apply. In the
         * event of an error, if the error code meets this value, then specified
         * redirect applies.
         */
        private Integer httpErrorCodeReturnedEquals;

        private List<IncludeHeader> includeHeaders;
    }

    public static class IncludeHeader {
        /**
         * name of header
         */
        private String key;

        /**
         * key should be equal to the given value
         */
        private String equals;

        /**
         * key should be start with the given value
         */
        private String startsWith;

        /**
         * key should be end with the given value
         */
        private String endsWith;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getEquals() {
            return equals;
        }

        public void setEquals(String equals) {
            this.equals = equals;
        }

        public String getStartsWith() {
            return startsWith;
        }

        public void setStartsWith(String startsWith) {
            this.startsWith = startsWith;
        }

        public String getEndsWith() {
            return endsWith;
        }

        public void setEndsWith(String endsWith) {
            this.endsWith = endsWith;
        }
    }

    public static enum RedirectType {
        /**
         * Internal mode is not supported yet.
         */
         Internal("Internal"),

        /**
         * 302 redirect.
         */
        External("External"),

        /**
         * AliCDN
         */
        AliCDN("AliCDN"),

        /**
         * Means OSS would read the source data on user's behalf and store it in
         * OSS for later access.
         */
        Mirror("Mirror");

        private String redirectTypeString;

        private RedirectType(String redirectTypeString) {
            this.redirectTypeString = redirectTypeString;
        }

        @Override
        public String toString() {
            return this.redirectTypeString;
        }

        public static RedirectType parse(String redirectTypeString) {
            for (RedirectType rt : RedirectType.values()) {
                if (rt.toString().equals(redirectTypeString)) {
                    return rt;
                }
            }

            throw new IllegalArgumentException("Unable to parse " + redirectTypeString);
        }
    }

    public static enum Protocol {
        Http("http"), Https("https");

        private String protocolString;

        private Protocol(String protocolString) {
            this.protocolString = protocolString;
        }

        @Override
        public String toString() {
            return this.protocolString;
        }

        public static Protocol parse(String protocolString) {
            for (Protocol protocol : Protocol.values()) {
                if (protocol.toString().equals(protocolString)) {
                    return protocol;
                }
            }

            throw new IllegalArgumentException("Unable to parse " + protocolString);
        }
    }

    public static class MirrorHeaders{

        /**
         * Flags of passing all headers to source site.
         */
        private boolean passAll;

        /**
         * Only headers include in list can be passed to source site.
         */
        private List<String> pass;

        /**
         * Headers include in list cannot be passed to source site.
         */
        private List<String> remove;

        /**
         * Define the value for some headers.
         */
        private List<Map<String, String>> set;

        public boolean isPassAll() {
            return passAll;
        }

        public void setPassAll(boolean passAll) {
            this.passAll = passAll;
        }

        public List<String> getPass() {
            return pass;
        }

        public void setPass(List<String> pass) {
            this.pass = pass;
        }

        public List<String> getRemove() {
            return remove;
        }

        public void setRemove(List<String> remove) {
            this.remove = remove;
        }

        public List<Map<String, String>> getSet() {
            return set;
        }

        public void setSet(List<Map<String, String>> set) {
            this.set = set;
        }
    }

    /**
     * Container element that provides instructions for redirecting the request.
     * You can redirect requests to another host, or another page, or you can
     * specify another protocol to use.
     *
     */
    public static class Redirect {
        public static class MirrorMultiAlternate {
            private Integer prior;
            private String url;

            public Integer getPrior() {
                return prior;
            }

            public void setPrior(Integer prior) throws ClientException {
                if (prior < 1 || prior > 10000) {
                    throw new ClientException("The specified prior is not valid", OSSErrorCode.INVALID_ARGUMENT, null);
                }
                this.prior = prior;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }

        public RedirectType getRedirectType() {
            return redirectType;
        }

        public void setRedirectType(RedirectType redirectType) {
            this.redirectType = redirectType;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        public void setProtocol(Protocol protocol) {
            this.protocol = protocol;
        }

        public String getReplaceKeyPrefixWith() {
            return replaceKeyPrefixWith;
        }

        public void setReplaceKeyPrefixWith(String replaceKeyPrefixWith) {
            this.replaceKeyPrefixWith = replaceKeyPrefixWith;
        }

        public String getReplaceKeyWith() {
            return replaceKeyWith;
        }

        public void setReplaceKeyWith(String replaceKeyWith) {
            this.replaceKeyWith = replaceKeyWith;
        }

        public Integer getHttpRedirectCode() {
            return httpRedirectCode;
        }

        public void setHttpRedirectCode(Integer httpRedirectCode) {
            if (httpRedirectCode == null) {
                return;
            }

            if (httpRedirectCode < 300 || httpRedirectCode > 399) {
                throw new IllegalArgumentException(MessageFormat.format("RedirectHttpRedirectCodeInvalid",
                        "HttpRedirectCode must be a valid HTTP 3xx status code."));
            }

            this.httpRedirectCode = httpRedirectCode;
        }

        public String getMirrorURL() {
            return mirrorURL;
        }

        public void setMirrorURL(String mirrorURL) {
            this.mirrorURL = mirrorURL;
        }

        public List<MirrorMultiAlternate> getMirrorMultiAlternates() {
            return mirrorMultiAlternates;
        }

        public void setMirrorMultiAlternates(List<MirrorMultiAlternate> mirrorMultiAlternates) {
            this.mirrorMultiAlternates = mirrorMultiAlternates;
        }

        public String getMirrorSecondaryURL() {
            return mirrorSecondaryURL;
        }

        public void setMirrorSecondaryURL(String mirrorSecondaryURL) {
            this.mirrorSecondaryURL = mirrorSecondaryURL;
        }

        public String getMirrorProbeURL() {
            return mirrorProbeURL;
        }

        public void setMirrorProbeURL(String mirrorProbeURL) {
            this.mirrorProbeURL = mirrorProbeURL;
        }

        public Boolean isMirrorPassQueryString() {
            return mirrorPassQueryString;
        }

        public void setMirrorPassQueryString(Boolean mirrorPassQueryString) {
            this.mirrorPassQueryString = mirrorPassQueryString;
        }

        public Boolean isPassQueryString() {
            return passQueryString;
        }

        public void setPassQueryString(Boolean passQueryString) {
            this.passQueryString = passQueryString;
        }

        public Boolean isPassOriginalSlashes() {
            return passOriginalSlashes;
        }

        public void setPassOriginalSlashes(Boolean passOriginalSlashes) {
            this.passOriginalSlashes = passOriginalSlashes;
        }


        public Boolean isMirrorFollowRedirect() {
            return mirrorFollowRedirect;
        }

        public void setMirrorFollowRedirect(Boolean mirrorFollowRedirect) {
            this.mirrorFollowRedirect = mirrorFollowRedirect;
        }

        public Boolean isMirrorUserLastModified() {
            return mirrorUserLastModified;
        }

        public void setMirrorUserLastModified(Boolean mirrorUserLastModified) {
            this.mirrorUserLastModified = mirrorUserLastModified;
        }

        public Boolean isMirrorIsExpressTunnel() {
            return mirrorIsExpressTunnel;
        }

        public void setMirrorIsExpressTunnel(Boolean mirrorIsExpressTunnel) {
            this.mirrorIsExpressTunnel = mirrorIsExpressTunnel;
        }

        public String getMirrorDstRegion() {
            return mirrorDstRegion;
        }

        public void setMirrorDstRegion(String mirrorDstRegion) {
            this.mirrorDstRegion = mirrorDstRegion;
        }

        public String getMirrorDstVpcId() {
            return mirrorDstVpcId;
        }

        public void setMirrorDstVpcId(String mirrorDstVpcId) {
            this.mirrorDstVpcId = mirrorDstVpcId;
        }

        public MirrorHeaders getMirrorHeaders() {
            return mirrorHeaders;
        }

        public void setMirrorHeaders(MirrorHeaders mirrorHeaders) {
            this.mirrorHeaders = mirrorHeaders;
        }

        public Boolean getMirrorPassQueryString() {
            return mirrorPassQueryString;
        }

        public String getMirrorRole() {
            return mirrorRole;
        }

        public void setMirrorRole(String mirrorRole) {
            this.mirrorRole = mirrorRole;
        }

        public Boolean isMirrorUsingRole() {
            return mirrorUsingRole;
        }

        public void setMirrorUsingRole(Boolean mirrorUsingRole) {
            this.mirrorUsingRole = mirrorUsingRole;
        }

        public Boolean isEnableReplacePrefix() {
            return enableReplacePrefix;
        }

        public void setEnableReplacePrefix(Boolean enableReplacePrefix) {
            this.enableReplacePrefix = enableReplacePrefix;
        }

        public Boolean isMirrorSwitchAllErrors() {
            return mirrorSwitchAllErrors;
        }

        public void setMirrorSwitchAllErrors(Boolean mirrorSwitchAllErrors) {
            this.mirrorSwitchAllErrors = mirrorSwitchAllErrors;
        }

        public Boolean isMirrorCheckMd5() {
            return mirrorCheckMd5;
        }

        public void setMirrorCheckMd5(Boolean mirrorCheckMd5) {
            this.mirrorCheckMd5 = mirrorCheckMd5;
        }

        /**
         * A Redirect element must contain at least one of the following sibling
         * elements.
         */
        public void ensureRedirectValid() {
            if (hostName == null && protocol == null && replaceKeyPrefixWith == null && replaceKeyWith == null
                    && httpRedirectCode == null && mirrorURL == null) {
                throw new IllegalArgumentException(MessageFormat.format("RoutingRuleRedirectInvalid",
                        "Redirect element must contain at least one of the sibling elements"));
            }

            if (replaceKeyPrefixWith != null && replaceKeyWith != null) {
                throw new IllegalArgumentException(MessageFormat.format("RoutingRuleRedirectInvalid",
                        "ReplaceKeyPrefixWith or ReplaceKeyWith only choose one"));
            }

            if (redirectType == RedirectType.Mirror && mirrorURL == null) {
                throw new IllegalArgumentException(
                        MessageFormat.format("RoutingRuleRedirectInvalid", "MirrorURL must have a value"));
            }

            if (redirectType == RedirectType.Mirror) {
                if ((!mirrorURL.startsWith("http://") && !mirrorURL.startsWith("https://"))
                        || !mirrorURL.endsWith("/")) {
                    throw new IllegalArgumentException(
                            MessageFormat.format("RoutingRuleRedirectInvalid", "MirrorURL is invalid", mirrorURL));
                }
            }
        }

        public String getMirrorTunnelId() {
            return mirrorTunnelId;
        }

        public void setMirrorTunnelId(String mirrorTunnelId) {
            this.mirrorTunnelId = mirrorTunnelId;
        }

        /**
         * Redirect type, Internal, External or Mirror
         */
        private RedirectType redirectType;

        /**
         * The host name to be used in the Location header that is returned in
         * the response. HostName is not required if one of its siblings is
         * supplied.
         */
        private String hostName;

        /**
         * The protocol, http or https, to be used in the Location header that
         * is returned in the response. Protocol is not required if one of its
         * siblings is supplied.
         */
        private Protocol protocol;

        /**
         * The object key name prefix that will replace the value of
         * KeyPrefixEquals in the redirect request. ReplaceKeyPrefixWith is not
         * required if one of its siblings is supplied. It can be supplied only
         * if ReplaceKeyWith is not supplied.
         */
        private String replaceKeyPrefixWith;

        /**
         * The object key to be used in the Location header that is returned in
         * the response. ReplaceKeyWith is not required if one of its siblings
         * is supplied. It can be supplied only if ReplaceKeyPrefixWith is not
         * supplied.
         */
        private String replaceKeyWith;

        /**
         * The HTTP redirect code to be used in the Location header that is
         * returned in the response. HttpRedirectCode is not required if one of
         * its siblings is supplied.
         */
        private Integer httpRedirectCode;

        /**
         * MirrorURL is effective when RedirectType is Mirror
         */
        private String mirrorURL;

        /**
         * The secondary URL for mirror. It should be same as mirrorURL. When
         * the primary mirror url is not available, OSS would switch to
         * secondary URL automatically.
         */
        private String mirrorSecondaryURL;

        /**
         * The probe URL for mirror. This is to detect the availability of the
         * primary mirror URL. If it does not return 200, then switch to
         * secondary mirror URL. If it returns 200, switch to primary mirror
         * URL.
         */
        private String mirrorProbeURL;

        /**
         * Flag of passing the query string to the source site. By default it's
         * false. The passQueryString applies to all kind of RoutingRule while the mirrorPassQueryString can only work on Back-to-Origin.
         */
        private Boolean passQueryString;

        /**
         * Flag of passing the query string to the source site. By default it's
         * false.
         */
        private Boolean mirrorPassQueryString;

        /**
         * Flag of passing the redundant backslash between host and uri to
         * source site. By default it's false.
         */
        private Boolean passOriginalSlashes;
        /**
         * Flags of following with the 3xx response from source site. By default it's true.
         */
        private Boolean mirrorFollowRedirect = true;

        /**
         * Flags of accepting the user-setting of lastModifiedTime in the response from source site. By default it's false.
         */
        private Boolean mirrorUserLastModified;

        /**
         * Flags of take high-speed channel on Back-to-Origin. By default it's false.
         */
        private Boolean mirrorIsExpressTunnel;


        /**
         * Need when the mirrorIsExpressTunnel is true, means the destination region for high-speed channel.
         */
        private String mirrorDstRegion;

        /**
         * The vpc id of destination when taking high-speed channel on Back-to-Origin.
         */
        private String mirrorDstVpcId;

        private MirrorHeaders mirrorHeaders;

        private List<MirrorMultiAlternate> mirrorMultiAlternates;

        /**
         * the role of back to private bucket
         */
        private String mirrorRole;

        /**
         * check if use the role to back to private bucket
         */
        private Boolean mirrorUsingRole;

        /**
         * replace or instead
         */
        private Boolean enableReplacePrefix;

        /**
         * MirrorSwitchAllErrors
         */
        private Boolean mirrorSwitchAllErrors;

        /***
         * checkMd5
         */
        private Boolean mirrorCheckMd5;

        /**
         * tunnel
         */
        private String mirrorTunnelId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Redirect getRedirect() {
        return redirect;
    }

    public void setRedirect(Redirect redirect) {
        this.redirect = redirect;
    }

    public void ensureRoutingRuleValid() {
        if (this.number == null || this.number <= 0) {
            throw new IllegalArgumentException(MessageFormat.format("RoutingRuleNumberInvalid", this.number));
        }

        this.redirect.ensureRedirectValid();

        this.condition.ensureConditionValid();
    }

    /**
     * RuleNumber must be a positive integer, can not be continuous, but must be
     * increased, can not be repeated. Condition matching to consider in
     * accordance with the order of rule to do, because it is difficult to
     * ensure that there is no rule between overlap.
     */
    private Integer number;

    /**
     * Container for describing a condition that must be met for the specified
     * redirect to be applied.
     */
    private Condition condition = new Condition();

    /**
     * Container element that provides instructions for redirecting the request.
     */
    private Redirect redirect = new Redirect();
}
