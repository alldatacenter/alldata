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
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.commons.codec.binary.Base64;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

public abstract class KerberosOperationHandlerTest extends EasyMockSupport {

  static final String DEFAULT_ADMIN_PRINCIPAL = "admin";
  static final String DEFAULT_ADMIN_PASSWORD = "hadoop";
  static final String DEFAULT_REALM = "EXAMPLE.COM";
  static final PrincipalKeyCredential DEFAULT_ADMIN_CREDENTIALS = new PrincipalKeyCredential(DEFAULT_ADMIN_PRINCIPAL, DEFAULT_ADMIN_PASSWORD);
  static final Map<String, String> DEFAULT_KERBEROS_ENV_MAP;

  static {
    Map<String, String> map = new HashMap<>();
    map.put(KerberosOperationHandler.KERBEROS_ENV_ENCRYPTION_TYPES, "aes des3-cbc-sha1 rc4 des-cbc-md5");
    map.put(IPAKerberosOperationHandler.KERBEROS_ENV_KDC_HOSTS, "localhost");
    map.put(IPAKerberosOperationHandler.KERBEROS_ENV_ADMIN_SERVER_HOST, "localhost");
    DEFAULT_KERBEROS_ENV_MAP = Collections.unmodifiableMap(map);
  }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void testOpenSucceeded() throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());

    verifyAll();

    Assert.assertTrue(handler.isOpen());
  }

  @Test
  public void testOpenFailed() throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenFailure(handler);

    replayAll();

    try {
      handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
      Assert.fail("KerberosAdminAuthenticationException expected");
    } catch (KerberosAdminAuthenticationException e) {
      // This is expected
    }

    verifyAll();

    Assert.assertFalse(handler.isOpen());
  }

  @Test(expected = KerberosPrincipalAlreadyExistsException.class)
  public void testCreateUserPrincipalPrincipalAlreadyExists() throws Exception {
    testCreatePrincipalPrincipalAlreadyExists(false);
  }

  @Test(expected = KerberosPrincipalAlreadyExistsException.class)
  public void testCreateServicePrincipalPrincipalAlreadyExists() throws Exception {
    testCreatePrincipalPrincipalAlreadyExists(true);
  }

  private void testCreatePrincipalPrincipalAlreadyExists(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalAlreadyExists(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    handler.createPrincipal(createPrincipal(service), "password", service);
    handler.close();

    verifyAll();

  }


  @Test
  public void testUserPrincipalExistsNotFound() throws Exception {
    testPrincipalExistsNotFound(false);
  }

  @Test
  public void testServicePrincipalExistsNotFound() throws Exception {
    testPrincipalExistsNotFound(true);
  }

  private void testPrincipalExistsNotFound(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalDoesNotExist(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    Assert.assertFalse(handler.principalExists(createPrincipal(service), service));
    handler.close();

    verifyAll();
  }

  @Test
  public void testUserPrincipalExistsFound() throws Exception {
    testPrincipalExistsFound(false);
  }

  @Test
  public void testServicePrincipalExistsFound() throws Exception {
    testPrincipalExistsFound(true);
  }

  private void testPrincipalExistsFound(boolean service) throws Exception {
    KerberosOperationHandler handler = createMockedHandler();

    setupOpenSuccess(handler);
    setupPrincipalExists(handler, service);

    replayAll();

    handler.open(getAdminCredentials(), DEFAULT_REALM, getKerberosEnv());
    Assert.assertTrue(handler.principalExists(createPrincipal(service), service));
    handler.close();

    verifyAll();

  }

  @Test
  public void testCreateKeytabFileOneAtATime() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    int count;

    Assert.assertTrue(handler.createKeytabFile(principal1, "some password", 0, file));

    Keytab keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    count = entries.size();

    for (KeytabEntry entry : entries) {
      Assert.assertEquals(principal1, entry.getPrincipalName());
    }

    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    Assert.assertEquals(count * 2, entries.size());
  }

  @Test
  public void testEnsureKeytabFileContainsNoDuplicates() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";
    final String principal2 = "principal2@REALM.COM";
    Set<String> seenEntries = new HashSet<>();

    Assert.assertTrue(handler.createKeytabFile(principal1, "some password", 0, file));
    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    // Attempt to add duplicate entries
    Assert.assertTrue(handler.createKeytabFile(principal2, "some password", 0, file));

    Keytab keytab = Keytab.read(file);
    Assert.assertNotNull(keytab);

    List<KeytabEntry> entries = keytab.getEntries();
    Assert.assertNotNull(entries);
    Assert.assertFalse(entries.isEmpty());

    for (KeytabEntry entry : entries) {
      String seenEntry = String.format("%s|%s", entry.getPrincipalName(), entry.getKey().getKeyType().toString());
      Assert.assertFalse(seenEntries.contains(seenEntry));
      seenEntries.add(seenEntry);
    }
  }

  @Test
  public void testCreateKeytabFileExceptions() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal1 = "principal1@REALM.COM";

    try {
      handler.createKeytabFile(null, "some password", 0, file);
      Assert.fail("KerberosOperationException not thrown with null principal");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, null, null, file);
      Assert.fail("KerberosOperationException not thrown with null password");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }

    try {
      handler.createKeytabFile(principal1, "some password", 0, null);
      Assert.fail("KerberosOperationException not thrown with null file");
    } catch (Throwable t) {
      Assert.assertEquals(KerberosOperationException.class, t.getClass());
    }
  }

  @Test
  public void testCreateKeytabFileFromBase64EncodedData() throws Exception {
    KerberosOperationHandler handler = createHandler();
    File file = folder.newFile();
    final String principal = "principal@REALM.COM";

    Assert.assertTrue(handler.createKeytabFile(principal, "some password", 0, file));

    FileInputStream fis = new FileInputStream(file);
    byte[] data = new byte[(int) file.length()];

    Assert.assertEquals(data.length, fis.read(data));
    fis.close();

    File f = handler.createKeytabFile(Base64.encodeBase64String(data));
    if (f != null) {
      try {
        Keytab keytab = Keytab.read(f);
        Assert.assertNotNull(keytab);

        List<KeytabEntry> entries = keytab.getEntries();
        Assert.assertNotNull(entries);
        Assert.assertFalse(entries.isEmpty());

        for (KeytabEntry entry : entries) {
          Assert.assertEquals(principal, entry.getPrincipalName());
        }
      } finally {
        if (!f.delete()) {
          f.deleteOnExit();
        }
      }
    }
  }

  @Test
  public void testMergeKeytabs() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    Keytab keytab1 = handler.createKeytab("principal@EXAMPLE.COM", "password", 1);
    Keytab keytab2 = handler.createKeytab("principal@EXAMPLE.COM", "password1", 1);
    Keytab keytab3 = handler.createKeytab("principal1@EXAMPLE.COM", "password", 4);

    Keytab merged;

    merged = handler.mergeKeytabs(keytab1, keytab2);
    Assert.assertEquals(keytab1.getEntries().size(), merged.getEntries().size());

    merged = handler.mergeKeytabs(keytab1, keytab3);
    Assert.assertEquals(keytab1.getEntries().size() + keytab3.getEntries().size(), merged.getEntries().size());

    merged = handler.mergeKeytabs(keytab2, keytab3);
    Assert.assertEquals(keytab2.getEntries().size() + keytab3.getEntries().size(), merged.getEntries().size());

    merged = handler.mergeKeytabs(keytab2, merged);
    Assert.assertEquals(keytab2.getEntries().size() + keytab3.getEntries().size(), merged.getEntries().size());
  }

  @Test
  public void testTranslateEncryptionTypes() throws Exception {
    KerberosOperationHandler handler = createHandler();

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
          add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
          add(EncryptionType.DES3_CBC_SHA1_KD);
          add(EncryptionType.DES_CBC_MD5);
          add(EncryptionType.DES_CBC_MD4);
          add(EncryptionType.DES_CBC_CRC);
          add(EncryptionType.UNKNOWN);
        }},
        handler.translateEncryptionTypes("aes256-cts-hmac-sha1-96\n aes128-cts-hmac-sha1-96\tdes3-cbc-sha1 arcfour-hmac-md5 " +
            "camellia256-cts-cmac camellia128-cts-cmac des-cbc-crc des-cbc-md5 des-cbc-md4", "\\s+")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
          add(EncryptionType.AES128_CTS_HMAC_SHA1_96);
        }},
        handler.translateEncryptionTypes("aes", " ")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.AES256_CTS_HMAC_SHA1_96);
        }},
        handler.translateEncryptionTypes("aes-256", " ")
    );

    Assert.assertEquals(
        new HashSet<EncryptionType>() {{
          add(EncryptionType.DES3_CBC_SHA1_KD);
        }},
        handler.translateEncryptionTypes("des3", " ")
    );
  }

  @Test(expected = KerberosOperationException.class)
  public void testTranslateWrongEncryptionTypes() throws Exception {
    KerberosOperationHandler handler = createHandler();
    handler.translateEncryptionTypes("aes-255", " ");
  }

  @Test
  public void testEscapeCharacters() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    HashSet<Character> specialCharacters = new HashSet<Character>() {
      {
        add('/');
        add(',');
        add('\\');
        add('#');
        add('+');
        add('<');
        add('>');
        add(';');
        add('"');
        add('=');
        add(' ');
      }
    };

    Assert.assertEquals("\\/\\,\\\\\\#\\+\\<\\>\\;\\\"\\=\\ ", handler.escapeCharacters("/,\\#+<>;\"= ", specialCharacters, '\\'));
    Assert.assertNull(handler.escapeCharacters(null, specialCharacters, '\\'));
    Assert.assertEquals("", handler.escapeCharacters("", specialCharacters, '\\'));
    Assert.assertEquals("nothing_special_here", handler.escapeCharacters("nothing_special_here", specialCharacters, '\\'));
    Assert.assertEquals("\\/\\,\\\\\\#\\+\\<\\>\\;\\\"\\=\\ ", handler.escapeCharacters("/,\\#+<>;\"= ", specialCharacters, '\\'));

    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", null, '\\'));
    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", Collections.emptySet(), '\\'));
    Assert.assertEquals("nothing<>special#here!", handler.escapeCharacters("nothing<>special#here!", Collections.singleton('?'), '\\'));
    Assert.assertEquals("\\A's are special!", handler.escapeCharacters("A's are special!", Collections.singleton('A'), '\\'));
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsNullPrincipal() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential(null, "password");
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsEmptyPrincipal() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("", "password");
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsNullCredential() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("principal", (char[]) null);
    handler.setAdministratorCredential(credentials);
  }

  @Test(expected = KerberosAdminAuthenticationException.class)
  public void testAdminCredentialsEmptyCredential1() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    PrincipalKeyCredential credentials = new PrincipalKeyCredential("principal", "");
    handler.setAdministratorCredential(credentials);
  }

  @Test
  public void testSetExecutableSearchPaths() throws KerberosOperationException {
    KerberosOperationHandler handler = createHandler();

    handler.setExecutableSearchPaths((String) null);
    Assert.assertNull(handler.getExecutableSearchPaths());

    handler.setExecutableSearchPaths((String[]) null);
    Assert.assertNull(handler.getExecutableSearchPaths());

    handler.setExecutableSearchPaths("");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(0, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths(new String[0]);
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(0, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths(new String[]{""});
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(1, handler.getExecutableSearchPaths().length);

    handler.setExecutableSearchPaths("/path1, path2, path3/");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);

    handler.setExecutableSearchPaths("/path1, path2, ,path3/");
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);

    handler.setExecutableSearchPaths(new String[]{"/path1", "path2", "path3/"});
    Assert.assertNotNull(handler.getExecutableSearchPaths());
    Assert.assertEquals(3, handler.getExecutableSearchPaths().length);
    Assert.assertEquals("/path1", handler.getExecutableSearchPaths()[0]);
    Assert.assertEquals("path2", handler.getExecutableSearchPaths()[1]);
    Assert.assertEquals("path3/", handler.getExecutableSearchPaths()[2]);
  }

  protected abstract KerberosOperationHandler createMockedHandler() throws KerberosOperationException;

  protected abstract void setupOpenSuccess(KerberosOperationHandler handler) throws Exception;

  protected abstract void setupOpenFailure(KerberosOperationHandler handler) throws Exception;

  protected abstract void setupPrincipalAlreadyExists(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract void setupPrincipalDoesNotExist(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract void setupPrincipalExists(KerberosOperationHandler handler, boolean service) throws Exception;

  protected abstract Map<String, String> getKerberosEnv();

  protected PrincipalKeyCredential getAdminCredentials() {
    return DEFAULT_ADMIN_CREDENTIALS;
  }

  private KerberosOperationHandler createHandler() throws KerberosOperationException {
    KerberosOperationHandler handler = new KerberosOperationHandler() {

      @Override
      public void open(PrincipalKeyCredential administratorCredentials, String defaultRealm, Map<String, String> kerberosConfiguration) throws KerberosOperationException {
        setAdministratorCredential(administratorCredentials);
        setDefaultRealm(defaultRealm);
        setExecutableSearchPaths("/usr/bin, /usr/kerberos/bin, /usr/sbin");
      }

      @Override
      public void close() throws KerberosOperationException {

      }

      @Override
      public boolean principalExists(String principal, boolean service) throws KerberosOperationException {
        return false;
      }

      @Override
      public Integer createPrincipal(String principal, String password, boolean service) throws KerberosOperationException {
        return 0;
      }

      @Override
      public Integer setPrincipalPassword(String principal, String password, boolean service) throws KerberosOperationException {
        return 0;
      }

      @Override
      public boolean removePrincipal(String principal, boolean service) throws KerberosOperationException {
        return false;
      }
    };

    handler.open(new PrincipalKeyCredential("admin/admin", "hadoop"), "EXAMPLE.COM", null);
    return handler;
  }

  private String createPrincipal(boolean service) {
    return String.format("%s@%s", (service) ? "service/host" : "user", DEFAULT_REALM);
  }
}
