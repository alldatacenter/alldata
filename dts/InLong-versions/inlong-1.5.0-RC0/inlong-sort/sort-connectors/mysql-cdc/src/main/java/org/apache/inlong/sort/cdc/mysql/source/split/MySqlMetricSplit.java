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

package org.apache.inlong.sort.cdc.mysql.source.split;

import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;

import java.io.Serializable;
import java.util.Map;

/**
 * The split to describe a split of MySql metric.
 */
public class MySqlMetricSplit extends MySqlSplit {

    private Long numRecordsIn = 0L;

    private Long numBytesIn = 0L;

    /**
     * The table level metric in a split of mysql metric.
     */
    private Map<String, MySqlTableMetric> tableMetricMap;

    /**
     * The read phase timestamp metric in a split of mysql metric.
     */
    private Map<String, Long> readPhaseMetricMap;

    public Long getNumRecordsIn() {
        return numRecordsIn;
    }

    public void setNumRecordsIn(Long numRecordsIn) {
        this.numRecordsIn = numRecordsIn;
    }

    public Long getNumBytesIn() {
        return numBytesIn;
    }

    public void setNumBytesIn(Long numBytesIn) {
        this.numBytesIn = numBytesIn;
    }

    public Map<String, MySqlTableMetric> getTableMetricMap() {
        return tableMetricMap;
    }

    public void setTableMetricMap(
            Map<String, MySqlTableMetric> tableMetricMap) {
        this.tableMetricMap = tableMetricMap;
    }

    public Map<String, Long> getReadPhaseMetricMap() {
        return readPhaseMetricMap;
    }

    public void setReadPhaseMetricMap(Map<String, Long> readPhaseMetricMap) {
        this.readPhaseMetricMap = readPhaseMetricMap;
    }

    public MySqlMetricSplit(String splitId) {
        super(splitId);
    }

    public MySqlMetricSplit(Long numBytesIn, Long numRecordsIn, Map<String, Long> readPhaseMetricMap,
            Map<String, MySqlTableMetric> tableMetricMap) {
        this("");
        this.numBytesIn = numBytesIn;
        this.numRecordsIn = numRecordsIn;
        this.readPhaseMetricMap = readPhaseMetricMap;
        this.tableMetricMap = tableMetricMap;
    }

    public void setMetricData(long count, long byteNum) {
        numRecordsIn = numRecordsIn + count;
        numBytesIn = numBytesIn + byteNum;
    }

    @Override
    public Map<TableId, TableChange> getTableSchemas() {
        return null;
    }

    @Override
    public String toString() {
        return "MysqlMetricSplit{"
                + "numRecordsIn=" + numRecordsIn
                + ", numBytesIn=" + numBytesIn
                + ", tableMetricMap=" + tableMetricMap
                + ", readPhaseMetricMap=" + readPhaseMetricMap
                + '}';
    }

    /**
     * The mysql table level metric in a split of mysql metric.
     */
    public static class MySqlTableMetric implements Serializable {

        private Long numRecordsIn;

        private Long numBytesIn;

        public MySqlTableMetric(Long numRecordsIn, Long numBytesIn) {
            this.numRecordsIn = numRecordsIn;
            this.numBytesIn = numBytesIn;
        }

        public Long getNumRecordsIn() {
            return numRecordsIn;
        }

        public void setNumRecordsIn(Long numRecordsIn) {
            this.numRecordsIn = numRecordsIn;
        }

        public Long getNumBytesIn() {
            return numBytesIn;
        }

        public void setNumBytesIn(Long numBytesIn) {
            this.numBytesIn = numBytesIn;
        }

        @Override
        public String toString() {
            return "MySqlTableMetric{"
                    + "numRecordsIn=" + numRecordsIn
                    + ", numBytesIn=" + numBytesIn
                    + '}';
        }
    }
}
