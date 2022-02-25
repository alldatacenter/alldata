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

package org.apache.ambari.server.serveraction.kerberos;


import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLHandshakeException;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.security.InternalSSLSocketFactoryNonTrusting;
import org.apache.ambari.server.security.InternalSSLSocketFactoryTrusting;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.inject.Inject;

/**
 * Implementation of <code>KerberosOperationHandler</code> to created principal in Active Directory
 */
public class ADKerberosOperationHandler extends KerberosOperationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ADKerberosOperationHandler.class);

  private static final String LDAP_CONTEXT_FACTORY_CLASS = "com.sun.jndi.ldap.LdapCtxFactory";

  /**
   * A String containing the URL for the LDAP interface for the relevant Active Directory
   */
  private String ldapUrl = null;

  /**
   * A String containing the DN of the container for managing Active Directory accounts
   */
  private String principalContainerDn = null;

  /**
   * The LdapName of the container for managing Active Directory accounts
   */
  private LdapName principalContainerLdapName = null;

  /**
   * A String containing the Velocity template to use to generate the JSON structure declaring the
   * attributes to use to create new Active Directory accounts.
   * <p/>
   * If this value is null, a default template will be used.
   */
  private String createTemplate = null;

  /**
   * The relevant LDAP context, created upon opening this KerberosOperationHandler
   */
  private LdapContext ldapContext = null;

  /**
   * The relevant SearchControls, created upon opening this KerberosOperationHandler
   */
  private SearchControls searchControls = null;

  /**
   * The Gson instance to use to convert the template-generated JSON structure to a Map of attribute
   * names to values.
   */
  @Inject
  private Gson gson;

  @Inject
  private Configuration configuration;

  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   * <p/>
   * It is expected that the kerberosConfiguration Map has the following properties:
   * <ul>
   * <li>ldap_url - ldapUrl of ldap back end where principals would be created</li>
   * <li>container_dn - DN of the container in ldap back end where principals would be created</li>
   * </il>
   *
   * @param administratorCredential a PrincipalKeyCredential containing the administrative credential
   *                                for the relevant KDC
   * @param realm                   a String declaring the default Kerberos realm (or domain)
   * @param kerberosConfiguration   a Map of key/value pairs containing data from the kerberos-env configuration set
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  @Override
  public void open(PrincipalKeyCredential administratorCredential, String realm,
                   Map<String, String> kerberosConfiguration) throws KerberosOperationException {

    if (isOpen()) {
      close();
    }

    if (administratorCredential == null) {
      throw new KerberosAdminAuthenticationException("administrator credential not provided");
    }
    if (realm == null) {
      throw new KerberosRealmException("realm not provided");
    }
    if (kerberosConfiguration == null) {
      throw new KerberosRealmException("kerberos-env configuration may not be null");
    }

    this.ldapUrl = kerberosConfiguration.get(KERBEROS_ENV_LDAP_URL);
    if (this.ldapUrl == null) {
      throw new KerberosKDCConnectionException("ldapUrl not provided");
    }
    if (!this.ldapUrl.startsWith("ldaps://")) {
      throw new KerberosKDCConnectionException("ldapUrl is not valid ldaps URL");
    }

    this.principalContainerDn = kerberosConfiguration.get(KERBEROS_ENV_PRINCIPAL_CONTAINER_DN);
    if (this.principalContainerDn == null) {
      throw new KerberosLDAPContainerException("principalContainerDn not provided");
    }

    try {
      this.principalContainerLdapName = new LdapName(principalContainerDn);
    } catch (InvalidNameException e) {
      throw new KerberosLDAPContainerException("principalContainerDn is not a valid LDAP name", e);
    }

    super.open(administratorCredential, realm, kerberosConfiguration);

    this.ldapContext = createLdapContext();
    this.searchControls = createSearchControls();

    this.createTemplate = kerberosConfiguration.get(KERBEROS_ENV_AD_CREATE_ATTRIBUTES_TEMPLATE);

    setOpen(true);
  }

  /**
   * Closes and cleans up any resources used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used after this call.
   */
  @Override
  public void close() throws KerberosOperationException {
    this.searchControls = null;

    if (this.ldapContext != null) {
      try {
        this.ldapContext.close();
      } catch (NamingException e) {
        throw new KerberosOperationException("Unexpected error", e);
      } finally {
        this.ldapContext = null;
      }
    }

    setOpen(false);
  }

  /**
   * Test to see if the specified principal exists in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to test
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return true if the principal exists; false otherwise
   * @throws KerberosOperationException
   */
  @Override
  public boolean principalExists(String principal, boolean service) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }

    DeconstructedPrincipal deconstructPrincipal = createDeconstructPrincipal(principal);

    try {
      return (findPrincipalDN(deconstructPrincipal.getNormalizedPrincipal()) != null);
    } catch (NamingException ne) {
      throw new KerberosOperationException("can not check if principal exists: " + principal, ne);
    }
  }

  /**
   * Creates a new principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosPrincipalAlreadyExistsException if the principal already exists
   * @throws KerberosOperationException
   */
  @Override
  public Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    if (password == null) {
      throw new KerberosOperationException("principal password is null");
    }
    if (principalExists(principal, service)) {
      throw new KerberosPrincipalAlreadyExistsException(principal);
    }

    DeconstructedPrincipal deconstructedPrincipal = createDeconstructPrincipal(principal);

    String realm = deconstructedPrincipal.getRealm();
    if (realm == null) {
      realm = "";
    }

    Map<String, Object> context = new HashMap<>();
    context.put("normalized_principal", deconstructedPrincipal.getNormalizedPrincipal());
    context.put("principal_name", deconstructedPrincipal.getPrincipalName());
    context.put("principal_primary", deconstructedPrincipal.getPrimary());
    context.put("principal_instance", deconstructedPrincipal.getInstance());
    context.put("realm", realm);
    context.put("realm_lowercase", realm.toLowerCase());
    context.put("password", password);
    context.put("is_service", service);
    context.put("container_dn", this.principalContainerDn);
    context.put("principal_digest", DigestUtils.sha1Hex(deconstructedPrincipal.getNormalizedPrincipal()));
    context.put("principal_digest_256", DigestUtils.sha256Hex(deconstructedPrincipal.getNormalizedPrincipal()));
    context.put("principal_digest_512", DigestUtils.sha512Hex(deconstructedPrincipal.getNormalizedPrincipal()));

    Map<String, Object> data = processCreateTemplate(context);

    Attributes attributes = new BasicAttributes();
    String cn = null;

    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        if ("unicodePwd".equals(key)) {
          if (value instanceof String) {
            try {
              attributes.put(new BasicAttribute("unicodePwd", String.format("\"%s\"", password).getBytes("UTF-16LE")));
            } catch (UnsupportedEncodingException ue) {
              throw new KerberosOperationException("Can not encode password with UTF-16LE", ue);
            }
          }
        } else {
          Attribute attribute = new BasicAttribute(key);
          if (value instanceof Collection) {
            for (Object object : (Collection) value) {
              attribute.add(object);
            }
          } else {
            if ("cn".equals(key) && (value != null)) {
              cn = value.toString();
            } else if ("sAMAccountName".equals(key) && (value != null)) {
              // Replace the following _illegal_ characters: [ ] : ; | = + * ? < > / , (space) \
              value = value.toString().replaceAll("\\[|\\]|\\:|\\;|\\||\\=|\\+|\\*|\\?|\\<|\\>|\\/|\\\\|\\,|\\s", "_");
            }

            attribute.add(value);
          }

          attributes.put(attribute);
        }
      }
    }

    if (cn == null) {
      cn = deconstructedPrincipal.getNormalizedPrincipal();
    }

    try {
      Rdn rdn = new Rdn("cn", cn);
      LdapName name = new LdapName(principalContainerLdapName.getRdns());
      name.add(name.size(), rdn);
      ldapContext.createSubcontext(name, attributes);
    } catch (NamingException ne) {
      throw new KerberosOperationException("Can not create principal : " + principal, ne);
    }
    return 0;
  }

  /**
   * Updates the password for an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return an Integer declaring the new key number
   * @throws KerberosPrincipalDoesNotExistException if the principal does not exist
   * @throws KerberosOperationException
   */
  @Override
  public Integer setPrincipalPassword(String principal, String password, boolean service) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }
    if (password == null) {
      throw new KerberosOperationException("principal password is null");
    }
    if (!principalExists(principal, service)) {
      throw new KerberosPrincipalDoesNotExistException(principal);
    }

    DeconstructedPrincipal deconstructPrincipal = createDeconstructPrincipal(principal);

    try {
      String dn = findPrincipalDN(deconstructPrincipal.getNormalizedPrincipal());

      if (dn != null) {
        ldapContext.modifyAttributes(
            new LdapName(dn),
            new ModificationItem[]{
                new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", String.format("\"%s\"", password).getBytes("UTF-16LE")))
            }
        );
      } else {
        throw new KerberosOperationException(String.format("Can not set password for principal %s: Not Found", principal));
      }
    } catch (NamingException e) {
      throw new KerberosOperationException(String.format("Can not set password for principal %s: %s", principal, e.getMessage()), e);
    } catch (UnsupportedEncodingException e) {
      throw new KerberosOperationException("Unsupported encoding UTF-16LE", e);
    }

    return 0;
  }

  /**
   * Removes an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to remove
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return true if the principal was successfully removed; otherwise false
   * @throws KerberosOperationException
   */
  @Override
  public boolean removePrincipal(String principal, boolean service) throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    if (principal == null) {
      throw new KerberosOperationException("principal is null");
    }

    DeconstructedPrincipal deconstructPrincipal = createDeconstructPrincipal(principal);

    try {
      String dn = findPrincipalDN(deconstructPrincipal.getNormalizedPrincipal());

      if (dn != null) {
        ldapContext.destroySubcontext(new LdapName(dn));
      }
    } catch (NamingException e) {
      throw new KerberosOperationException(String.format("Can not remove principal %s: %s", principal, e.getMessage()), e);
    }

    return true;
  }

  @Override
  public boolean testAdministratorCredentials() throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }
    // If this KerberosOperationHandler was successfully opened, successful authentication has
    // already occurred.
    return true;
  }

  /**
   * Helper method to create the LDAP context needed to interact with the Active Directory.
   *
   * @return the relevant LdapContext
   * @throws KerberosKDCConnectionException       if a connection to the KDC cannot be made
   * @throws KerberosAdminAuthenticationException if the administrator credentials fail to authenticate
   * @throws KerberosRealmException               if the realm does not map to a KDC
   * @throws KerberosOperationException           if an unexpected error occurred
   */
  protected LdapContext createLdapContext() throws KerberosOperationException {
    PrincipalKeyCredential administratorCredential = getAdministratorCredential();

    Properties properties = new Properties();
    properties.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CONTEXT_FACTORY_CLASS);
    properties.put(Context.PROVIDER_URL, ldapUrl);
    properties.put(Context.SECURITY_PRINCIPAL, administratorCredential.getPrincipal());
    properties.put(Context.SECURITY_CREDENTIALS, String.valueOf(administratorCredential.getKey()));
    properties.put(Context.SECURITY_AUTHENTICATION, "simple");
    properties.put(Context.REFERRAL, "follow");

    if (ldapUrl.startsWith("ldaps")) {
      if (configuration.validateKerberosOperationSSLCertTrust()) {
        properties.put("java.naming.ldap.factory.socket", InternalSSLSocketFactoryNonTrusting.class.getName());
      } else {
        properties.put("java.naming.ldap.factory.socket", InternalSSLSocketFactoryTrusting.class.getName());
      }
    }

    try {
      return createInitialLdapContext(properties, null);
    } catch (CommunicationException e) {
      Throwable rootCause = e.getRootCause();

      String message = String.format("Failed to communicate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
      LOG.warn(message, e);

      if (rootCause instanceof SSLHandshakeException) {
        throw new KerberosKDCSSLConnectionException(message, e);
      } else {
        throw new KerberosKDCConnectionException(message, e);
      }
    } catch (AuthenticationException e) {
      String message = String.format("Failed to authenticate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
      LOG.warn(message, e);
      throw new KerberosAdminAuthenticationException(message, e);
    } catch (NamingException e) {
      String error = e.getMessage();

      if (StringUtils.isEmpty(error)) {
        String message = String.format("Failed to communicate with the Active Directory at %s: %s", ldapUrl, e.getMessage());
        LOG.warn(message, e);

        if (error.startsWith("Cannot parse url:")) {
          throw new KerberosKDCConnectionException(message, e);
        } else {
          throw new KerberosOperationException(message, e);
        }
      } else {
        throw new KerberosOperationException("Unexpected error condition", e);
      }
    }
  }

  /**
   * Helper method to create the LDAP context needed to interact with the Active Directory.
   * <p/>
   * This is mainly used to help with building mocks for test cases.
   *
   * @param properties environment used to create the initial DirContext.
   *                   Null indicates an empty environment.
   * @param controls   connection request controls for the initial context.
   *                   If null, no connection request controls are used.
   * @return the relevant LdapContext
   * @throws NamingException if a naming exception is encountered
   */
  protected LdapContext createInitialLdapContext(Properties properties, Control[] controls)
      throws NamingException {
    return new InitialLdapContext(properties, controls);
  }

  /**
   * Helper method to create the SearchControls instance
   *
   * @return the relevant SearchControls
   */
  protected SearchControls createSearchControls() {
    SearchControls searchControls = new SearchControls();
    searchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    searchControls.setReturningAttributes(new String[]{"cn"});
    return searchControls;
  }

  /**
   * Processes a Velocity template to generate a map of attributes and values to use to create
   * Active Directory accounts.
   * <p/>
   * If a template was not set, a default template will be used.
   *
   * @param context a map of properties to pass to the Velocity engine
   * @return a Map of attribute names and values to use for creating an Active Directory account
   * @throws KerberosOperationException if an error occurs processing the template.
   */
  protected Map<String, Object> processCreateTemplate(Map<String, Object> context)
      throws KerberosOperationException {

    if (gson == null) {
      throw new KerberosOperationException("The JSON parser must not be null");
    }

    Map<String, Object> data = null;
    String template;
    StringWriter stringWriter = new StringWriter();

    if (StringUtils.isEmpty(createTemplate)) {
      template = "{" +
          "\"objectClass\": [\"top\", \"person\", \"organizationalPerson\", \"user\"]," +
          "\"cn\": \"$principal_name\"," +
          "#if( $is_service )" +
          "  \"servicePrincipalName\": \"$principal_name\"," +
          "#end" +
          "\"userPrincipalName\": \"$normalized_principal\"," +
          "\"unicodePwd\": \"$password\"," +
          "\"accountExpires\": \"0\"," +
          "\"userAccountControl\": \"66048\"" +
          "}";
    } else {
      template = createTemplate;
    }

    try {
      if (Velocity.evaluate(new VelocityContext(context), stringWriter, "Active Directory principal create template", template)) {
        String json = stringWriter.toString();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();

        data = gson.fromJson(json, type);
      }
    } catch (ParseErrorException e) {
      LOG.warn("Failed to parse Active Directory create principal template", e);
      throw new KerberosOperationException("Failed to parse Active Directory create principal template", e);
    } catch (MethodInvocationException | ResourceNotFoundException e) {
      LOG.warn("Failed to process Active Directory create principal template", e);
      throw new KerberosOperationException("Failed to process Active Directory create principal template", e);
    }

    return data;
  }

  private String findPrincipalDN(String normalizedPrincipal) throws NamingException, KerberosOperationException {
    String dn = null;

    if (normalizedPrincipal != null) {
      NamingEnumeration<SearchResult> results = null;

      try {
        results = ldapContext.search(
            principalContainerLdapName,
            String.format("(userPrincipalName=%s)", normalizedPrincipal),
            searchControls
        );

        if ((results != null) && results.hasMore()) {
          SearchResult result = results.next();
          dn = result.getNameInNamespace();
        }
      } finally {
        try {
          if (results != null) {
            results.close();
          }
        } catch (NamingException ne) {
          // ignore, we can not do anything about it
        }
      }
    }

    return dn;
  }
}
