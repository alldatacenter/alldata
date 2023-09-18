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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.plugin.datax.common.BasicDataXRdbmsReader;
import com.qlangtech.tis.plugin.datax.common.RdbmsReaderContext;
import com.qlangtech.tis.plugin.ds.IDataSourceDumper;
import com.qlangtech.tis.plugin.ds.tidb.TiKVDataSourceFactory;

/**
 * @see com.qlangtech.tis.plugin.datax.TisDataXTiDBReader
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-05 09:00
 **/
@Public
public class DataXTiDBReader extends BasicDataXRdbmsReader<TiKVDataSourceFactory> {

    public static final String DATAX_NAME = "TiDB";

    @Override
    protected RdbmsReaderContext createDataXReaderContext(
            String jobName, SelectedTab tab, IDataSourceDumper dumper) {
        TiDBReaderContext readerContext = new TiDBReaderContext(jobName, tab.getName(), dumper, this);
        return readerContext;
    }


    public static String getDftTemplate() {
        return IOUtils.loadResourceFromClasspath(DataXTiDBReader.class, "tidb-reader-tpl.json");
    }

    @TISExtension()
    public static class DefaultDescriptor extends BasicDataXRdbmsReaderDescriptor
    {
        @Override
        public boolean isSupportIncr() {
            return false;
        }

        @Override
        public EndType getEndType() {
            return EndType.TiDB;
        }

        @Override
        public String getDisplayName() {
            return DATAX_NAME;
        }
    }
}
