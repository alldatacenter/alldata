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

package org.apache.ranger.authorization.credutils;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.KerberosCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.ranger.authorization.credutils.kerberos.KerberosCredentialsProvider;
import org.apache.ranger.authorization.credutils.kerberos.KeytabJaasConf;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.math.BigDecimal;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class CredentialsProviderUtil {
    private static final Logger logger = LoggerFactory.getLogger(CredentialsProviderUtil.class);
    private static final Oid SPNEGO_OID = getSpnegoOid();
    private static final String CRED_CONF_NAME = "ESClientLoginConf";
    public static long ticketExpireTime80 = 0;

    private static Oid getSpnegoOid() {
        Oid oid = null;
        try {
            oid = new Oid("1.3.6.1.5.5.2");
        } catch (GSSException gsse) {
            throw new RuntimeException(gsse);
        }
        return oid;
    }

    public static KerberosCredentialsProvider getKerberosCredentials(String user, String password){
        KerberosCredentialsProvider credentialsProvider = new KerberosCredentialsProvider();
        final GSSManager gssManager = GSSManager.getInstance();
        try {
            final GSSName gssUserPrincipalName = gssManager.createName(user, GSSName.NT_USER_NAME);
            Subject subject = login(user, password);
            final AccessControlContext acc = AccessController.getContext();
            final GSSCredential credential = doAsPrivilegedWrapper(subject,
                    (PrivilegedExceptionAction<GSSCredential>) () -> gssManager.createCredential(gssUserPrincipalName,
                            GSSCredential.DEFAULT_LIFETIME, SPNEGO_OID, GSSCredential.INITIATE_ONLY),
                    acc);
            credentialsProvider.setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.SPNEGO),
                    new KerberosCredentials(credential));
        } catch (GSSException e) {
            logger.error("GSSException:", e);
            throw new RuntimeException(e);
        } catch (PrivilegedActionException e) {
            logger.error("PrivilegedActionException:", e);
            throw new RuntimeException(e);
        }
        return credentialsProvider;
    }

    public static synchronized KerberosTicket getTGT(Subject subject) {
        Set<KerberosTicket> tickets = subject.getPrivateCredentials(KerberosTicket.class);
        for(KerberosTicket ticket: tickets) {
            KerberosPrincipal server = ticket.getServer();
            if (server.getName().equals("krbtgt/" + server.getRealm() + "@" + server.getRealm())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Client principal is \"" + ticket.getClient().getName() + "\".");
                    logger.debug("Server principal is \"" + ticket.getServer().getName() + "\".");
                }
                return ticket;
            }
        }
        return null;
    }

    public static Boolean ticketWillExpire(KerberosTicket ticket){
        long ticketExpireTime = ticket.getEndTime().getTime();
        long currrentTime = new Date().getTime();
        if (logger.isDebugEnabled()) {
            logger.debug("TicketExpireTime is:" + ticketExpireTime);
            logger.debug("currrentTime is:" + currrentTime);
        }
        if (ticketExpireTime80 == 0) {
            long timeDiff = ticketExpireTime - currrentTime;
            long timeDiff20 = Math.round(Float.parseFloat(BigDecimal.valueOf(timeDiff * 0.2).toPlainString()));
            ticketExpireTime80 = ticketExpireTime - timeDiff20;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("ticketExpireTime80 is:" + ticketExpireTime80);
        }
        if (currrentTime > ticketExpireTime80) {
            if (logger.isDebugEnabled()) {
                logger.debug("Current time is more than 80% of Ticket Expire Time!!");
            }
            ticketExpireTime80 = 0;
            return true;
        }
        return false;
    }

    public static synchronized Subject login(String userPrincipalName, String keytabPath) throws PrivilegedActionException {
             Subject sub = AccessController.doPrivileged((PrivilegedExceptionAction<Subject>) () -> {
                final Subject subject = new Subject(false, Collections.singleton(new KerberosPrincipal(userPrincipalName)),
                        Collections.emptySet(), Collections.emptySet());
                Configuration conf = new KeytabJaasConf(userPrincipalName, keytabPath, false);

                LoginContext loginContext = new LoginContext(CRED_CONF_NAME, subject, null, conf);
                loginContext.login();
                return loginContext.getSubject();
            });
        return sub;
    }


    static <T> T doAsPrivilegedWrapper(final Subject subject, final PrivilegedExceptionAction<T> action, final AccessControlContext acc)
            throws PrivilegedActionException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<T>) () -> Subject.doAsPrivileged(subject, action, acc));
        } catch (PrivilegedActionException pae) {
            if (pae.getCause() instanceof PrivilegedActionException) {
                throw (PrivilegedActionException) pae.getCause();
            }
            throw pae;
        }
    }

    public static CredentialsProvider getBasicCredentials(String user, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, password));
        return credentialsProvider;
    }

}
