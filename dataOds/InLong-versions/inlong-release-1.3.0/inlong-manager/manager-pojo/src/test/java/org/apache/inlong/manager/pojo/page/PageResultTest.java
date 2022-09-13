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

package org.apache.inlong.manager.pojo.page;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.none.InlongNoneMqInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for the {@link org.apache.inlong.manager.pojo.common.PageResult}.
 */

class PageResultTest {

    @Test
    void testDeserializePageInfoFromWeb() {

        String pageInfoJson = "{\"total\":30,\"list\":[{\"id\":null,\"inlongGroupId\":\"22\",\"name\":null,"
                + "\"description\":null,\"middlewareType\":null,\"mqType\":\"NONE\",\"mqResource\":null,"
                + "\"enableZookeeper\":0,\"enableCreateResource\":null,\"lightweight\":null,"
                + "\"inlongClusterTag\":null,\"dailyRecords\":null,\"dailyStorage\":null,\"peakRecords\":null,"
                + "\"maxLength\":null,\"inCharges\":null,\"followers\":null,\"status\":null,\"creator\":null,"
                + "\"modifier\":null,\"createTime\":null,\"modifyTime\":null,\"extList\":null,\"sortConf\":null,"
                + "\"version\":44},{\"id\":null,\"inlongGroupId\":null,\"name\":null,\"description\":null,"
                + "\"middlewareType\":null,\"mqType\":\"PULSAR\",\"mqResource\":null,\"enableZookeeper\":0,"
                + "\"enableCreateResource\":null,\"lightweight\":null,\"inlongClusterTag\":null,"
                + "\"dailyRecords\":null,\"dailyStorage\":null,\"peakRecords\":null,\"maxLength\":null,"
                + "\"inCharges\":null,\"followers\":null,\"status\":null,\"creator\":null,\"modifier\":null,"
                + "\"createTime\":null,\"modifyTime\":null,\"extList\":null,\"sortConf\":null,\"version\":null,"
                + "\"tenant\":null,\"adminUrl\":null,\"serviceUrl\":null,\"queueModule\":\"PARALLEL\","
                + "\"partitionNum\":3,\"ensemble\":3,\"writeQuorum\":3,\"ackQuorum\":2,\"ttl\":24,"
                + "\"ttlUnit\":\"hours\",\"retentionTime\":72,\"retentionTimeUnit\":\"hours\","
                + "\"retentionSize\":-1,\"retentionSizeUnit\":\"MB\"}],\"pageNum\":1,\"pageSize\":20,\"size\":0,"
                + "\"startRow\":0,\"endRow\":0,\"pages\":0,\"prePage\":0,\"nextPage\":0,\"isFirstPage\":false,"
                + "\"isLastPage\":false,\"hasPreviousPage\":false,\"hasNextPage\":false,\"navigatePages\":0,"
                + "\"navigatepageNums\":null,\"navigateFirstPage\":0,\"navigateLastPage\":0}";

        PageResult<InlongGroupInfo> pageResult = JsonUtils.parseObject(pageInfoJson,
                new TypeReference<PageResult<InlongGroupInfo>>() {
                });

        Assertions.assertEquals(1, pageResult.getPageNum());
        Assertions.assertEquals(30, pageResult.getTotal());
        Assertions.assertEquals(20, pageResult.getPageSize());

        List<InlongGroupInfo> inlongGroupInfos = pageResult.getList();
        Assertions.assertTrue(inlongGroupInfos.get(0) instanceof InlongNoneMqInfo);
        Assertions.assertTrue(inlongGroupInfos.get(1) instanceof InlongPulsarInfo);
    }
}
