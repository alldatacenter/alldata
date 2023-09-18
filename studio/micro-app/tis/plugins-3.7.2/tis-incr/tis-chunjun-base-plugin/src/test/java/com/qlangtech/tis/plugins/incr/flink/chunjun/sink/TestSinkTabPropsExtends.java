/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugins.incr.flink.chunjun.sink;

import com.qlangtech.tis.extension.impl.StubSuFormGetterContext;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.test.TISEasyMock;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-24 15:22
 **/
public class TestSinkTabPropsExtends implements TISEasyMock {

    @Before
    public void startClearMocks() {
        this.clearMocks();
        SuFormProperties.subFormGetterProcessThreadLocal.remove();
    }


    /**
     * 数据库表上有主键
     */
    @Test
    public void testGetPrimaryKeysWithPkOnTable() {
        StubSuFormGetterContext suFormGetterContext = new StubSuFormGetterContext(this);
        this.replay();
        suFormGetterContext.setSuFormGetterContext();

        List<Option> primaryKeys = SinkTabPropsExtends.getPrimaryKeys();
        Assert.assertEquals(1, primaryKeys.size());
        Assert.assertEquals(StubSuFormGetterContext.FILED_USER_ID, primaryKeys.get(0).getName());

        List<String> deftRecordKeys = SinkTabPropsExtends.getDeftRecordKeys();
        Assert.assertEquals(1, deftRecordKeys.size());

        Assert.assertEquals(StubSuFormGetterContext.FILED_USER_ID, deftRecordKeys.get(0));

        this.verifyAll();
    }

    /**
     * 数据库表上没有主键
     */
    @Test
    public void testGetPrimaryKeysWithOutPkOnTable() {
        StubSuFormGetterContext suFormGetterContext = new StubSuFormGetterContext(this, false);
        this.replay();
        suFormGetterContext.setSuFormGetterContext();

        List<Option> primaryKeys = SinkTabPropsExtends.getPrimaryKeys();
        Assert.assertEquals(2, primaryKeys.size());
        Assert.assertEquals(StubSuFormGetterContext.FILED_USER_ID, primaryKeys.get(0).getName());
        Assert.assertEquals(StubSuFormGetterContext.FIELD_USER_NAME, primaryKeys.get(1).getName());

        List<String> deftRecordKeys = SinkTabPropsExtends.getDeftRecordKeys();
        Assert.assertTrue(CollectionUtils.isEmpty(deftRecordKeys));


        this.verifyAll();
    }

}
