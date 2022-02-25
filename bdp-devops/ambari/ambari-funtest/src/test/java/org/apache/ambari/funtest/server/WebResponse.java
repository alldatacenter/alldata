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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.funtest.server;

/**
 * Gets a response from a URL.
 */
public class WebResponse {
    private String content;
    private int statusCode;

    public WebResponse() {}

    /**
     * Gets the response content.
     *
     * @return - Response content.
     */
    public String getContent() { return this.content; }

    /**
     * Sets the response content.
     *
     * @param content - Response content.
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Gets the response status code.
     *
     * @return - Response status code.
     */
    public int getStatusCode() { return this.statusCode; }

    /**
     * Sets the response status code.
     *
     * @param statusCode - Response status code.
     */
    public void setStatusCode(int statusCode) { this.statusCode = statusCode;}
}
