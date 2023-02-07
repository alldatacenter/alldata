/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.pojo.sort;

import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.sort.BaseSortConf.SortType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BaseSortConf}
 */
public class BaseSortConfTest {

    @Test
    public void testSerde() {
        FlinkSortConf flinkSortConf = new FlinkSortConf();
        BaseSortConf baseSortConf = JsonUtils.parseObject(JsonUtils.toJsonString(flinkSortConf), BaseSortConf.class);
        Assertions.assertEquals(baseSortConf.getType(), SortType.FLINK);

        UserDefinedSortConf userDefinedSortConf = new UserDefinedSortConf();
        BaseSortConf baseSortConf1 = JsonUtils.parseObject(JsonUtils.toJsonString(userDefinedSortConf),
                BaseSortConf.class);
        Assertions.assertEquals(baseSortConf1.getType(), SortType.USER_DEFINED);
    }

}
