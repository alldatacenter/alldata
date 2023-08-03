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

package org.apache.inlong.sort.kudu.sink;

import org.apache.inlong.sort.kudu.common.KuduTableInfo;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.RowData;
import org.apache.kudu.Schema;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduException;
import org.apache.kudu.client.KuduTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.inlong.sort.kudu.common.KuduOptions.MAX_RETRIES;
import static org.apache.inlong.sort.kudu.common.KuduOptions.SINK_FORCE_WITH_UPSERT_MODE;
import static org.apache.inlong.sort.kudu.common.KuduUtils.checkSchema;

/**
 * The Flink kudu Producer.
 */
@PublicEvolving
public class KuduSinkFunction
        extends
            AbstractKuduSinkFunction {

    private static final Logger LOG = LoggerFactory.getLogger(KuduSinkFunction.class);
    private int maxRetries;
    private transient KuduWriter kuduWriter;

    public KuduSinkFunction(
            KuduTableInfo kuduTableInfo,
            Configuration configuration,
            String inlongMetric,
            String auditHostAndPorts) {
        super(
                kuduTableInfo,
                configuration,
                inlongMetric,
                auditHostAndPorts);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        maxRetries = configuration.getInteger(MAX_RETRIES);

        boolean forceWithUpsertMode = configuration.getBoolean(SINK_FORCE_WITH_UPSERT_MODE);

        KuduClient client = buildKuduClient();
        KuduTable kuduTable;
        try {
            String tableName = kuduTableInfo.getTableName();
            kuduTable = client.openTable(tableName);
            checkState(client.tableExists(tableName), "Can not find table with name:{} in kudu.", tableName);
        } catch (KuduException e) {
            LOG.error("Error on Open kudu table", e);
            throw new RuntimeException(e);
        }

        // check table schema
        Schema schema = kuduTable.getSchema();
        try {
            checkSchema(kuduTableInfo.getFieldNames(), kuduTableInfo.getDataTypes(), schema);
        } catch (Exception e) {
            LOG.error("The provided schema is invalid!", e);
            throw new RuntimeException(e);
        }
        kuduWriter = new KuduWriter(client, kuduTable, kuduTableInfo);
    }

    @Override
    protected void addBatch(RowData in) throws Exception {
        kuduWriter.applyRow(in, maxRetries);
    }

    @Override
    protected void flush() throws IOException {
        kuduWriter.flush(maxRetries);
    }

    @Override
    public void close() throws Exception {
        super.close();
        try {
            kuduWriter.flushAndCheckErrors();
        } finally {
            try {
                kuduWriter.close();
            } catch (Exception e) {
                LOG.error("Error while closing kuduWrite.", e);
            }
        }
    }

    @Override
    protected void checkError() {
        kuduWriter.checkError();
    }
}
