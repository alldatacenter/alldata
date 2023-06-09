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
package com.qlangtech.tis.coredefine.module.action;

import com.google.common.collect.Lists;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.util.TestHeteroList;
import edu.emory.mathcs.backport.java.util.Collections;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-13 11:00
 */
public class DataxAction {

    public static List<Option> getDepartments() {
        return Collections.emptyList();
    }

    public static List<String> getTablesInDB(IPropertyType.SubFormFilter filter) {
        String dataxName = filter.param(DataxUtils.DATAX_NAME);
        Assert.assertEquals("dataxName must equal", TestHeteroList.DATAX_INSTANCE_NAME, dataxName);
        return Lists.newArrayList("table1", "table2", "table3");
    }
}
