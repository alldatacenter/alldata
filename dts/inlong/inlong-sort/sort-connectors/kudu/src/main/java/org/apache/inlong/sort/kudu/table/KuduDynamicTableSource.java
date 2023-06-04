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

package org.apache.inlong.sort.kudu.table;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.TableFunctionProvider;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.inlong.sort.kudu.source.KuduLookupFunction;

import java.util.Objects;

/**
 * Creates a TableSource to scan a kudu table.
 */
public class KuduDynamicTableSource implements LookupTableSource {

    /**
     * The parameter collection for tde scanner.
     */
    private final Configuration configuration;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final KuduTableInfo kuduTableInfo;

    public KuduDynamicTableSource(
            KuduTableInfo kuduTableInfo,
            Configuration configuration,
            String inlongMetric,
            String auditHostAndPorts) {
        this.kuduTableInfo = kuduTableInfo;
        this.configuration = configuration;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuduDynamicTableSource that = (KuduDynamicTableSource) o;
        return kuduTableInfo.equals(that.kuduTableInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configuration, kuduTableInfo);
    }

    @Override
    public DynamicTableSource copy() {
        return new KuduDynamicTableSource(
                kuduTableInfo,
                configuration,
                inlongMetric,
                auditHostAndPorts);
    }

    @Override
    public String asSummaryString() {
        return "KuduSource";
    }

    @Override
    public LookupRuntimeProvider getLookupRuntimeProvider(LookupContext lookupContext) {
        return TableFunctionProvider.of(
                new KuduLookupFunction(
                        kuduTableInfo,
                        configuration));
    }
}
