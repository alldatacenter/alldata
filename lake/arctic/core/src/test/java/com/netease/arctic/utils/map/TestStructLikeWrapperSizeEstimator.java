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

package com.netease.arctic.utils.map;

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.data.ChangedLsn;
import com.netease.arctic.iceberg.optimize.StructLikeWrapper;
import com.netease.arctic.iceberg.optimize.StructLikeWrapperFactory;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.utils.ObjectSizeCalculator;
import org.apache.iceberg.data.Record;
import org.junit.Assert;
import org.junit.Test;

public class TestStructLikeWrapperSizeEstimator {

  @Test
  public void testSizeEstimator() {
    Record record1 = DataTestHelpers.createRecord(1, "name1", 0, "2022-08-30T12:00:00");
    Record record2 = DataTestHelpers.createRecord(2, "test2", 1, "2023-06-29T13:00:00");
    StructLikeCollections structLikeCollections =
        new StructLikeCollections(true, Long.MAX_VALUE);

    StructLikeBaseMap<ChangedLsn> map =
        structLikeCollections.createStructLikeMap(TableTestHelpers.TABLE_SCHEMA.asStruct());
    ChangedLsn changedLsn = ChangedLsn.of(1, 2);
    map.put(record1, changedLsn);
    long oldSize = ObjectSizeCalculator.getObjectSize(((map.getInternalMap())));
    map.put(record2, changedLsn);
    long newSize = ObjectSizeCalculator.getObjectSize(((map.getInternalMap())));
    long record2Size = newSize - oldSize;
    StructLikeWrapperFactory wrapperFactory = new StructLikeWrapperFactory(TableTestHelpers.TABLE_SCHEMA.asStruct());
    StructLikeWrapper wrapper = wrapperFactory.create().set(record2);

    // Because the size of map also will increase, so the record2Size should a little bigger than the size of the record
    Assert.assertEquals(1, record2Size / new StructLikeWrapperSizeEstimator().sizeEstimate(wrapper));
  }
}