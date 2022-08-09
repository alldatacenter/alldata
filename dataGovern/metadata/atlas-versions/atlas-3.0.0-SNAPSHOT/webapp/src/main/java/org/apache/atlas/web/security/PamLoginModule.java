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

package org.apache.atlas.web.security;

import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PamLoginModule extends Object implements LoginModule {
    private static final Logger LOG = LoggerFactory.getLogger(PamLoginModule.class);

    public static final String SERVICE_KEY = "service";

    private PAM pam;
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;

    private String username;
    private String password;

    private boolean authSucceeded;
    private PamPrincipal principal;

    public PamLoginModule()
    {
        super();
        authSucceeded = false;
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options)
    {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = new HashMap<>(options);
    }

    @Override
    public boolean login() throws LoginException
    {
        initializePam();
        obtainUserAndPassword();
        return performLogin();
    }

    private void initializePam() throws LoginException
    {
        String service = (String) options.get(SERVICE_KEY);
        if (service == null)
        {
            throw new LoginException("Error: PAM service was not defined");
        }
        createPam(service);
    }

    private void createPam(String service) throws LoginException
    {
        try
        {
            pam = new PAM(service);
        }
        catch (PAMException ex)
        {
            LoginException le = new LoginException("Error initializing PAM");
            le.initCause(ex);
            throw le;
        }
    }

    private void obtainUserAndPassword() throws LoginException
    {
        if (callbackHandler == null)
        {
            throw new LoginException("Error: no CallbackHandler available  to gather authentication information from the user");
        }

        try
        {
            NameCallback nameCallback = new NameCallback("username");
            PasswordCallback passwordCallback = new PasswordCallback("password", false);

            invokeCallbackHandler(nameCallback, passwordCallback);

            initUserName(nameCallback);
            initPassword(passwordCallback);

            if (LOG.isDebugEnabled())
                LOG.debug("Searching for user " + nameCallback.getName());
        }
        catch (IOException | UnsupportedCallbackException ex)
        {
            LoginException le = new LoginException("Error in callbacks");
            le.initCause(ex);
            throw le;
        }
    }

    private void invokeCallbackHandler(NameCallback nameCallback, PasswordCallback passwordCallback) throws IOException, UnsupportedCallbackException
    {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = nameCallback;
        callbacks[1] = passwordCallback;

        callbackHandler.handle(callbacks);
    }

    private void initUserName(NameCallback nameCallback)
    {
        username = nameCallback.getName();
    }

    private void initPassword(PasswordCallback passwordCallback)
    {
        char[] password = passwordCallback.getPassword();
        if (password != null) {
            this.password = new String(password);
        }
        passwordCallback.clearPassword();
    }

    private boolean performLogin() throws LoginException
    {
        try
        {
            UnixUser user = pam.authenticate(username, password);
            principal = new PamPrincipal(user);
            authSucceeded = true;

            if (LOG.isDebugEnabled())
                LOG.debug("user " + username );
            return true;
        }
        catch (PAMException ex)
        {
            LoginException le = new FailedLoginException("Invalid username or password");
            le.initCause(ex);
            throw le;
        }
    }

    @Override
    public boolean commit() throws LoginException
    {
        if (authSucceeded == false)
        {
            return false;
        }

        if (subject.isReadOnly())
        {
            cleanup();
            throw new LoginException("Subject is read-only");
        }

        Set<Principal> principals = subject.getPrincipals();
        if (principals.contains(principal) == false)
        {
            principals.add(principal);
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException
    {
        if (authSucceeded == false)
        {
            return false;
        }

        cleanup();
        return true;
    }

    @Override
    public boolean logout() throws LoginException
    {
        if (subject.isReadOnly())
        {
            cleanup();
            throw new LoginException("Subject is read-only");
        }

        subject.getPrincipals().remove(principal);

        cleanup();
        return true;
    }

    private void cleanup()
    {
        authSucceeded = false;
        username = null;
        password = null;
        principal = null;
        pam.dispose();
    }
}
