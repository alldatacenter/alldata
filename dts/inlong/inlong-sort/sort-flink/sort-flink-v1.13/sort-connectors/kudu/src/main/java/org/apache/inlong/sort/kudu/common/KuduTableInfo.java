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

package org.apache.inlong.sort.kudu.common;

import org.apache.flink.table.types.DataType;
import org.apache.kudu.client.SessionConfiguration;
import org.apache.kudu.client.SessionConfiguration.FlushMode;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * The meta info of Kudu table client.
 */
public class KuduTableInfo implements Serializable {

    private final String masters;
    private final String tableName;

    private final String[] fieldNames;

    private final DataType[] dataTypes;

    /**
     * The flush mode of kudu client. <br/>
     * AUTO_FLUSH_BACKGROUND： calls will return immediately, but the writes will be sent in the background,
     * potentially batched together with other writes from the same session. <br/>
     * AUTO_FLUSH_SYNC: call will return only after being flushed to the server automatically. <br/>
     * MANUAL_FLUSH: calls will return immediately, but the writes will not be sent
     * until the user calls <code>KuduSession.flush()</code>.
     */
    private SessionConfiguration.FlushMode flushMode;

    private boolean forceInUpsertMode;

    public String getMasters() {
        return masters;
    }

    public String getTableName() {
        return tableName;
    }

    public String[] getFieldNames() {
        return fieldNames;
    }

    public DataType[] getDataTypes() {
        return dataTypes;
    }

    public FlushMode getFlushMode() {
        return flushMode;
    }

    public void setFlushMode(FlushMode flushMode) {
        this.flushMode = flushMode;
    }

    public boolean isForceInUpsertMode() {
        return forceInUpsertMode;
    }

    public void setForceInUpsertMode(boolean forceInUpsertMode) {
        this.forceInUpsertMode = forceInUpsertMode;
    }

    public KuduTableInfo(
            String masters,
            String tableName,
            String[] fieldNames,
            DataType[] dataTypes) {
        this.masters = masters;
        this.tableName = tableName;
        this.fieldNames = fieldNames;
        this.dataTypes = dataTypes;
    }

    public static KuduMetaInfoBuilder builder() {
        return new KuduMetaInfoBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KuduTableInfo that = (KuduTableInfo) o;
        return forceInUpsertMode == that.forceInUpsertMode &&
                masters.equals(that.masters) &&
                tableName.equals(that.tableName) &&
                Arrays.equals(fieldNames, that.fieldNames) &&
                Arrays.equals(dataTypes, that.dataTypes) &&
                flushMode == that.flushMode;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(masters, tableName, flushMode, forceInUpsertMode);
        result = 31 * result + Arrays.hashCode(fieldNames);
        result = 31 * result + Arrays.hashCode(dataTypes);
        return result;
    }

    public static final class KuduMetaInfoBuilder {

        private String masters;
        private String tableName;
        private String[] fieldNames;
        private DataType[] dataTypes;
        private FlushMode flushMode;
        private boolean forceInUpsertMode;

        private KuduMetaInfoBuilder() {
        }

        public static KuduMetaInfoBuilder aKuduMetaInfo() {
            return new KuduMetaInfoBuilder();
        }

        public KuduMetaInfoBuilder masters(String masters) {
            this.masters = masters;
            return this;
        }

        public KuduMetaInfoBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public KuduMetaInfoBuilder fieldNames(String[] fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public KuduMetaInfoBuilder dataTypes(DataType[] dataTypes) {
            this.dataTypes = dataTypes;
            return this;
        }

        public KuduMetaInfoBuilder flushMode(FlushMode flushMode) {
            this.flushMode = flushMode;
            return this;
        }

        public KuduMetaInfoBuilder forceInUpsertMode(boolean forceInUpsertMode) {
            this.forceInUpsertMode = forceInUpsertMode;
            return this;
        }

        public KuduTableInfo build() {
            KuduTableInfo kuduTableInfo = new KuduTableInfo(masters, tableName, fieldNames, dataTypes);
            kuduTableInfo.forceInUpsertMode = this.forceInUpsertMode;
            kuduTableInfo.flushMode = this.flushMode;
            return kuduTableInfo;
        }
    }
}
