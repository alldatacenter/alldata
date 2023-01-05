/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.compiler.streamcode;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.citrus.turbine.impl.DefaultContext;
import com.google.common.collect.Maps;
import com.qlangtech.tis.coredefine.module.action.IndexIncrStatus;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import junit.framework.TestCase;

import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestGenerateDAOAndIncrScript extends TestCase {

    private static final String collection = "search4totalpay";

    private static final String dataflowName = "totalpay";

    private static final long dataflowTimestamp = 20190820171040l;

    static {
        HttpUtils.addMockGlobalParametersConfig();
    }

    public void testCompileAndPackageStreamCode() throws Exception {
        IControlMsgHandler msgHandler = new DefaultMessageHandler();
        IndexStreamCodeGenerator indexStreamCodeGenerator = getIndexStreamCodeGenerator();
        GenerateDAOAndIncrScript generateDAOAndIncrScript = new GenerateDAOAndIncrScript(msgHandler, indexStreamCodeGenerator);
        DefaultContext context = new DefaultContext();
        IndexIncrStatus incrStatus = new IndexIncrStatus();
        Map<Integer, Long> /**
         * timestamp ver
         */
                dependencyDbs = Maps.newHashMap();
        // order
        dependencyDbs.put(58, 20200227124059l);
        // member
        dependencyDbs.put(61, 20200227124200l);
        // cardcenter
        dependencyDbs.put(62, 20200227124220l);
        // shop
        dependencyDbs.put(63, 20200227124231l);
        generateDAOAndIncrScript.generate(context, incrStatus, true, dependencyDbs);
    }

    private IndexStreamCodeGenerator getIndexStreamCodeGenerator() throws Exception {


        IAppSource appSource = IAppSource.load(null, collection);
        assertNotNull(appSource);

        return new IndexStreamCodeGenerator(collection, (IBasicAppSource) appSource, dataflowTimestamp, (dbid, tables) -> {
            assertTrue(tables.size() > 0);
            return tables;
        });
    }

    private static class DefaultMessageHandler implements IControlMsgHandler {

        @Override
        public boolean validateBizLogic(BizLogic logicType, Context context, String fieldName, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getString(String key) {
            return null;
        }

        @Override
        public String getString(String key, String dftVal) {
            return null;
        }

        @Override
        public boolean getBoolean(String key) {
            return false;
        }

        @Override
        public void addFieldError(Context context, String fieldName, String msg, Object... params) {
        }

        @Override
        public void errorsPageShow(Context context) {
        }

        @Override
        public void addActionMessage(Context context, String msg) {
        }

        @Override
        public void setBizResult(Context context, Object result) {
        }

        @Override
        public void addErrorMessage(Context context, String msg) {
        }
    }
}
