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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KerberosOperationHandler is an abstract class providing basic implementations of common Kerberos
 * operations (like generating secure passwords) and placeholders for KDC-specific operations
 * (such as creating principals).
 */
public abstract class KerberosOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(KerberosOperationHandler.class);

  /**
   * Kerberos-env configuration property name: ldap_url
   */
  public final static String KERBEROS_ENV_LDAP_URL = "ldap_url";

  /**
   * Kerberos-env configuration property name: container_dn
   */
  public final static String KERBEROS_ENV_PRINCIPAL_CONTAINER_DN = "container_dn";

  /**
   * Kerberos-env configuration property name: group
   */
  public final static String KERBEROS_ENV_USER_PRINCIPAL_GROUP = "ipa_user_group";

  /**
   * Kerberos-env configuration property name: ad_create_attributes_template
   */
  public final static String KERBEROS_ENV_AD_CREATE_ATTRIBUTES_TEMPLATE = "ad_create_attributes_template";

  /**
   * Kerberos-env configuration property name: kdc_create_attributes
   */
  public final static String KERBEROS_ENV_KDC_CREATE_ATTRIBUTES = "kdc_create_attributes";

  /**
   * Kerberos-env configuration property name: encryption_types
   */
  public final static String KERBEROS_ENV_ENCRYPTION_TYPES = "encryption_types";

  /**
   * Kerberos-env configuration property name: kdc_hosts
   */
  public final static String KERBEROS_ENV_KDC_HOSTS = "kdc_hosts";

  /**
   * Kerberos-env configuration property name: admin_server_host
   */
  public final static String KERBEROS_ENV_ADMIN_SERVER_HOST = "admin_server_host";

  /**
   * Kerberos-env configuration property name: kadmin_principal_name
   */
  public final static String KERBEROS_ENV_KADMIN_PRINCIPAL_NAME = "kadmin_principal_name";

  /**
   * Kerberos-env configuration property name: executable_search_paths
   */
  public final static String KERBEROS_ENV_EXECUTABLE_SEARCH_PATHS = "executable_search_paths";

  /**
   * An array of String values declaring the default (ordered) list of path to search for executables
   */
  private static final String[] DEFAULT_EXECUTABLE_SEARCH_PATHS = {"/usr/bin", "/usr/kerberos/bin", "/usr/sbin", "/usr/lib/mit/bin", "/usr/lib/mit/sbin"};

  /**
   * A Map of MIT KDC Encryption types to EncryptionType values.
   * <p/>
   * See http://web.mit.edu/kerberos/krb5-devel/doc/admin/conf_files/kdc_conf.html#encryption-types
   */
  private static final Map<String, Set<EncryptionType>> ENCRYPTION_TYPE_TRANSLATION_MAP = Collections.unmodifiableMap(
      new HashMap<String, Set<EncryptionType>>() {
        {
          // aes: The AES family: aes256-cts-hmac-sha1-96 and aes128-cts-hmac-sha1-96
          put("aes", EnumSet.of(EncryptionType.AES256_CTS_HMAC_SHA1_96, EncryptionType.AES128_CTS_HMAC_SHA1_96));

          // aes256-cts-hmac-sha1-96 aes256-cts:  AES-256	CTS mode with 96-bit SHA-1 HMAC
          put("aes256-cts-hmac-sha1-96", EnumSet.of(EncryptionType.AES256_CTS_HMAC_SHA1_96));
          put("aes256-cts", EnumSet.of(EncryptionType.AES256_CTS_HMAC_SHA1_96));
          put("aes-256", EnumSet.of(EncryptionType.AES256_CTS_HMAC_SHA1_96));

          // aes128-cts-hmac-sha1-96 aes128-cts AES-128:	CTS mode with 96-bit SHA-1 HMAC
          put("aes128-cts-hmac-sha1-96", EnumSet.of(EncryptionType.AES128_CTS_HMAC_SHA1_96));
          put("aes128-cts", EnumSet.of(EncryptionType.AES128_CTS_HMAC_SHA1_96));
          put("aes-128", EnumSet.of(EncryptionType.AES128_CTS_HMAC_SHA1_96));

          // rc4	The RC4 family: arcfour-hmac
          put("rc4", EnumSet.of(EncryptionType.RC4_HMAC));

          // arcfour-hmac rc4-hmac arcfour-hmac-md5:	RC4 with HMAC/MD5
          put("arcfour-hmac", EnumSet.of(EncryptionType.RC4_HMAC));
          put("rc4-hmac", EnumSet.of(EncryptionType.RC4_HMAC));
          put("arcfour-hmac-md5", EnumSet.of(EncryptionType.UNKNOWN));

          // arcfour-hmac-exp rc4-hmac-exp arcfour-hmac-md5-exp:	Exportable RC4 with HMAC/MD5 (weak)
          put("arcfour-hmac-exp", EnumSet.of(EncryptionType.RC4_HMAC_EXP));
          put("rc4-hmac-exp", EnumSet.of(EncryptionType.RC4_HMAC_EXP));
          put("arcfour-hmac-md5-exp", EnumSet.of(EncryptionType.UNKNOWN));

          // camellia 	The Camellia family: camellia256-cts-cmac and camellia128-cts-cmac
          put("camellia", EnumSet.of(EncryptionType.UNKNOWN));

          // camellia256-cts-cmac camellia256-cts:	Camellia-256 CTS mode with CMAC
          put("camellia256-cts-cmac", EnumSet.of(EncryptionType.UNKNOWN));
          put("camellia256-cts", EnumSet.of(EncryptionType.UNKNOWN));

          // camellia128-cts-cmac camellia128-cts:	Camellia-128 CTS mode with CMAC
          put("camellia128-cts-cmac", EnumSet.of(EncryptionType.UNKNOWN));
          put("camellia128-cts", EnumSet.of(EncryptionType.UNKNOWN));

          //des:	The DES family: des-cbc-crc, des-cbc-md5, and des-cbc-md4 (weak)
          put("des", EnumSet.of(EncryptionType.DES_CBC_CRC, EncryptionType.DES_CBC_MD5, EncryptionType.DES_CBC_MD4));

          // des-cbc-md4: DES cbc mode with RSA-MD4 (weak)
          put("des-cbc-md4", EnumSet.of(EncryptionType.DES_CBC_MD4));

          // des-cbc-md5:	DES cbc mode with RSA-MD5 (weak)
          put("des-cbc-md5", EnumSet.of(EncryptionType.DES_CBC_MD5));

          // des-cbc-crc:	DES cbc mode with CRC-32 (weak)
          put("des-cbc-crc", EnumSet.of(EncryptionType.DES_CBC_CRC));

          // des-cbc-raw: DES cbc mode raw (weak)
          put("des-cbc-raw", EnumSet.of(EncryptionType.UNKNOWN));

          // des-hmac-sha1: DES with HMAC/sha1 (weak)
          put("des-hmac-sha1", EnumSet.of(EncryptionType.UNKNOWN));

          // des3:	The triple DES family: des3-cbc-sha1
          put("des3", EnumSet.of(EncryptionType.DES3_CBC_SHA1_KD)); // Using DES3_CBC_SHA1_KD since DES3_CBC_SHA1 invalid key issues with KDC

          // des3-cbc-raw:	Triple DES cbc mode raw (weak)
          put("des3-cbc-raw", EnumSet.of(EncryptionType.UNKNOWN));

          // des3-cbc-sha1 des3-hmac-sha1 des3-cbc-sha1-kd:	Triple DES cbc mode with HMAC/sha1
          put("des3-cbc-sha1", EnumSet.of(EncryptionType.DES3_CBC_SHA1_KD)); // Using DES3_CBC_SHA1_KD since DES3_CBC_SHA1 invalid key issues with KDC
          put("des3-hmac-sha1", EnumSet.of(EncryptionType.UNKNOWN));
          put("des3-cbc-sha1-kd", EnumSet.of(EncryptionType.DES3_CBC_SHA1_KD));


        }
      }
  );

  /**
   * The default set of ciphers to use for creating keytab entries
   */
  private static final Set<EncryptionType> DEFAULT_CIPHERS = Collections.unmodifiableSet(
      new HashSet<EncryptionType>() {{
        add(EncryptionType.DES_CBC_MD5);
        add(EncryptionType.DES3_CBC_SHA1_KD);
        add(EncryptionType.RC4_HMAC);
        add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
        add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
      }});

  private PrincipalKeyCredential administratorCredential = null;
  private String defaultRealm = null;
  private Set<EncryptionType> keyEncryptionTypes = new HashSet<>(DEFAULT_CIPHERS);
  private boolean open = false;

  /**
   * An array of String indicating an ordered list of filesystem paths to use to search for executables
   * needed to perform Kerberos-related operations. For example, kadmin
   */
  private String[] executableSearchPaths = null;


  /**
   * Prepares and creates resources to be used by this KerberosOperationHandler.
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used before this call.
   *
   * @param administratorCredential a PrincipalKeyCredential containing the administrative credential
   *                                for the relevant KDC
   * @param defaultRealm            a String declaring the default Kerberos realm (or domain)
   * @param kerberosConfiguration   a Map of key/value pairs containing data from the kerberos-env configuration set
   */
  public void open(PrincipalKeyCredential administratorCredential, String defaultRealm, Map<String, String> kerberosConfiguration)
      throws KerberosOperationException {

    setAdministratorCredential(administratorCredential);
    setDefaultRealm(defaultRealm);

    if (kerberosConfiguration != null) {
      setKeyEncryptionTypes(translateEncryptionTypes(kerberosConfiguration.get(KERBEROS_ENV_ENCRYPTION_TYPES), "\\s+"));
      setExecutableSearchPaths(kerberosConfiguration.get(KERBEROS_ENV_EXECUTABLE_SEARCH_PATHS));
    }
  }

  /**
   * Closes and cleans up any resources used by this KerberosOperationHandler
   * <p/>
   * It is expected that this KerberosOperationHandler will not be used after this call.
   */
  public void close() throws KerberosOperationException {
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
  public abstract boolean principalExists(String principal, boolean service)
      throws KerberosOperationException;

  /**
   * Creates a new principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to add
   * @param password  a String containing the password to use when creating the principal
   * @param service   a boolean value indicating whether the principal is to be created as a service principal or not
   * @return an Integer declaring the generated key number
   * @throws KerberosOperationException
   * @throws KerberosPrincipalAlreadyExistsException if the principal already exists
   */
  public abstract Integer createPrincipal(String principal, String password, boolean service)
      throws KerberosOperationException;

  /**
   * Updates the password for an existing principal in a previously configured KDC
   * <p/>
   * The implementation is specific to a particular type of KDC.
   *
   * @param principal a String containing the principal to update
   * @param password  a String containing the password to set
   * @param service   a boolean value indicating whether the principal is for a service or not
   * @return an Integer declaring the new key number
   * @throws KerberosOperationException
   * @throws KerberosPrincipalDoesNotExistException if the principal does not exist
   */
  public abstract Integer setPrincipalPassword(String principal, String password, boolean service)
      throws KerberosOperationException;

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
  public abstract boolean removePrincipal(String principal, boolean service)
      throws KerberosOperationException;

  /**
   * Tests to ensure the connection information and credentials allow for administrative
   * connectivity to the KDC
   *
   * @return true of successful; otherwise false
   * @throws KerberosOperationException if a failure occurs while testing the
   *                                    administrator credentials
   */
  public boolean testAdministratorCredentials() throws KerberosOperationException {
    if (!isOpen()) {
      throw new KerberosOperationException("This operation handler has not been opened");
    }

    PrincipalKeyCredential credential = getAdministratorCredential();
    if (credential == null) {
      throw new KerberosOperationException("Missing KDC administrator credential");
    } else {
      return principalExists(credential.getPrincipal(), false);
    }
  }

  /**
   * Create a keytab using the specified principal and password.
   *
   * @param principal a String containing the principal to test
   * @param password  a String containing the password to use when creating the principal
   * @param keyNumber a Integer indicating the key number for the keytab entries
   * @return the created Keytab
   * @throws KerberosOperationException
   */
  protected Keytab createKeytab(String principal, String password, Integer keyNumber)
      throws KerberosOperationException {

    if (StringUtils.isEmpty(principal)) {
      throw new KerberosOperationException("Failed to create keytab file, missing principal");
    }

    if (password == null) {
      throw new KerberosOperationException(String.format("Failed to create keytab file for %s, missing password", principal));
    }

    Set<EncryptionType> ciphers = new HashSet<>(keyEncryptionTypes);
    List<KeytabEntry> keytabEntries = new ArrayList<>();
    Keytab keytab = new Keytab();


    if (!ciphers.isEmpty()) {
      // Create a set of keys and relevant keytab entries
      Map<EncryptionType, EncryptionKey> keys = KerberosKeyFactory.getKerberosKeys(principal, password, ciphers);

      if (keys != null) {
        byte keyVersion = (keyNumber == null) ? 0 : keyNumber.byteValue();
        KerberosTime timestamp = new KerberosTime();

        for (EncryptionKey encryptionKey : keys.values()) {
          keytabEntries.add(new KeytabEntry(principal, 1, timestamp, keyVersion, encryptionKey));
        }

        keytab.setEntries(keytabEntries);
      }
    }

    return keytab;
  }

  /**
   * Create or append to a keytab file using keytab data from another keytab file.
   * <p/>
   * If the destination keytab file contains keytab data, that data will be merged with the new data
   * to create a composite set of keytab entries.
   *
   * @param sourceKeytabFile      a File containing the absolute path to the file with the keytab data to store
   * @param destinationKeytabFile a File containing the absolute path to where the keytab data is to be stored
   * @return true if the keytab file was successfully created; false otherwise
   * @throws KerberosOperationException
   * @see #createKeytabFile(org.apache.directory.server.kerberos.shared.keytab.Keytab, java.io.File)
   */
  protected boolean createKeytabFile(File sourceKeytabFile, File destinationKeytabFile)
      throws KerberosOperationException {
    return createKeytabFile(readKeytabFile(sourceKeytabFile), destinationKeytabFile);
  }

  /**
   * Create or append to a keytab file using the specified principal and password.
   * <p/>
   * If the destination keytab file contains keytab data, that data will be merged with the new data
   * to create a composite set of keytab entries.
   *
   * @param principal             a String containing the principal to test
   * @param password              a String containing the password to use when creating the principal
   * @param keyNumber             an Integer declaring the relevant key number to use for the keytabs entries
   * @param destinationKeytabFile a File containing the absolute path to where the keytab data is to be stored
   * @return true if the keytab file was successfully created; false otherwise
   * @throws KerberosOperationException
   * @see #createKeytabFile(org.apache.directory.server.kerberos.shared.keytab.Keytab, java.io.File)
   */
  protected boolean createKeytabFile(String principal, String password, Integer keyNumber, File destinationKeytabFile)
      throws KerberosOperationException {
    return createKeytabFile(createKeytab(principal, password, keyNumber), destinationKeytabFile);
  }

  /**
   * Create or append to a keytab file using the specified Keytab
   * <p/>
   * If the destination keytab file contains keytab data, that data will be merged with the new data
   * to create a composite set of keytab entries.
   *
   * @param keytab                the Keytab containing the data to add to the keytab file
   * @param destinationKeytabFile a File containing the absolute path to where the keytab data is to be stored
   * @return true if the keytab file was successfully created; false otherwise
   * @throws KerberosOperationException
   */
  public boolean createKeytabFile(Keytab keytab, File destinationKeytabFile)
      throws KerberosOperationException {

    if (destinationKeytabFile == null) {
      throw new KerberosOperationException("The destination file path is null");
    }

    try {
      mergeKeytabs(readKeytabFile(destinationKeytabFile), keytab).write(destinationKeytabFile);
      return true;
    } catch (IOException e) {
      String message = "Failed to export keytab file";
      LOG.error(message, e);

      if (!destinationKeytabFile.delete()) {
        destinationKeytabFile.deleteOnExit();
      }

      throw new KerberosOperationException(message, e);
    }
  }

  /**
   * Merge the keytab data from one keytab with the keytab data from a different keytab.
   * <p/>
   * If similar key entries exist for the same principal, the updated values will be used
   *
   * @param keytab  a Keytab with the base keytab data
   * @param updates a Keytab containing the updated keytab data
   * @return a Keytab with the merged data
   */
  protected Keytab mergeKeytabs(Keytab keytab, Keytab updates) {
    List<KeytabEntry> keytabEntries = (keytab == null)
        ? Collections.emptyList()
        : new ArrayList<>(keytab.getEntries());
    List<KeytabEntry> updateEntries = (updates == null)
        ? Collections.emptyList()
        : new ArrayList<>(updates.getEntries());
    List<KeytabEntry> mergedEntries = new ArrayList<>();

    if (keytabEntries.isEmpty()) {
      mergedEntries.addAll(updateEntries);
    } else if (updateEntries.isEmpty()) {
      mergedEntries.addAll(keytabEntries);
    } else {
      Iterator<KeytabEntry> iterator = keytabEntries.iterator();

      while (iterator.hasNext()) {
        KeytabEntry keytabEntry = iterator.next();

        for (KeytabEntry entry : updateEntries) {
          if (entry.getPrincipalName().equals(keytabEntry.getPrincipalName()) &&
              entry.getKey().getKeyType().equals(keytabEntry.getKey().getKeyType())) {
            iterator.remove();
            break;
          }
        }
      }

      mergedEntries.addAll(keytabEntries);
      mergedEntries.addAll(updateEntries);
    }

    Keytab mergedKeytab = new Keytab();
    mergedKeytab.setEntries(mergedEntries);
    return mergedKeytab;
  }

  /**
   * Reads a file containing keytab data into a new Keytab
   *
   * @param file A File containing the path to the file from which to read keytab data
   * @return a Keytab or null if the file was not readable
   */
  protected Keytab readKeytabFile(File file) {
    Keytab keytab;

    if (file.exists() && file.canRead() && (file.length() > 0)) {
      try {
        keytab = Keytab.read(file);
      } catch (IOException e) {
        // There was an issue reading in the existing keytab file... quietly assume no data
        keytab = null;
      }
    } else {
      keytab = null;
    }

    return keytab;
  }

  /**
   * Sets the KDC administrator credential.
   *
   * @return a credential
   */
  public PrincipalKeyCredential getAdministratorCredential() {
    return administratorCredential;
  }

  /**
   * Sets the administrator credentials for this KerberosOperationHandler.
   * <p/>
   * If the supplied {@link PrincipalKeyCredential} is not <code>null</code>, validates that the administrator
   * principal and values are not <code>null</code> or empty. If the credential value does not
   * validate, then a {@link KerberosAdminAuthenticationException} will be thrown.
   *
   * @param administratorCredential the KDC administrator credential
   * @throws KerberosAdminAuthenticationException if the non-null Credential fails to contain
   *                                              a non-empty principal and password values.
   */
  public void setAdministratorCredential(PrincipalKeyCredential administratorCredential)
      throws KerberosAdminAuthenticationException {

    // Ensure the credential is not null
    if (administratorCredential == null) {
      throw new KerberosAdminAuthenticationException("The administrator credential must not be null");
    }

    // Ensure the principal is not null or empty
    String principal = administratorCredential.getPrincipal();
    if (StringUtils.isEmpty(principal)) {
      throw new KerberosAdminAuthenticationException("Must specify a principal but it is null or empty");
    }

    // Ensure either the password or the keytab value is not null or empty
    char[] password = administratorCredential.getKey();
    if (ArrayUtils.isEmpty(password)) {
      throw new KerberosAdminAuthenticationException("Must specify a password but it is null or empty");
    }

    this.administratorCredential = administratorCredential;
  }

  public String getDefaultRealm() {
    return defaultRealm;
  }

  public void setDefaultRealm(String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }

  /**
   * Gets the encryption algorithms used to encrypt keys in keytab entries
   *
   * @return a Set of EncryptionKey values indicating which algorithms are to be used when
   * encrypting keys for keytab entries.
   */
  public Set<EncryptionType> getKeyEncryptionTypes() {
    return keyEncryptionTypes;
  }

  /**
   * Sets the encryption algorithms to use to encrypt keys in keytab entries
   * <p/>
   * If set to <code>null</code> the default set of ciphers will be used.  See {@link #DEFAULT_CIPHERS}
   *
   * @param keyEncryptionTypes a Set of EncryptionKey values or null to indicate the default set
   */
  public void setKeyEncryptionTypes(Set<EncryptionType> keyEncryptionTypes) {
    this.keyEncryptionTypes = Collections.unmodifiableSet(new HashSet<>(
        (keyEncryptionTypes == null)
            ? DEFAULT_CIPHERS
            : keyEncryptionTypes
    ));
  }


  /**
   * Gets the ordered array of search paths used to find Kerberos-related executables.
   *
   * @return an array of String values indicating an order list of filesystem paths to search
   */
  public String[] getExecutableSearchPaths() {
    return executableSearchPaths;
  }

  /**
   * Sets the ordered array of search paths used to find Kerberos-related executables.
   * <p/>
   * If null, a default set of paths will be assumed when searching:
   * <ul>
   * <li>/usr/bin</li>
   * <li>/usr/kerberos/bin</li>
   * <li>/usr/sbin</li>
   * <li>/usr/lib/mit/bin</li>
   * <li>/usr/lib/mit/sbin</li>
   * </ul>
   *
   * @param executableSearchPaths an array of String values indicating an ordered list of filesystem paths to search
   */
  public void setExecutableSearchPaths(String[] executableSearchPaths) {
    this.executableSearchPaths = executableSearchPaths;
  }

  /**
   * Sets the ordered array of search paths used to find Kerberos-related executables.
   * <p/>
   * If null, a default set of paths will be assumed when searching:
   * <ul>
   * <li>/usr/bin</li>
   * <li>/usr/kerberos/bin</li>
   * <li>/usr/sbin</li>
   * </ul>
   *
   * @param delimitedExecutableSearchPaths a String containing a comma-delimited (ordered) list of filesystem paths to search
   */
  public void setExecutableSearchPaths(String delimitedExecutableSearchPaths) {
    List<String> searchPaths = null;

    if (delimitedExecutableSearchPaths != null) {
      searchPaths = new ArrayList<>();
      for (String path : delimitedExecutableSearchPaths.split(",")) {
        path = path.trim();
        if (!path.isEmpty()) {
          searchPaths.add(path);
        }
      }
    }

    setExecutableSearchPaths((searchPaths == null) ? null : searchPaths.toArray(new String[searchPaths.size()]));
  }

  /**
   * Test this KerberosOperationHandler to see whether is was previously open or not
   *
   * @return a boolean value indicating whether this KerberosOperationHandler was open (true) or not (false)
   */
  public boolean isOpen() {
    return open;
  }

  /**
   * Sets whether this KerberosOperationHandler is open or not.
   *
   * @param open a boolean value indicating whether this KerberosOperationHandler was open (true) or not (false)
   */
  public void setOpen(boolean open) {
    this.open = open;
  }

  /**
   * Given base64-encoded keytab data, decode the String to binary data and write it to a (temporary)
   * file.
   * <p/>
   * Upon success, a new file is created.  The caller is expected to clean up this file when done
   * with it.
   *
   * @param keytabData a String containing base64-encoded keytab data
   * @return a File pointing to the decoded keytab file or null if not successful
   * @throws KerberosOperationException
   */
  protected File createKeytabFile(String keytabData)
      throws KerberosOperationException {
    boolean success = false;
    File tempFile = null;

    // Create a temporary file
    try {
      tempFile = File.createTempFile("temp", ".dat");
    } catch (IOException e) {
      LOG.error(String.format("Failed to create temporary keytab file: %s", e.getLocalizedMessage()), e);
    }

    if ((tempFile != null) && (keytabData != null)) {
      OutputStream fos = null;

      // Decoded the base64-encoded String and write it to the temporary file
      try {
        fos = new FileOutputStream(tempFile);
        fos.write(Base64.decodeBase64(keytabData));
        success = true;
      } catch (IOException e) {
        String message = String.format("Failed to write to temporary keytab file %s: %s",
            tempFile.getAbsolutePath(), e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } finally {
        if (fos != null) {
          try {
            fos.close();
          } catch (IOException e) {
            // Ignore this...
          }
        }

        // If there was an issue, clean up the file
        if (!success) {
          if (!tempFile.delete()) {
            tempFile.deleteOnExit();
          }

          tempFile = null;
        }
      }
    }

    return tempFile;
  }

  /**
   * Executes a shell command.
   * <p/>
   * See {@link org.apache.ambari.server.utils.ShellCommandUtil#runCommand(String[], Map<String,String>)}
   *
   * @param command            an array of String value representing the command and its arguments
   * @param envp               a map of string, string of environment variables
   * @param interactiveHandler a handler to provide responses to queries from the command,
   *                           or null if no queries are expected
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   */
  protected ShellCommandUtil.Result executeCommand(String[] command, Map<String, String> envp, ShellCommandUtil.InteractiveHandler interactiveHandler)
      throws KerberosOperationException {

    if ((command == null) || (command.length == 0)) {
      return null;
    } else {
      try {
        return ShellCommandUtil.runCommand(command, envp, interactiveHandler, false);
      } catch (IOException e) {
        String message = String.format("Failed to execute the command: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      } catch (InterruptedException e) {
        String message = String.format("Failed to wait for the command to complete: %s", e.getLocalizedMessage());
        LOG.error(message, e);
        throw new KerberosOperationException(message, e);
      }
    }
  }

  /**
   * Executes a shell command.
   * <p/>
   * See {@link org.apache.ambari.server.utils.ShellCommandUtil#runCommand(String[])}
   *
   * @param command an array of String value representing the command and its arguments
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   * @see #executeCommand(String[], Map, ShellCommandUtil.InteractiveHandler)
   */
  protected ShellCommandUtil.Result executeCommand(String[] command)
      throws KerberosOperationException {
    return executeCommand(command, null);
  }

  /**
   * Executes a shell command.
   * <p/>
   * See {@link org.apache.ambari.server.utils.ShellCommandUtil#runCommand(String[])}
   *
   * @param command            an array of String value representing the command and its arguments
   * @param interactiveHandler a handler to provide responses to queries from the command,
   *                           or null if no queries are expected
   * @return a ShellCommandUtil.Result declaring the result of the operation
   * @throws KerberosOperationException
   * @see #executeCommand(String[], Map, ShellCommandUtil.InteractiveHandler)
   */
  protected ShellCommandUtil.Result executeCommand(String[] command, ShellCommandUtil.InteractiveHandler interactiveHandler) throws KerberosOperationException {
    return executeCommand(command, null, interactiveHandler);
  }

  /**
   * Given a principal, attempt to create a new DeconstructedPrincipal
   *
   * @param principal a String containing the principal to deconstruct
   * @return a DeconstructedPrincipal
   * @throws KerberosOperationException
   */
  protected DeconstructedPrincipal createDeconstructPrincipal(String principal) throws KerberosOperationException {
    try {
      return DeconstructedPrincipal.valueOf(principal, getDefaultRealm());
    } catch (IllegalArgumentException e) {
      throw new KerberosOperationException(e.getMessage(), e);
    }
  }

  /**
   * Given a cipher (or algorithm) name, attempts to translate it into an EncryptionType value.
   * <p/>
   * If a translation is not able to be made, {@link org.apache.directory.shared.kerberos.codec.types.EncryptionType#UNKNOWN}
   * is returned.
   *
   * @param name a String containing the name of the cipher to translate
   * @return an EncryptionType
   */
  protected Set<EncryptionType> translateEncryptionType(String name) {
    Set<EncryptionType> encryptionTypes = null;

    if (!StringUtils.isEmpty(name)) {
      encryptionTypes = ENCRYPTION_TYPE_TRANSLATION_MAP.get(name.toLowerCase());
    }

    if (encryptionTypes == null) {
      LOG.warn("The given encryption type name ({}) is not supported.", name);
      return Collections.emptySet();
    }

    return encryptionTypes;
  }

  /**
   * Given a delimited set of encryption type names, attempts to translate into a set of EncryptionType
   * values.
   *
   * @param names     a String containing a delimited list of encryption type names
   * @param delimiter a String declaring the delimiter to use to split names, if null, " " is used.
   * @return a Set of EncryptionType values
   * @throws KerberosOperationException When all the encryption type names are not supported
   */
  protected Set<EncryptionType> translateEncryptionTypes(String names, String delimiter)
      throws KerberosOperationException {
    Set<EncryptionType> encryptionTypes = new HashSet<>();

    if (!StringUtils.isEmpty(names)) {
      for (String name : names.split((delimiter == null) ? "\\s+" : delimiter)) {
        encryptionTypes.addAll(translateEncryptionType(name.trim()));
      }
    }

    if (encryptionTypes.isEmpty()) {
      throw new KerberosOperationException("All the encryption type names you set are not supported. Aborting.");
    }
    return encryptionTypes;
  }

  /**
   * Iterates through the characters in a string to escape special characters
   *
   * @param string             the String to process
   * @param charactersToEscape a Set of characters declaring the special characters to escape
   * @param escapeCharacter    a character to use for escaping
   * @return the string with escaped characters
   */
  protected String escapeCharacters(String string, Set<Character> charactersToEscape, Character escapeCharacter) {
    if (StringUtils.isEmpty(string) || (charactersToEscape == null) || charactersToEscape.isEmpty()) {
      return string;
    } else {
      StringBuilder builder = new StringBuilder();

      for (char character : string.toCharArray()) {
        if (charactersToEscape.contains(character)) {
          builder.append(escapeCharacter);
        }
        builder.append(character);
      }

      return builder.toString();
    }
  }

  /**
   * Given the name of an executable, searches the configured executable search path for an executable
   * file with that name.
   *
   * @param executable a String declaring the name of the executable to find within the search path
   * @return the absolute path of the found execute or the name of the executable if not found
   * within the search path
   * @see #setExecutableSearchPaths(String)
   * @see #setExecutableSearchPaths(String[])
   */
  protected String getExecutable(String executable) {
    String[] searchPaths = getExecutableSearchPaths();
    String executablePath = null;

    if (searchPaths == null) {
      searchPaths = DEFAULT_EXECUTABLE_SEARCH_PATHS;
    }

    for (String searchPath : searchPaths) {
      File executableFile = new File(searchPath, executable);

      if (executableFile.canExecute()) {
        executablePath = executableFile.getAbsolutePath();
        break;
      }
    }

    return (executablePath == null) ? executable : executablePath;
  }
}
