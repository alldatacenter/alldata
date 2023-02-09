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


package com.qcloud.cos.endpoint;

/**
 * resolve the endpoint like dns.
 *
 */

public interface EndpointResolver {
    /**
     * return the general api endpoint addr, the result can be ip or ip:port. if you don't do
     * anything like DefaultEndpointResolver. the http library will implement the dns resolve.
     * 
     * @param generalApiEndpoint
     * @return the endpoint addr
     */
    public String resolveGeneralApiEndpoint(String generalApiEndpoint);

    /**
     * return the get service api endpoint addr, the result can be ip or ip:port. if you don't do
     * anything like DefaultEndpointResolver. the http library will implement the dns resolve.
     * 
     * @param getServiceApiEndpoint
     * @return the endpoint addr
     */
    public String resolveGetServiceApiEndpoint(String getServiceApiEndpoint);
}
