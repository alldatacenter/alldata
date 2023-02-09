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
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Bucket configuration options for hosting static websites entirely out of COS
 */
public class BucketWebsiteConfiguration implements Serializable {

    /**
     * The document to serve when a directory is specified (ex: index.html).
     * This path is relative to the requested resource.
     */
    private String indexDocumentSuffix;

    /** The complete path to the document to serve for 4xx errors. */
    private String errorDocument;

    /**
     * Container for redirect information where all requests will be redirect
     * to. You can redirect requests to another host, to another page, or with
     * another protocol. In the event of an error, you can can specify a
     * different error code to return. .
     */
    private RedirectRule redirectAllRequestsTo;

    /**
     * The list of routing rules that can be used for configuring redirects if
     * certain conditions are meet.
     */
    private List<RoutingRule> routingRules = new LinkedList<RoutingRule>();

    /**
     * Creates a new BucketWebsiteConfiguration.
     */
    public BucketWebsiteConfiguration() {
    }

    /**
     * Creates a new BucketWebsiteConfiguration with the specified index
     * document suffix.
     *
     * @param indexDocumentSuffix
     *            The document to serve when a directory is specified (ex:
     *            index.html). This path is relative to the requested resource.
     */
    public BucketWebsiteConfiguration(String indexDocumentSuffix) {
        this.indexDocumentSuffix = indexDocumentSuffix;
    }

    /**
     * Creates a new BucketWebsiteConfiguration with the specified index
     * document suffix and error document.
     *
     * @param indexDocumentSuffix
     *            The document to serve when a directory is specified (ex:
     *            index.html). This path is relative to the requested resource.
     * @param errorDocument
     *            The complete path to the document to serve for 4xx errors.
     */
    public BucketWebsiteConfiguration(String indexDocumentSuffix, String errorDocument) {
        this.indexDocumentSuffix = indexDocumentSuffix;
        this.errorDocument = errorDocument;
    }

    /**
     * Returns the document to serve when a directory is specified (ex:
     * index.html). This path is relative to the requested resource.
     *
     * @return The document to serve when a directory is specified (ex:
     *         index.html). This path is relative to the requested resource.
     */
    public String getIndexDocumentSuffix() {
        return indexDocumentSuffix;
    }

    /**
     * Sets the document to serve when a directory is specified (ex:
     * index.html). This path is relative to the requested resource.
     *
     * @param indexDocumentSuffix
     *            The document to serve when a directory is specified (ex:
     *            index.html). This path is relative to the requested resource.
     */
    public void setIndexDocumentSuffix(String indexDocumentSuffix) {
        this.indexDocumentSuffix = indexDocumentSuffix;
    }

    /**
     * Returns the complete path to the document to serve for 4xx errors, or
     * null if no error document has been configured.
     *
     * @return The complete path to the document to serve for 4xx errors, or
     *         null if no error document has been configured.
     */
    public String getErrorDocument() {
        return errorDocument;
    }

    /**
     * Sets the complete path to the document to serve for 4xx errors.
     *
     * @param errorDocument
     *            The complete path to the document to serve for 4xx errors.
     */
    public void setErrorDocument(String errorDocument) {
        this.errorDocument = errorDocument;
    }

    /**
     * Sets the redirect information where all requests will be redirect to.
     *
     * @param redirectAllRequestsTo
     *            The Redirect information where all requests will be redirect
     *            to.
     */
    public void setRedirectAllRequestsTo(RedirectRule redirectAllRequestsTo) {
        this.redirectAllRequestsTo = redirectAllRequestsTo;
    }

    /**
     * Return the redirect information where all requests will be redirect to.
     */
    public RedirectRule getRedirectAllRequestsTo() {
        return redirectAllRequestsTo;
    }

    /**
     * Sets the redirect information where all requests will be redirect to and
     * returns a reference to this object(BucketWebsiteConfiguration) for method
     * chaining.
     *
     * @param redirectAllRequestsTo
     *            The Redirect information where all requests will be redirect
     *            to.
     * @return a reference to this object(BucketWebsiteConfiguration) for method
     *         chaining.
     */
    public BucketWebsiteConfiguration withRedirectAllRequestsTo(RedirectRule redirectAllRequestsTo) {
        this.redirectAllRequestsTo = redirectAllRequestsTo;
        return this;
    }

    /**
     * Set the list of routing rules that can be used for configuring redirects
     * if certain conditions are meet.
     *
     * @param routingRules
     *            The list of routing rules that can be used for configuring
     *            redirects.
     */
    public void setRoutingRules(List<RoutingRule> routingRules) {
        this.routingRules = routingRules;
    }

    /**
     * Return the list of routing rules that can be used for configuring
     * redirects if certain conditions are meet.
     */
    public List<RoutingRule> getRoutingRules() {
        return routingRules;
    }

    /**
     * Set the list of routing rules that can be used for configuring redirects
     * if certain conditions are meet and returns a reference to this
     * object(BucketWebsiteConfiguration) for method chaining.
     *
     * @param routingRules
     *            The list of routing rules that can be used for configuring
     *            redirects.
     * @return A reference to this object(BucketWebsiteConfiguration) for method
     *         chaining.
     *
     */
    public BucketWebsiteConfiguration withRoutingRules(List<RoutingRule> routingRules) {
        this.routingRules = routingRules;
        return this;
    }
}
