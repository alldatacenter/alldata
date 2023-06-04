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

package org.apache.inlong.sort.kudu.source;

import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava18.com.google.common.cache.Cache;
import org.apache.flink.shaded.guava18.com.google.common.cache.CacheBuilder;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.TableFunction;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.kudu.common.KuduOptions;
import org.apache.inlong.sort.kudu.common.KuduTableInfo;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduPredicate;
import org.apache.kudu.client.KuduScanToken;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.LocatedTablet;
import org.apache.kudu.client.RowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.TimeUtils.parseDuration;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_ADMIN_OPERATION_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_OPERATION_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DEFAULT_SOCKET_READ_TIMEOUT_IN_MS;
import static org.apache.inlong.sort.kudu.common.KuduOptions.DISABLED_STATISTICS;
import static org.apache.kudu.client.KuduPredicate.ComparisonOp.EQUAL;

/**
 * The KuduLookupFunction is a standard user-defined table function, it can be
 * used in tableAPI and also useful for temporal table join plan in SQL.
 */
public class KuduLookupFunction extends TableFunction<Row> {

    private static final Logger LOG =
            LoggerFactory.getLogger(KuduLookupFunction.class);

    /**
     * The names of lookup key.
     */
    private final String[] keyNames;

    /**
     * The configuration for the tde source.
     */
    private final Configuration configuration;

    /**
     * The masters of kudu server.
     */
    private final String masters;

    /**
     * The name of kudu table.
     */
    private final String tableName;

    /**
     * The maximum number of retries.
     */
    private transient int maxRetries;

    /**
     * The cache for lookup results.
     */
    private transient Cache<Row, List<Row>> cache;

    /**
     * The client of kudu.
     */
    private transient KuduClient client;

    /**
     * The table of kudu.
     */
    private transient KuduTable table;

    public KuduLookupFunction(
            KuduTableInfo kuduTableInfo,
            Configuration configuration) {
        checkNotNull(configuration,
                "The configuration must not be null.");

        this.masters = kuduTableInfo.getMasters();
        this.tableName = kuduTableInfo.getTableName();
        this.keyNames = kuduTableInfo.getFieldNames();
        this.configuration = configuration;
    }

    @Override
    public void open(FunctionContext context) throws Exception {
        super.open(context);
        maxRetries = configuration.getInteger(KuduOptions.MAX_RETRIES);
        int maxCacheSize = configuration.getInteger(KuduOptions.MAX_CACHE_SIZE);
        Duration maxCacheTime = parseDuration(configuration.getString(
                KuduOptions.MAX_CACHE_TIME));
        LOG.info("opening KuduLookupFunction, maxCacheSize:{}, maxCacheTime:{}.", maxCacheSize, maxCacheTime);

        if (maxCacheSize > 0) {
            cache =
                    CacheBuilder.newBuilder()
                            .maximumSize(maxCacheSize)
                            .expireAfterWrite(maxCacheTime.toMillis(), TimeUnit.MILLISECONDS)
                            .build();
        }

        this.client = buildKuduClient();

        this.table = client.openTable(tableName);
        LOG.info("KuduLookupFunction opened.");
    }

    private KuduClient buildKuduClient() {
        KuduClient.KuduClientBuilder builder = new KuduClient.KuduClientBuilder(masters);
        if (configuration.getBoolean(DISABLED_STATISTICS)) {
            builder.disableStatistics();
        }
        builder.defaultAdminOperationTimeoutMs(configuration.getLong(DEFAULT_ADMIN_OPERATION_TIMEOUT_IN_MS));
        builder.defaultOperationTimeoutMs(configuration.getLong(DEFAULT_OPERATION_TIMEOUT_IN_MS));
        builder.defaultSocketReadTimeoutMs(configuration.getLong(DEFAULT_SOCKET_READ_TIMEOUT_IN_MS));

        return builder
                .build();
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(configuration, masters, tableName);
        result = 31 * result + Arrays.hashCode(keyNames);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuduLookupFunction that = (KuduLookupFunction) o;
        return Arrays.equals(keyNames, that.keyNames) && configuration.equals(that.configuration)
                && masters.equals(that.masters) && tableName.equals(that.tableName);
    }

