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

package org.apache.inlong.sort.hudi.table;

import static org.apache.inlong.sort.base.Constants.INLONG_AUDIT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC;

import java.util.Set;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.table.HoodieTableFactory;

/**
 * The DynamicTableSourceFactory and DynamicTableSinkFactory implements for hudi table.
 */
public class HudiTableInlongFactory extends HoodieTableFactory {

    public static final String SORT_CONNECTOR_IDENTIFY_HUDI = "hudi-inlong";

    public HudiTableInlongFactory() {
        super();
    }

    public String factoryIdentifier() {
        return SORT_CONNECTOR_IDENTIFY_HUDI;
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        Configuration conf = FlinkOptions.fromMap(context.getCatalogTable().getOptions());
        adaptPathIfSyncMeta(context);
        return super.createDynamicTableSink(context);
    }

    private void adaptPathIfSyncMeta(Context context) {
        // TODO query hive path from hive metastore
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> configOptions = super.optionalOptions();
        configOptions.add(INLONG_METRIC);
        configOptions.add(INLONG_AUDIT);
        return configOptions;
    }
}
