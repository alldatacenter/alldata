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

package com.qlangtech.tis.plugins.incr.flink.connector.hudi.streamscript;

import com.qlangtech.tis.plugin.datax.hudi.partition.FieldValBasedPartition;
import com.qlangtech.tis.plugin.datax.hudi.partition.OffPartition;
import com.qlangtech.tis.plugins.incr.flink.connector.hudi.scripttype.SqlType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-25 11:13
 **/
public class TestSQLStyleFlinkStreamScriptCreator extends BaiscFlinkStreamScriptCreator {

    private SqlType sqlType;

    @Before
    public void before() {
        this.sqlType = new SqlType();
    }

    @Test
    public void testFlinkSqlTableDDLCreate() throws Exception {

        validateGenerateScript(sqlType, new OffPartition(), (mdata) -> {
            SQLStyleFlinkStreamScriptCreator.HudiStreamTemplateData tplData
                    = (SQLStyleFlinkStreamScriptCreator.HudiStreamTemplateData) mdata;
            StringBuffer createTabDdl = tplData.getSinkFlinkTableDDL(targetTableName);

            Assert.assertNotNull(createTabDdl);
            System.out.println(createTabDdl);
        });
    }


    @Test
    public void testFlinkSqlTableDDLCreateWithFieldValBasedPartition() throws Exception {

        FieldValBasedPartition fieldValBasedPartition = new FieldValBasedPartition();
       // fieldValBasedPartition.partitionPathField = "kind";

        validateGenerateScript(sqlType, fieldValBasedPartition, (mdata) -> {
            SQLStyleFlinkStreamScriptCreator.HudiStreamTemplateData tplData
                    = (SQLStyleFlinkStreamScriptCreator.HudiStreamTemplateData) mdata;
            StringBuffer createTabDdl = tplData.getSinkFlinkTableDDL(targetTableName);

            Assert.assertNotNull(createTabDdl);
            System.out.println(createTabDdl);
        });
    }


}
