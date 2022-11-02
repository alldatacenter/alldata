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

package org.apache.ambari.server.api.services.serializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.easymock.EasyMockSupport;
import org.junit.Test;

/**
 * CsvSerializer unit tests
 */
public class CsvSerializerTest extends EasyMockSupport {

  @Test
  public void testSerializeResources_NoColumnInfo() throws Exception {
    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();

    List<TreeMap<String, Object>> data = new ArrayList<TreeMap<String, Object>>() {
      {
        add(new TreeMap<String, Object>() {
          {
            put("property1", "value1a");
            put("property2", "value2a");
            put("property3", "value3a");
            put("property4", "value4a");
          }
        });
        add(new TreeMap<String, Object>() {
          {
            put("property1", "value1'b");
            put("property2", "value2'b");
            put("property3", "value3'b");
            put("property4", "value4'b");
          }
        });
        add(new TreeMap<String, Object>() {
          {
            put("property1", "value1,c");
            put("property2", "value2,c");
            put("property3", "value3,c");
            put("property4", "value4,c");
          }
        });
      }
    };

    tree.setName("items");
    tree.setProperty("isCollection", "true");

    addChildResource(tree, "resource", 0, data.get(0));
    addChildResource(tree, "resource", 1, data.get(1));
    addChildResource(tree, "resource", 2, data.get(2));

    replayAll();

    //execute test
    Object o = new CsvSerializer().serialize(result).toString().replace("\r", "");

    verifyAll();

    assertNotNull(o);

    StringReader reader = new StringReader(o.toString());
    CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
    List<CSVRecord> records = csvParser.getRecords();

    assertNotNull(records);
    assertEquals(3, records.size());

    int i = 0;
    for (CSVRecord record : records) {
      TreeMap<String, Object> actualData = data.get(i++);
      assertEquals(actualData.size(), record.size());

      for (String item : record) {
        assertTrue(actualData.containsValue(item));
      }
    }

    csvParser.close();
  }

  @Test
  public void testSerializeResources_HeaderInfo() throws Exception {
    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    tree.setName("items");
    tree.setProperty("isCollection", "true");
    tree.setProperty(CsvSerializer.PROPERTY_COLUMN_MAP, new TreeMap<String, String>() {{
      put("propertyD", "Property D");
      put("propertyC", "Property C");
      put("propertyB", "Property B");
      put("propertyA", "Property A");
    }});


    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>() {
      {
        add(new HashMap<String, Object>() {
          {
            put("propertyD", "value1a");
            put("propertyC", "value2a");
            put("propertyB", "value3a");
            put("propertyA", "value4a");
          }
        });
        add(new HashMap<String, Object>() {
          {
            put("propertyD", "value1'b");
            put("propertyC", "value2'b");
            put("propertyB", "value3'b");
            put("propertyA", "value4'b");
          }
        });
        add(new HashMap<String, Object>() {
          {
            put("propertyD", "value1,c");
            put("propertyC", "value2,c");
            put("propertyB", "value3,c");
            put("propertyA", "value4,c");
          }
        });
      }
    };

    addChildResource(tree, "resource", 0, data.get(0));
    addChildResource(tree, "resource", 1, data.get(1));
    addChildResource(tree, "resource", 2, data.get(2));

    replayAll();

    //execute test
    Object o = new CsvSerializer().serialize(result).toString().replace("\r", "");

    verifyAll();


    String expected = "Property A,Property B,Property C,Property D\n" +
        "value4a,value3a,value2a,value1a\n" +
        "value4'b,value3'b,value2'b,value1'b\n" +
        "\"value4,c\",\"value3,c\",\"value2,c\",\"value1,c\"\n";

    assertEquals(expected, o);

  }

  @Test
  public void testSerializeResources_HeaderOrderInfo() throws Exception {
    Result result = new ResultImpl(true);
    result.setResultStatus(new ResultStatus(ResultStatus.STATUS.OK));
    TreeNode<Resource> tree = result.getResultTree();
    tree.setName("items");
    tree.setProperty("isCollection", "true");
    tree.setProperty(CsvSerializer.PROPERTY_COLUMN_MAP, new HashMap<String, String>() {{
      put("property1", "Property 1");
      put("property2", "Property 2");
      put("property3", "Property 3");
      put("property4", "Property 4");
    }});
    tree.setProperty(CsvSerializer.PROPERTY_COLUMN_ORDER, Arrays.asList(
        "property1",
        "property2",
        "property3",
        "property4"));

    addChildResource(tree, "resource", 0, new HashMap<String, Object>() {
      {
        put("property1", "value1a");
        put("property2", "value2a");
        put("property3", "value3a");
        put("property4", "value4a");
      }
    });
    addChildResource(tree, "resource", 1, new HashMap<String, Object>() {
      {
        put("property1", "value1'b");
        put("property2", "value2'b");
        put("property3", "value3'b");
        put("property4", "value4'b");
      }
    });
    addChildResource(tree, "resource", 2, new HashMap<String, Object>() {
      {
        put("property1", "value1,c");
        put("property2", "value2,c");
        put("property3", "value3,c");
        put("property4", "value4,c");
      }
    });

    replayAll();

    //execute test
    Object o = new CsvSerializer().serialize(result).toString().replace("\r", "");

    String expected = "Property 1,Property 2,Property 3,Property 4\n" +
        "value1a,value2a,value3a,value4a\n" +
        "value1'b,value2'b,value3'b,value4'b\n" +
        "\"value1,c\",\"value2,c\",\"value3,c\",\"value4,c\"\n";

    assertEquals(expected, o);

    verifyAll();
  }


  private void addChildResource(TreeNode<Resource> parent, String name, int index, final Map<String, Object> data) {
    Resource resource = new ResourceImpl(Resource.Type.Cluster);

    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        resource.setProperty(entry.getKey(), entry.getValue());
      }
    }

    parent.addChild(resource, String.format("%s:%d", name, index));
  }

}
