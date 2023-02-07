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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.components.tables;

import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@Category(MetastoreTest.class)
public class TestTablesMetadataTypeValidator extends BaseTest {

  @Test
  public void testValidType() {
    TablesMetadataTypeValidator.INSTANCE.validate(Collections.singleton(MetadataType.ALL));
    TablesMetadataTypeValidator.INSTANCE.validate(Collections.singleton(MetadataType.TABLE));
  }

  @Test
  public void testValidTypes() {
    TablesMetadataTypeValidator.INSTANCE.validate(Sets.newHashSet(
      MetadataType.TABLE,
      MetadataType.SEGMENT,
      MetadataType.FILE,
      MetadataType.ROW_GROUP,
      MetadataType.PARTITION));
  }

  @Test
  public void testInvalidType() {
    try {
      TablesMetadataTypeValidator.INSTANCE.validate(Collections.singleton(MetadataType.NONE));
      fail();
    } catch (MetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unsupported metadata types are detected"));
    }
  }

  @Test
  public void testValidAndInvalidTypes() {
    try {
      TablesMetadataTypeValidator.INSTANCE.validate(Sets.newHashSet(
        MetadataType.TABLE,
        MetadataType.ALL,
        MetadataType.NONE,
        MetadataType.VIEW));
      fail();
    } catch (MetastoreException e) {
      assertThat(e.getMessage(), startsWith("Unsupported metadata types are detected"));
    }
  }
}
