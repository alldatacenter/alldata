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

package org.apache.inlong.sort.jdbc.table;

import org.apache.flink.connector.jdbc.dialect.JdbcDialect;
import org.apache.flink.connector.jdbc.internal.connection.SimpleJdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.inlong.sort.jdbc.internal.JdbcMultiBatchingComm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Default JDBC dialects implements for validate.
 */
public abstract class AbstractJdbcDialect implements JdbcDialect {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractJdbcDialect.class);
    public static final String PRIMARY_KEY_COLUMN = "pkColumn";

    @Override
    public void validate(TableSchema schema) throws ValidationException {
        for (int i = 0; i < schema.getFieldCount(); i++) {
            DataType dt = schema.getFieldDataType(i).get();
            String fieldName = schema.getFieldName(i).get();

            // TODO: We can't convert VARBINARY(n) data type to
            // PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO in
            // LegacyTypeInfoDataTypeConverter
            // when n is smaller than Integer.MAX_VALUE
            if (unsupportedTypes().contains(dt.getLogicalType().getTypeRoot())
                    || (dt.getLogicalType() instanceof VarBinaryType
                            && Integer.MAX_VALUE != ((VarBinaryType) dt.getLogicalType()).getLength())) {
                throw new ValidationException(
                        String.format(
                                "The %s dialect doesn't support type: %s.",
                                dialectName(), dt.toString()));
            }

            // only validate precision of DECIMAL type for blink planner
            if (dt.getLogicalType() instanceof DecimalType) {
                int precision = ((DecimalType) dt.getLogicalType()).getPrecision();
                if (precision > maxDecimalPrecision() || precision < minDecimalPrecision()) {
                    throw new ValidationException(
                            String.format(
                                    "The precision of field '%s' is out of the DECIMAL "
                                            + "precision range [%d, %d] supported by %s dialect.",
                                    fieldName,
                                    minDecimalPrecision(),
                                    maxDecimalPrecision(),
                                    dialectName()));
                }
            }

            // only validate precision of DECIMAL type for blink planner
            if (dt.getLogicalType() instanceof TimestampType) {
                int precision = ((TimestampType) dt.getLogicalType()).getPrecision();
                if (precision > maxTimestampPrecision() || precision < minTimestampPrecision()) {
                    throw new ValidationException(
                            String.format(
                                    "The precision of field '%s' is out of the TIMESTAMP "
                                            + "precision range [%d, %d] supported by %s dialect.",
                                    fieldName,
                                    minTimestampPrecision(),
                                    maxTimestampPrecision(),
                                    dialectName()));
                }
            }
        }
    }

    public abstract int maxDecimalPrecision();

    public abstract int minDecimalPrecision();

    public abstract int maxTimestampPrecision();

    public abstract int minTimestampPrecision();

    /**
     * Defines the unsupported types for the dialect.
     *
     * @return a list of logical type roots.
     */
    public abstract List<LogicalTypeRoot> unsupportedTypes();

    public abstract PreparedStatement setQueryPrimaryKeySql(Connection conn,
            String tableIdentifier) throws SQLException;

    /**
     * get getPkNames from query db.tb
     *
     * @return a list of PkNames.
     */
    public List<String> getPkNamesFromDb(String tableIdentifier,
            JdbcOptions jdbcOptions) {
        PreparedStatement st = null;
        try {
            JdbcOptions jdbcExecOptions = JdbcMultiBatchingComm.getExecJdbcOptions(jdbcOptions, tableIdentifier);
            SimpleJdbcConnectionProvider tableConnectionProvider = new SimpleJdbcConnectionProvider(jdbcExecOptions);
            Connection conn = tableConnectionProvider.getOrEstablishConnection();
            st = setQueryPrimaryKeySql(conn, tableIdentifier);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                String pkColumn = rs.getString(PRIMARY_KEY_COLUMN);
                LOG.info("TableIdentifier:{} get pkColumn:{}", tableIdentifier, pkColumn);
                checkAndClose(st);
                return Arrays.asList(pkColumn.split(","));
            } else {
                LOG.info("TableIdentifier:{} get pkColumn: null", tableIdentifier);
                checkAndClose(st);
                return null;
            }
        } catch (Exception e) {
            LOG.error("TableIdentifier:{} getAndSetPkNamesFromDb get err:", tableIdentifier, e);
            checkAndClose(st);
        }
        return null;
    }

    private void checkAndClose(PreparedStatement st) {
        if (null != st) {
            try {
                st.close();
            } catch (Exception e) {
                LOG.error("CheckAndClose PreparedStatement get err:", e);
            }
        }
    }
}
