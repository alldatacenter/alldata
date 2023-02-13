/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.data.provider;

import datart.core.base.PageInfo;
import datart.core.base.consts.ValueType;
import datart.core.base.exception.Exceptions;
import datart.core.common.DateUtils;
import datart.core.data.provider.*;
import datart.data.provider.calcite.SqlParserUtils;
import datart.data.provider.calcite.dialect.SqlStdOperatorSupport;
import datart.data.provider.local.LocalDB;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlDialect;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public abstract class DefaultDataProvider extends DataProvider {

    public static final String TEST_DATA_SIZE = "size";

    public static final String SCHEMAS = "schemas";

    public static final String DEFAULT_DB = "default";

    protected static final String COLUMN_TYPE = "type";

    protected static final String COLUMN_NAME = "name";

    protected static final String TABLE = "tableName";

    protected static final String COLUMNS = "columns";

    @Override
    public Object test(DataProviderSource source) throws Exception {
        PageInfo pageInfo = PageInfo.builder()
                .pageNo(1)
                .pageSize(Integer.parseInt(source.getProperties().getOrDefault(TEST_DATA_SIZE, "100").toString()))
                .countTotal(false)
                .build();
        ExecuteParam executeParam = ExecuteParam.empty();
        executeParam.setPageInfo(pageInfo);
        return execute(source, null, executeParam);
    }

    @Override
    public Set<String> readAllDatabases(DataProviderSource source) {
        return Collections.singleton(DEFAULT_DB);
    }

    @Override
    public Set<String> readTables(DataProviderSource source, String database) {

        List<Map<String, Object>> schemas = (List<Map<String, Object>>) source.getProperties().get(SCHEMAS);

        if (CollectionUtils.isEmpty(schemas)) {
            return Collections.emptySet();
        }
        return schemas.stream()
                .map(s -> s.get(TABLE).toString())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Column> readTableColumns(DataProviderSource source, String schema, String table) {

        List<Map<String, Object>> schemas = (List<Map<String, Object>>) source.getProperties().get(SCHEMAS);

        List<Map<String, String>> columns = null;
        for (Map<String, Object> o : schemas) {
            if (table.equals(o.get(TABLE))) {
                columns = (List<Map<String, String>>) o.get(COLUMNS);
            }
        }
        if (columns == null) {
            return Collections.emptySet();
        }
        return columns.stream().map(col -> {
            Column column = new Column();
            column.setName(col.get(COLUMN_NAME));
            column.setType(ValueType.valueOf(col.get(COLUMN_TYPE).toUpperCase()));
            return column;
        }).collect(Collectors.toSet());

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public Dataframe execute(DataProviderSource config, QueryScript queryScript, ExecuteParam executeParam) throws Exception {

        Dataframes dataframes = loadDataFromSource(config);

        boolean persistent = isCacheEnabled(config);
        Date expire = null;
        if (persistent) {
            expire = getExpireTime(config);
        }
        if (!dataframes.isEmpty() && queryScript != null && !CollectionUtils.isEmpty(queryScript.getSchema())) {
            for (Dataframe dataframe : dataframes.getDataframes()) {
                for (Column column : dataframe.getColumns()) {
                    column.setType(queryScript.getSchema().getOrDefault(column.columnKey(), column).getType());
                }
                dataframe.setRows(parseValues(dataframe.getRows(), dataframe.getColumns()));
            }
            persistent = false;
        }
        return LocalDB.executeLocalQuery(queryScript, executeParam, dataframes, persistent, expire);
    }

    @Override
    public Set<StdSqlOperator> supportedStdFunctions(DataProviderSource source) {
        SqlDialect sqlDialect = LocalDB.SQL_DIALECT;

        if (!(sqlDialect instanceof SqlStdOperatorSupport)) {
            return super.supportedStdFunctions(source);
        }

        return ((SqlStdOperatorSupport) sqlDialect).supportedOperators();
    }


    protected List<Column> parseColumns(Map<String, Object> schema) {
        List<Column> columns = null;
        try {
            List<Map<String, String>> columnConfig = (List<Map<String, String>>) schema.get(COLUMNS);
            if (!CollectionUtils.isEmpty(columnConfig)) {
                columns = columnConfig
                        .stream()
                        .map(c -> Column.of(ValueType.valueOf(c.get(COLUMN_TYPE)), c.get(COLUMN_NAME)))
                        .collect(Collectors.toList());
            }
        } catch (ClassCastException ignored) {
        }
        return columns;
    }

    /**
     * 从数据源加载全量数据
     *
     * @param config 数据源配置
     */
    public abstract Dataframes loadDataFromSource(DataProviderSource config) throws Exception;

    /**
     * 检查该数据源缓存中数据是否存在
     */
    public boolean cacheExists(DataProviderSource config, String cacheKey) throws SQLException {
        Object cacheEnable = config.getProperties().get("cacheEnable");
        if (cacheEnable == null) {
            return false;
        }
        if (!Boolean.parseBoolean(cacheEnable.toString())) {
            return false;
        }
        return !LocalDB.checkCacheExpired(cacheKey);
    }

    @Override
    public boolean validateFunction(DataProviderSource source, String snippet) {
        try {
            SqlParserUtils.parseSnippet(snippet);
        } catch (Exception e) {
            Exceptions.e(e);
        }

        return true;
    }

    @Override
    public void resetSource(DataProviderSource source) {
        try {
            LocalDB.clearCache("DB" + source.getSourceId());
        } catch (Exception e) {
            log.error("reset datasource error ", e);
        }
    }

    protected List<List<Object>> parseValues(List<List<Object>> values, List<Column> columns) {
        if (CollectionUtils.isEmpty(values)) {
            return values;
        }
        if (values.get(0).size() != columns.size()) {
            Exceptions.msg("message.provider.default.schema", values.get(0).size() + ":" + columns.size());
        }
        values.stream().forEach(vals -> {
            for (int i = 0; i < vals.size(); i++) {
                Object val = vals.get(i);
                if (val == null) {
                    vals.set(i, null);
                    continue;
                }
                switch (columns.get(i).getType()) {
                    case STRING:
                        val = val.toString();
                        break;
                    case NUMERIC:
                        if (val instanceof Number) {
                            break;
                        }
                        if (StringUtils.isBlank(val.toString())) {
                            val = null;
                        } else if (NumberUtils.isDigits(val.toString())) {
                            val = Long.parseLong(val.toString());
                        } else if (NumberUtils.isNumber(val.toString())) {
                            val = Double.parseDouble(val.toString());
                        } else {
                            val = null;
                        }
                        break;
                    case DATE:
                        if (val instanceof Date) {
                            break;
                        }
                        String fmt = columns.get(i).getFmt();
                        if (StringUtils.isBlank(fmt)) {
                            fmt = DateUtils.inferDateFormat(val.toString());
                            columns.get(i).setFmt(fmt);
                        }
                        if (StringUtils.isNotBlank(fmt)) {
                            DateParser parser = FastDateFormat.getInstance(fmt);
                            try {
                                val = parser.parse(val.toString());
                            } catch (ParseException e) {
                                val = null;
                            }
                        } else {
                            val = null;
                        }
                    default:
                }
                vals.set(i, val);
            }
        });
        return values;
    }

    protected void removeHeader(List<List<Object>> values) {
        if (CollectionUtils.isEmpty(values)) {
            return;
        }
        boolean isHeader = values.get(0).stream()
                .allMatch(typedValue -> typedValue instanceof String);
        if (isHeader) {
            values.remove(0);
        }
    }

    protected boolean isCacheEnabled(DataProviderSource config) {
        try {
            return (boolean) config.getProperties().getOrDefault("cacheEnable", false);
        } catch (Exception e) {
            return false;
        }
    }

    protected Date getExpireTime(DataProviderSource config) {
        Object cacheTimeout = config.getProperties().get("cacheTimeout");
        if (cacheTimeout == null) {
            Exceptions.msg("cache timeout can not be empty");
        }
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, Integer.parseInt(cacheTimeout.toString()));
        return instance.getTime();
    }

}
