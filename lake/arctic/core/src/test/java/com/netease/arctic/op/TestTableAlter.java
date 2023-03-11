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

package com.netease.arctic.op;

import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.UpdateSchema;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.apache.iceberg.types.Types.NestedField.optional;
import static org.apache.iceberg.types.Types.NestedField.required;

public class TestTableAlter extends TableTestBase {

  public TestTableAlter() {
    super(TableFormat.MIXED_ICEBERG, true, true);
  }

  @Test
  public void testUpdateProperties() throws TException {
    Map<String, String> properties = getArcticTable().properties();
    final String testProps = "test.props";
    final String testPropsValue = "values11111";

    final String testProps2 = "test.props2";
    final String testPropsVal2 = "v2";

    Assert.assertFalse(properties.containsKey(testProps));
    Assert.assertFalse(properties.containsKey(testProps2));

    UpdateProperties updateProperties = getArcticTable().updateProperties();
    updateProperties.set(testProps, testPropsValue);
    updateProperties.commit();

    properties = getArcticTable().properties();
    Assert.assertTrue(properties.containsKey(testProps));
    Assert.assertEquals(testPropsValue, properties.get(testProps));

    TableMeta meta = getAmsHandler().getTable(getArcticTable().id().buildTableIdentifier());
    properties = meta.getProperties();
    Assert.assertTrue(properties.containsKey(testProps));
    Assert.assertEquals(testPropsValue, properties.get(testProps));

    updateProperties = getArcticTable().updateProperties();
    updateProperties.remove(testProps);
    updateProperties.set(testProps2, testPropsVal2);
    updateProperties.commit();
    properties = getArcticTable().properties();
    Assert.assertTrue(properties.containsKey(testProps2));
    Assert.assertEquals(testPropsVal2, properties.get(testProps2));
    Assert.assertFalse(properties.containsKey(testProps));
  }

  @Test
  public void testSyncSchema() {
    UpdateSchema us = getArcticTable().asKeyedTable().baseTable().updateSchema();
    us.addColumn("height", Types.FloatType.get(), "height");
    us.addColumn("birthday", Types.DateType.get());
    us.addColumn("preferences", Types.StructType.of(
        required(1, "feature1", Types.IntegerType.get()),
        optional(2, "feature2", Types.StructType.of(
            required(3, "item1", Types.IntegerType.get()),
            optional(4, "optional", Types.BooleanType.get())
        ))
    ), "struct of named boolean options");
    us.addColumn("locations", Types.MapType.ofRequired(5, 6,
        Types.StructType.of(
            required(7, "address", Types.StringType.get()),
            required(8, "city", Types.StringType.get()),
            required(9, "state", Types.StringType.get()),
            required(10, "zip", Types.IntegerType.get())
        ),
        Types.StructType.of(
            required(11, "lat", Types.DoubleType.get()),
            required(12, "alt", Types.DoubleType.get()),
            required(13, "long", Types.FloatType.get())
        )), "map of address to coordinate");
    us.addColumn("points", Types.ListType.ofOptional(
        14,
        Types.StructType.of(
            required(15, "x", Types.LongType.get()),
            required(16, "y", Types.IntegerType.get()),
            required(17, "z", Types.IntegerType.get())
        )), "2-D cartesian points");

    us.commit();
    KeyedSchemaUpdate.syncSchema(getArcticTable().asKeyedTable());

    us = getArcticTable().asKeyedTable().baseTable().updateSchema();
    // primitive
    us.renameColumn("name", "name.nick");
    us.addColumn("friends", Types.StructType.of(
        required(18, "name", Types.IntegerType.get()),
        optional(19, "locations", Types.StructType.of(
            required(20, "lat", Types.IntegerType.get()),
            optional(21, "long", Types.BooleanType.get())
        ))));
    us.updateColumn("height", Types.DoubleType.get(), "height double");
    us.deleteColumn("birthday");

    // map
    us.addColumn("locations", "id", Types.LongType.get());
    us.renameColumn("locations.lat", "latitude");
    us.updateColumn("locations.long", Types.DoubleType.get());
    us.deleteColumn("locations.alt");

    // list
    us.addColumn("pets", Types.MapType.ofRequired(22, 23,
        Types.StructType.of(
            required(24, "name", Types.StringType.get())
        ),
        Types.StructType.of(
            required(25, "weight", Types.DoubleType.get())
        )), "pet list");
    us.renameColumn("points.x", "x.y");
    us.updateColumn("points.y", Types.LongType.get());
    us.deleteColumn("points.z");

    // struct
    us.addColumn("preferences", "a", Types.StructType.of(
        required(26, "a1", Types.IntegerType.get()),
        optional(27, "a2", Types.BooleanType.get())
    ));
    us.updateColumn("preferences.feature1", Types.LongType.get());
    us.renameColumn("preferences.feature2", "feature3");
    us.deleteColumn("preferences.feature2.item1");

    us.commit();
    KeyedSchemaUpdate.syncSchema(getArcticTable().asKeyedTable());

    Assert.assertEquals(
        "Should match base and change schema",
        getArcticTable().asKeyedTable().baseTable().schema().asStruct(),
        getArcticTable().asKeyedTable().changeTable().schema().asStruct());
  }
}
