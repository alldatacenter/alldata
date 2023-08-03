package org.apache.ranger.services.hive;
/**
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

import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TestAllHiveOperationInRanger{

    /**
     * test that all enums in {@link HiveOperationType} match one map entry in
     * RangerHiveOperationType Map
     */
    @Test
    public void checkHiveOperationTypeMatch() {

        List<String> rangerHiveOperationList = new ArrayList<>();
        for (RangerHiveOperationType rangerHiveOperationType : RangerHiveOperationType.values()) {
            String rangerOpType = rangerHiveOperationType.name();
            rangerHiveOperationList.add(rangerOpType);
        }
        for (HiveOperationType operationType : HiveOperationType.values()) {
            String hiveOperationType = operationType.name();
            if (!rangerHiveOperationList.contains(hiveOperationType)) {
                fail("Unable to find corresponding HiveOperationType in RangerHiveOperation map.Please check this new operation.. "
                        + operationType);
            }
        }
        assert(true);
    }

}