    private KuduPredicate predicateComparator(ColumnSchema column, Object value) {

        KuduPredicate.ComparisonOp comparison = EQUAL;

        KuduPredicate predicate;

        switch (column.getType()) {
            case STRING:
                String data;
                data = (String) value;
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, data);
                break;
            case FLOAT:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (float) value);
                break;
            case INT8:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (byte) value);
                break;
            case INT16:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (short) value);
                break;
            case INT32:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (int) value);
                break;
            case INT64:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (long) value);
                break;
            case DOUBLE:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (double) value);
                break;
            case BOOL:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (boolean) value);
                break;
            case UNIXTIME_MICROS:
                Long time = (Long) value;
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, time * 1000);
                break;
            case BINARY:
                predicate = KuduPredicate.newComparisonPredicate(column, comparison, (byte[]) value);
                break;
            default:
                throw new IllegalArgumentException("Illegal var type: " + column.getType());
        }
        return predicate;
    }

    public void eval(Object... keys) throws Exception {
        if (keys.length != keyNames.length) {
            throw new RuntimeException("The length of lookUpKey and lookUpKeyVals is difference!");
        }
        Row keyRow = buildCacheKey(keys);
        if (this.cache != null) {
            ConcurrentMap<Row, List<Row>> cacheMap = this.cache.asMap();
            int keyCount = cacheMap.size();
            List<Row> cacheRows = this.cache.getIfPresent(keyRow);
            if (CollectionUtils.isNotEmpty(cacheRows)) {
                for (Row cacheRow : cacheRows) {
                    collect(cacheRow);
                }
                return;
            }
        }

        for (int retry = 1; retry <= maxRetries; retry++) {
            try {
                final KuduScanToken.KuduScanTokenBuilder scanTokenBuilder = client.newScanTokenBuilder(table);
                final Schema kuduTableSchema = table.getSchema();
                for (int i = 0; i < keyNames.length; i++) {
                    String keyName = keyNames[i];
                    Object value = keys[i];
                    final ColumnSchema column = kuduTableSchema.getColumn(keyName);
                    KuduPredicate predicate = predicateComparator(column, value);
                    scanTokenBuilder.addPredicate(predicate);
                }
                final List<KuduScanToken> tokenList = scanTokenBuilder.build();
                ArrayList<Row> rows = new ArrayList<>();
                for (final KuduScanToken token : tokenList) {
                    final List<LocatedTablet.Replica> replicas = token.getTablet().getReplicas();
                    final String[] array = replicas
                            .stream()
                            .map(replica -> replica.getRpcHost() + ":" + replica.getRpcPort())
                            .collect(Collectors.toList()).toArray(new String[replicas.size()]);
                    final byte[] scanToken = token.serialize();
                    final KuduScanner scanner = KuduScanToken.deserializeIntoScanner(scanToken, client);
                    for (RowResult rowResult : scanner) {
                        final Row row = convertor(rowResult);
                        if (cache != null) {
                            rows.add(row);
                        }
                        collect(row);
                    }
                }
                rows.trimToSize();
                if (cache != null) {
                    cache.put(keyRow, rows);
                }
                break;
            } catch (Exception e) {
                LOG.error(String.format("Kudu scan error, retry times = %d", retry), e);
                if (retry >= maxRetries) {
                    throw new RuntimeException("Execution of Kudu scan failed.", e);
                }
                try {
                    Thread.sleep(1000L * retry);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    public Row convertor(RowResult row) {
        Schema schema = row.getColumnProjection();

        Row values = new Row(schema.getColumnCount());
        schema.getColumns().forEach(column -> {
            String name = column.getName();
            int pos = schema.getColumnIndex(name);
            values.setField(pos, row.getObject(name));
        });
        return values;

    }

    private Row buildCacheKey(Object... keys) {
        return Row.of(keys);
    }
}
