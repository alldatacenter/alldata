/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.login;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.login.ldap.SelfSignedSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class LoginServiceLdapImpl implements LoginService {
    private static final Logger LOGGER = LoggerFactory.getLogger
        (LoginServiceLdapImpl.class);

    private static final String LDAP_FACTORY =
        "com.sun.jndi.ldap.LdapCtxFactory";

    private String url;
    private String email;
    private String searchBase;
    private String searchPattern;
    private SearchControls searchControls;
    private boolean sslSkipVerify;
    private String bindDN;
    private String bindPassword;

    public LoginServiceLdapImpl(String url, String email, String searchBase,
                                String searchPattern, boolean sslSkipVerify,
                                String bindDN, String bindPassword) {
        this.url = url;
        this.email = email;
        this.searchBase = searchBase;
        this.searchPattern = searchPattern;
        this.sslSkipVerify = sslSkipVerify;
        this.bindDN = bindDN;
        this.bindPassword = bindPassword;
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        this.searchControls = searchControls;
    }

    @Override
    public ResponseEntity<Map<String, Object>> login(Map<String, String> map) {
        String username = map.get("username");
        String password = map.get("password");

        // use separate bind credentials, if bindDN is provided
        String bindAccount = StringUtils.isEmpty(this.bindDN) ? username : this.bindDN;
        String bindPassword = StringUtils.isEmpty(this.bindDN) ? password : this.bindPassword;
        String searchFilter = searchPattern.replace("{0}", username);
        LdapContext ctx = null;
        try {
            ctx = getContextInstance(toPrincipal(bindAccount), bindPassword);

            NamingEnumeration<SearchResult> results = ctx.search(searchBase,
                searchFilter, searchControls);
            SearchResult userObject = getSingleUser(results);

            // verify password if different bind user is used
            if (!StringUtils.equals(username, bindAccount)) {
                String userDN = getAttributeValue(userObject, "distinguishedName", toPrincipal(username));
                checkPassword(userDN, password);
            }

            Map<String, Object> message = new HashMap<>();
            message.put("ntAccount", username);
            message.put("fullName", getFullName(userObject, username));
            message.put("status", 0);
            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (AuthenticationException e) {
            LOGGER.warn("User {} failed to login with LDAP auth. {}", username,
                e.getMessage());
        } catch (NamingException e) {
            LOGGER.warn(String.format("User %s failed to login with LDAP auth.", username), e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    LOGGER.debug("Failed to close LDAP context", e);
                }
            }
        }
        return null;
    }

    private void checkPassword(String name, String password) throws NamingException {
        getContextInstance(name, password).close();
    }

    private SearchResult getSingleUser(NamingEnumeration<SearchResult> results) throws NamingException {
        if (!results.hasMoreElements()) {
            throw new AuthenticationException("User does not exist or not allowed by search string");
        }
        SearchResult result = results.nextElement();
        if (results.hasMoreElements()) {
            SearchResult second = results.nextElement();
            throw new NamingException(String.format("Ambiguous search, found two users: %s, %s",
                result.getNameInNamespace(), second.getNameInNamespace()));
        }
        return result;
    }

    private String getAttributeValue(SearchResult searchResult, String key, String defaultValue) throws NamingException {
        Attributes attrs = searchResult.getAttributes();
        if (attrs == null) {
            return defaultValue;
        }
        Attribute attrObj = attrs.get(key);
        if (attrObj == null) {
            return defaultValue;
        }
        try {
            return (String) attrObj.get();
        } catch (NoSuchElementException e) {
            return defaultValue;
        }
    }

    private String getFullName(SearchResult searchResult, String ntAccount) {
        try {
            String cnName = getAttributeValue(searchResult, "cn", null);
            if (cnName.indexOf("(") > 0) {
                return cnName.substring(0, cnName.indexOf("("));
            } else {
                // old behavior ignores CNs without "("
                return ntAccount;
            }
        } catch (NamingException e) {
            LOGGER.warn("User {} successfully login with LDAP auth, " +
                "but failed to get full name.", ntAccount);
            return ntAccount;
        }
    }

    private String toPrincipal(String ntAccount) {
        if (ntAccount.toUpperCase().startsWith("CN=")) {
            return ntAccount;
        } else {
            return ntAccount + email;
        }
    }

    private LdapContext getContextInstance(String principal, String password)
        throws NamingException {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_FACTORY);
        ht.put(Context.PROVIDER_URL, url);
        ht.put(Context.SECURITY_PRINCIPAL, principal);
        ht.put(Context.SECURITY_CREDENTIALS, password);
        if (url.startsWith("ldaps") && sslSkipVerify) {
            ht.put("java.naming.ldap.factory.socket", SelfSignedSocketFactory.class.getName());
        }
        return new InitialLdapContext(ht, null);
    }
}
