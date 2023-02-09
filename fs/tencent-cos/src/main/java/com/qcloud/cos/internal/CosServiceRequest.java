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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.event.ProgressListener;

public class CosServiceRequest implements Cloneable, ReadLimitInfo  {
    /**
     * Request related key information
     */
    private COSCredentials cosCredentials;
    /**
     * The optional progress listener for receiving updates about the progress of the request.
     */

    private ProgressListener progressListener = ProgressListener.NOOP;
    /**
     * The mannually set cos server ip and port, format is ip:port
     */
    private String fixedEndpointAddr;

    /**
     * A map of custom header names to header values.
     */
    private Map<String, String> customRequestHeaders;

    /**
     * Custom query parameters for the request.
     */
    private Map<String, List<String>> customQueryParameters;

    private final RequestClientOptions requestClientOptions = new RequestClientOptions();
    
    /**
     * The source object from which the current object was cloned; or null if there isn't one.
     */
    private CosServiceRequest cloneSource;

    /**
     * Sets the optional progress listener for receiving updates about the progress of the request.
     *
     * @param progressListener
     *            The new progress listener.
     */
    public void setGeneralProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener == null ? ProgressListener.NOOP : progressListener;
    }

    /**
     * Returns the optional progress listener for receiving updates about the progress of the
     * request.
     *
     * @return the optional progress listener for receiving updates about the progress of the
     *         request.
     */
    public ProgressListener getGeneralProgressListener() {
        return progressListener;
    }

    /**
     * Sets the cos server ip and port .
     *
     * @param fixedEndpointAddr
     *              ip and port string, format is ip:port
     */
    public void setFixedEndpointAddr(String fixedEndpointAddr) {
        this.fixedEndpointAddr = fixedEndpointAddr;
    }

    /**
     * Returns the setted server ip and port.
     *
     * @return The setted ip and port string
     */
    public String getFixedEndpointAddr() {
        return fixedEndpointAddr;
    }
    /**
     * Sets the optional progress listener for receiving updates about the progress of the request,
     * and returns a reference to this object so that method calls can be chained together.
     *
     * @param progressListener
     *            The new progress listener.
     * @return A reference to this updated object so that method calls can be chained together.
     */
    public <T extends CosServiceRequest> T withGeneralProgressListener(ProgressListener progressListener) {
        setGeneralProgressListener(progressListener);
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }
    
    /**
     * Returns an immutable map of custom header names to header values.
     *
     * @return The immutable map of custom header names to header values.
     */
    public Map<String, String> getCustomRequestHeaders() {
        if (customRequestHeaders == null) {
            return null;
        }
        return Collections.unmodifiableMap(customRequestHeaders);
    }

    /**
     * Put a new custom header to the map of custom header names to custom header values, and return
     * the previous value if the header has already been set in this map.
     * <p>
     * NOTE: Custom header values set via this method will overwrite any conflicting values coming
     * from the request parameters.
     *
     * @param name
     *            The name of the header to add
     * @param value
     *            The value of the header to add
     * @return the previous value for the name if it was set, null otherwise
     */
    public String putCustomRequestHeader(String name, String value) {
        if (customRequestHeaders == null) {
            customRequestHeaders = new HashMap<String, String>();
        }
        if (name == null) {
            throw new IllegalArgumentException("custom header key cann't be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("custom header value cann't be null");
        }
        return customRequestHeaders.put(name, value);
    }
    
    /**
     * @return the immutable map of custom query parameters. The parameter value is modeled as a
     *         list of strings because multiple values can be specified for the same parameter name.
     */
    public Map<String, List<String>> getCustomQueryParameters() {
        if (customQueryParameters == null) {
            return null;
        }
        return Collections.unmodifiableMap(customQueryParameters);
    }

    /**
     * Add a custom query parameter for the request. Since multiple values are allowed for the same
     * query parameter, this method does NOT overwrite any existing parameter values in the request.
     *
     * @param name
     *            The name of the query parameter
     * @param value
     *            The value of the query parameter. Only the parameter name will be added in the URI
     *            if the value is set to null. For example, putCustomQueryParameter("param", null)
     *            will be serialized to "?param", while putCustomQueryParameter("param", "") will be
     *            serialized to "?param=".
     */
    public void putCustomQueryParameter(String name, String value) {
        if (customQueryParameters == null) {
            customQueryParameters = new HashMap<String, List<String>>();
        }
        List<String> paramList = customQueryParameters.get(name);
        if (paramList == null) {
            paramList = new LinkedList<String>();
            customQueryParameters.put(name, paramList);
        }
        paramList.add(value);
    }
    
    /**
     * Gets the options stored with this request object. Intended for internal use only.
     */
    public RequestClientOptions getRequestClientOptions() {
        return requestClientOptions;
    }
    
    /**
     * Copies the internal state of this base class to that of the target request.
     *
     * @return the target request
     */
    protected final <T extends CosServiceRequest> T copyBaseTo(T target) {
        if (customRequestHeaders != null) {
            for (Map.Entry<String, String> e : customRequestHeaders.entrySet())
                target.putCustomRequestHeader(e.getKey(), e.getValue());
        }
        if (customQueryParameters != null) {
            for (Map.Entry<String, List<String>> e : customQueryParameters.entrySet()) {
                if (e.getValue() != null) {
                    for (String value : e.getValue()) {
                        target.putCustomQueryParameter(e.getKey(), value);
                    }
                }
            }
        }
        target.setGeneralProgressListener(progressListener);
        requestClientOptions.copyTo(target.getRequestClientOptions());
        return target;
    }

    @Override
    public int getReadLimit() {
        return requestClientOptions.getReadLimit();
    }
    
    /**
     * Returns the source object from which the current object was cloned; or null if there isn't
     * one.
     */
    public CosServiceRequest getCloneSource() {
        return cloneSource;
    }

    /**
     * Returns the root object from which the current object was cloned; or null if there isn't one.
     */
    public CosServiceRequest getCloneRoot() {
        CosServiceRequest cloneRoot = cloneSource;
        if (cloneRoot != null) {
            while (cloneRoot.getCloneSource() != null) {
                cloneRoot = cloneRoot.getCloneSource();
            }
        }
        return cloneRoot;
    }

    private void setCloneSource(CosServiceRequest cloneSource) {
        this.cloneSource = cloneSource;
    }
    
    /**
     * Creates a shallow clone of this object for all fields except the handler context. Explicitly does <em>not</em> clone the
     * deep structure of the other fields in the message.
     *
     * @see Object#clone()
     */
    @Override
    public CosServiceRequest clone() {
        try {
            CosServiceRequest cloned = (CosServiceRequest) super.clone();
            cloned.setCloneSource(this);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(
                    "Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

    public COSCredentials getCosCredentials() {
        return cosCredentials;
    }

    public void setCosCredentials(COSCredentials cosCredentials) {
        this.cosCredentials = cosCredentials;
    }
}
