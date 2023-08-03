/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.credutils.kerberos;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;

public class KerberosCredentialsProvider implements CredentialsProvider {
    private AuthScope authScope;
    private Credentials credentials;

    @Override
    public void setCredentials(AuthScope authscope, Credentials credentials) {
        if (authscope.getScheme().regionMatches(true, 0, AuthSchemes.SPNEGO, 0, AuthSchemes.SPNEGO.length()) == false) {
            throw new IllegalArgumentException("Only " + AuthSchemes.SPNEGO + " auth scheme is supported in AuthScope");
        }
        this.authScope = authscope;
        this.credentials = credentials;
    }

    @Override
    public Credentials getCredentials(AuthScope authscope) {
        assert this.authScope != null && authscope != null;
        return authscope.match(this.authScope) > -1 ? this.credentials : null;
    }

    @Override
    public void clear() {
        this.authScope = null;
        this.credentials = null;
    }

}
