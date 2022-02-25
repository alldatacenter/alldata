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
 * This is a test to see how well the KerberosConfigDataFileWriter and KerberosConfigDataFileReader
 * work when the data temporaryDirectory is opened, close, reopened, and appended to.
 */
public class KerberosConfigDataFileTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private KerberosConfigDataFileReaderFactory kerberosConfigDataFileReaderFactory = new KerberosConfigDataFileReaderFactory();
  private KerberosConfigDataFileWriterFactory kerberosConfigDataFileWriterFactory = new KerberosConfigDataFileWriterFactory();

  @Test
  public void testKerberosConfigDataFile() throws Exception {
    File file = folder.newFile();
    Assert.assertNotNull(file);

    // Write the data
    KerberosConfigDataFileWriter writer = kerberosConfigDataFileWriterFactory.createKerberosConfigDataFileWriter(file);
    Assert.assertFalse(writer.isClosed());

    for (int i = 0; i < 10; i++) {
      writer.addRecord("config-type" + i, "key" + i, "value" + i, KerberosConfigDataFileWriter.OPERATION_TYPE_SET);
    }
    for (int i = 10; i < 15; i++) {
      writer.addRecord("config-type" + i, "key" + i, "value" + i, KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE);
    }

    writer.close();
    Assert.assertTrue(writer.isClosed());

    // Read the data...
    KerberosConfigDataFileReader reader = kerberosConfigDataFileReaderFactory.createKerberosConfigDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    Iterator<Map<String, String>> iterator = reader.iterator();
    Assert.assertNotNull(iterator);

    // Test iterator
    int i = 0;
    while (iterator.hasNext()) {
      Map<String, String> record = iterator.next();

      if (i < 15) {
        Assert.assertEquals("config-type" + i, record.get(KerberosConfigDataFileReader.CONFIGURATION_TYPE));
        Assert.assertEquals("key" + i, record.get(KerberosConfigDataFileReader.KEY));
        Assert.assertEquals("value" + i, record.get(KerberosConfigDataFileReader.VALUE));

        if(i<10) {
          Assert.assertEquals("SET", record.get(KerberosConfigDataFileReader.OPERATION));
        }
        else {
          Assert.assertEquals("REMOVE", record.get(KerberosConfigDataFileReader.OPERATION));
        }
      }

      i++;
    }

    Assert.assertEquals(15, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());
    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      if (i < 10) {
        Assert.assertEquals("config-type" + i, record.get(KerberosConfigDataFileReader.CONFIGURATION_TYPE));
        Assert.assertEquals("key" + i, record.get(KerberosConfigDataFileReader.KEY));
        Assert.assertEquals("value" + i, record.get(KerberosConfigDataFileReader.VALUE));
      }

      i++;
    }

    Assert.assertEquals(15, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    writer.open();
    Assert.assertFalse(writer.isClosed());

    writer.addRecord("config-type", "key", "value", KerberosConfigDataFileReader.OPERATION_TYPE_SET);

    writer.close();
    Assert.assertTrue(writer.isClosed());

    reader = kerberosConfigDataFileReaderFactory.createKerberosConfigDataFileReader(file);
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(16, i);

    reader.close();
    Assert.assertTrue(reader.isClosed());

    // Add an additional record
    writer = kerberosConfigDataFileWriterFactory.createKerberosConfigDataFileWriter(file);
    Assert.assertFalse(writer.isClosed());

    writer.addRecord("config-type", "key", "value", KerberosConfigDataFileReader.OPERATION_TYPE_REMOVE);

    writer.close();
    Assert.assertTrue(writer.isClosed());

    reader.open();
    Assert.assertFalse(reader.isClosed());

    i = 0;
    for (Map<String, String> record : reader) {
      i++;
    }

    Assert.assertEquals(17, i);

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