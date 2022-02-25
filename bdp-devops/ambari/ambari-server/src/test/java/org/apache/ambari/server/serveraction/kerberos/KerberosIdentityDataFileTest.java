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
import java.util.Iterator;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.Assert;

/**
 * This is a test to see how well the KerberosIdentityDataFileWriter and KerberosIdentityDataFileReader
 * work when the data temporaryDirectory is opened, close, reopened, and appended to.
 */
public class KerberosIdentityDataFileTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private KerberosIdentityDataFileReaderFactory kerberosIdentityDataFileReaderFactory = new KerberosIdentityDataFileReaderFactory();
  private KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory = new KerberosIdentityDataFileWriterFactory();

  @Test
  public void testKerberosIdentityDataFile() throws Exception {
    File file = folder.newFile();
    Assert.assertNotNull(file);

    // Write the data
    KerberosIdentityDataFileWriter writer = kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(file);
    Assert.assertFalse(writer.isClosed());

    for (int i = 0; i < 10; i++) {
      writer.writeRecord("hostName" + i, "serviceName" + i, "serviceComponentName" + i,
          "principal" + i, "principal_type" + i, "keytabFilePath" + i,
          "keytabFileOwnerName" + i, "keytabFileOwnerAccess" + i,
          "keytabFileGroupName" + i, "keytabFileGroupAccess" + i,
          "false");
    }

    // Add some odd characters
    writer.writeRecord("hostName's", "serviceName#", "serviceComponentName\"",
        "principal", "principal_type", "keytabFilePath",
        "'keytabFileOwnerName'", "<keytabFileOwnerAccess>",
        "\"keytabFileGroupName\"", "keytab,File,Group,Access",
        "false");

    writer.close();
    Assert.assertTrue(writer.isClosed());

    // Read the data...
    KerberosIdentityDataFileReader reader = kerberosIdentityDataFileReaderFactory.createKerberosIdentityDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    Iterator<Map<String, String>> iterator = reader.iterator();
    Assert.assertNotNull(iterator);

    // Test iterator
    int i = 0;
    while (iterator.hasNext()) {
      Map<String, String> record = iterator.next();

      if (i < 10) {
        Assert.assertEquals("hostName" + i, record.get(KerberosIdentityDataFileReader.HOSTNAME));
        Assert.assertEquals("serviceName" + i, record.get(KerberosIdentityDataFileReader.SERVICE));
        Assert.assertEquals("serviceComponentName" + i, record.get(KerberosIdentityDataFileReader.COMPONENT));
        Assert.assertEquals("principal" + i, record.get(KerberosIdentityDataFileReader.PRINCIPAL));
        Assert.assertEquals("principal_type" + i, record.get(KerberosIdentityDataFileReader.PRINCIPAL_TYPE));
        Assert.assertEquals("keytabFilePath" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH));
        Assert.assertEquals("keytabFileOwnerName" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("keytabFileOwnerAccess" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("keytabFileGroupName" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytabFileGroupAccess" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("false", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_IS_CACHABLE));
      } else {
        Assert.assertEquals("hostName's", record.get(KerberosIdentityDataFileReader.HOSTNAME));
        Assert.assertEquals("serviceName#", record.get(KerberosIdentityDataFileReader.SERVICE));
        Assert.assertEquals("serviceComponentName\"", record.get(KerberosIdentityDataFileReader.COMPONENT));
        Assert.assertEquals("principal", record.get(KerberosIdentityDataFileReader.PRINCIPAL));
        Assert.assertEquals("principal_type", record.get(KerberosIdentityDataFileReader.PRINCIPAL_TYPE));
        Assert.assertEquals("keytabFilePath", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH));
        Assert.assertEquals("'keytabFileOwnerName'", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("<keytabFileOwnerAccess>", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("\"keytabFileGroupName\"", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytab,File,Group,Access", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));
        Assert.assertEquals("false", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_IS_CACHABLE));
      }

      i++;
    }

    reader.close();
    Assert.assertTrue(reader.isClosed());
    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      if (i < 10) {
        Assert.assertEquals("hostName" + i, record.get(KerberosIdentityDataFileReader.HOSTNAME));
        Assert.assertEquals("serviceName" + i, record.get(KerberosIdentityDataFileReader.SERVICE));
        Assert.assertEquals("serviceComponentName" + i, record.get(KerberosIdentityDataFileReader.COMPONENT));
        Assert.assertEquals("principal" + i, record.get(KerberosIdentityDataFileReader.PRINCIPAL));
        Assert.assertEquals("principal_type" + i, record.get(KerberosIdentityDataFileReader.PRINCIPAL_TYPE));
        Assert.assertEquals("keytabFilePath" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH));
        Assert.assertEquals("keytabFileOwnerName" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("keytabFileOwnerAccess" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("keytabFileGroupName" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytabFileGroupAccess" + i, record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));
      } else {
        Assert.assertEquals("hostName's", record.get(KerberosIdentityDataFileReader.HOSTNAME));
        Assert.assertEquals("serviceName#", record.get(KerberosIdentityDataFileReader.SERVICE));
        Assert.assertEquals("serviceComponentName\"", record.get(KerberosIdentityDataFileReader.COMPONENT));
        Assert.assertEquals("principal", record.get(KerberosIdentityDataFileReader.PRINCIPAL));
        Assert.assertEquals("principal_type", record.get(KerberosIdentityDataFileReader.PRINCIPAL_TYPE));
        Assert.assertEquals("keytabFilePath", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_PATH));
        Assert.assertEquals("'keytabFileOwnerName'", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_NAME));
        Assert.assertEquals("<keytabFileOwnerAccess>", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_OWNER_ACCESS));
        Assert.assertEquals("\"keytabFileGroupName\"", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_NAME));
        Assert.assertEquals("keytab,File,Group,Access", record.get(KerberosIdentityDataFileReader.KEYTAB_FILE_GROUP_ACCESS));
      }

      i++;
    }

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    writer.open();
    Assert.assertFalse(writer.isClosed());

    writer.writeRecord("hostName", "serviceName", "serviceComponentName",
        "principal", "principal_type", "keytabFilePath",
        "keytabFileOwnerName", "keytabFileOwnerAccess",
        "keytabFileGroupName", "keytabFileGroupAccess",
        "true");

    writer.close();
    Assert.assertTrue(writer.isClosed());

    reader = kerberosIdentityDataFileReaderFactory.createKerberosIdentityDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(12, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    writer = kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter(file);
    Assert.assertFalse(writer.isClosed());

    writer.writeRecord("hostName", "serviceName", "serviceComponentName",
        "principal", "principal_type", "keytabFilePath",
        "keytabFileOwnerName", "keytabFileOwnerAccess",
        "keytabFileGroupName", "keytabFileGroupAccess",
        "true");

    writer.close();
    Assert.assertTrue(writer.isClosed());

    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(13, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // trying to iterate over a closed reader...
    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }
    Assert.assertEquals(0, i);

  }
}
