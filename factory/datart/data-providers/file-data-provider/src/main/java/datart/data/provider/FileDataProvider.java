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

import datart.core.base.consts.FileFormat;
import datart.core.base.consts.ValueType;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.common.*;
import datart.core.data.provider.*;
import datart.data.provider.jdbc.DataTypeUtils;
import datart.data.provider.jdbc.SqlScriptRender;
import datart.data.provider.local.LocalDB;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class FileDataProvider extends DefaultDataProvider {

    public static final String FILE_FORMAT = "format";

    public static final String FILE_PATH = "path";

    private static final String I18N_PREFIX = "config.template.file.";

    @Override
    public String getConfigDisplayName(String name) {
        return MessageResolver.getMessage(I18N_PREFIX + name);
    }

    @Override
    public String getConfigDescription(String name) {
        String message = MessageResolver.getMessage(I18N_PREFIX + name + ".desc");
        if (message.startsWith(I18N_PREFIX)) {
            return null;
        } else {
            return message;
        }
    }

    @Override
    public String getQueryKey(DataProviderSource config, QueryScript script, ExecuteParam executeParam) throws Exception {
        SqlScriptRender render = new SqlScriptRender(script, executeParam, LocalDB.SQL_DIALECT);
        return "Q" + DigestUtils.md5Hex(render.render(true, true, true));
    }

    @Override
    public Dataframes loadDataFromSource(DataProviderSource config) throws Exception {
        Dataframes dataframes = Dataframes.of(config.getSourceId());
        if (cacheExists(config, dataframes.getKey())) {
            return dataframes;
        }
        Map<String, Object> properties = config.getProperties();
        List<Map<String, Object>> schemas;
        if (properties.containsKey(SCHEMAS)) {
            schemas = (List<Map<String, Object>>) properties.get(SCHEMAS);
        } else {
            schemas = Collections.singletonList(properties);
        }
        for (Map<String, Object> schema : schemas) {
            if (schema.get(FILE_PATH) == null || StringUtils.isEmpty(schema.get(FILE_PATH).toString())) {
                Exceptions.msg("message.file.notfound", schema.getOrDefault(TABLE, "").toString());
            }
            String path = schema.get(FILE_PATH).toString();
            FileFormat fileFormat = FileFormat.valueOf(schema.get(FILE_FORMAT).toString().toUpperCase());
            List<Column> columns = parseColumns(schema);
            Dataframe dataframe = loadFromPath(FileUtils.withBasePath(path), fileFormat, columns);
            if (dataframe != null) {
                dataframe.setName(StringUtils.isNoneBlank(schema.getOrDefault(TABLE, "").toString()) ? schema.get(TABLE).toString() : "TEST" + UUIDGenerator.generate());
                dataframes.add(dataframe);
            }
        }
        return dataframes;
    }

    private Dataframe loadFromPath(String path, FileFormat format, List<Column> columns) throws IOException {

        File file = new File(path);

        if (!file.exists()) {
            Exceptions.tr(BaseException.class, "message.file.notfound", file.getPath());
        }
        List<List<Object>> values = new LinkedList<>();
        if (file.isFile()) {
            values.addAll(loadSingleFile(path, format));
        } else {
            File[] files = file.listFiles();
            if (files == null) {
                return null;
            }
            for (File f : files) {
                values.addAll(loadSingleFile(f.getPath(), format));
            }
        }

        Dataframe dataframe = new Dataframe();
        if (values.size() == 0) {
            return dataframe;
        }

        if (columns == null) {
            columns = inferHeader(values);
        } else {
            removeHeader(values);
        }

        dataframe.setColumns(columns);

        values = parseValues(values, columns);

        dataframe.setRows(values);

        return dataframe;
    }

    @Override
    public void resetSource(DataProviderSource config) {
        super.resetSource(config);
        Map<String, Object> properties = config.getProperties();
        List<Map<String, Object>> schemas;
        if (properties.containsKey(SCHEMAS)) {
            schemas = (List<Map<String, Object>>) properties.get(SCHEMAS);
        } else {
            schemas = Collections.singletonList(properties);
        }
        Set<String> refFiles = new HashSet<>();
        for (Map<String, Object> schema : schemas) {
            String path = schema.get(FILE_PATH).toString();
            refFiles.add(FileUtils.withBasePath(path));
        }
        if (refFiles.size() > 0) {
            Set<String> refFilenames = refFiles.stream().map(FilenameUtils::getName).collect(Collectors.toSet());

            String basePath = FilenameUtils.getFullPath(refFiles.stream().findFirst().get());
            Set<String> fileNames = FileUtils.walkDir(new File(basePath), null, false);
            if (!CollectionUtils.isEmpty(fileNames)) {
                for (String fileName : fileNames) {
                    if (!refFilenames.contains(fileName)) {
                        String fileToDelete = FileUtils.concatPath(basePath, fileName);
                        log.info("delete unused file " + fileToDelete);
                        FileUtils.delete(fileToDelete);
                    }
                }
            }
        }
    }

    private List<Column> inferHeader(List<List<Object>> values) {
        List<Object> typedValues = values.get(0);
        LinkedList<Column> columns = new LinkedList<>();
        boolean isHeader = typedValues.stream()
                .allMatch(typedValue -> typedValue instanceof String);
        if (isHeader) {
            typedValues = values.size() > 1 ? values.get(1) : typedValues;
            for (int i = 0; i < typedValues.size(); i++) {
                Column column = new Column();
                ValueType valueType = DataTypeUtils.javaType2DataType(typedValues.get(i));
                column.setType(valueType);
                String name = values.get(0).get(i).toString();
                column.setName(StringUtils.isBlank(values.get(0).get(i).toString()) ? "col" + i : name);
                columns.add(column);
            }
            values.remove(0);
        } else {
            for (int i = 0; i < typedValues.size(); i++) {
                Column column = new Column();
                ValueType valueType = DataTypeUtils.javaType2DataType(typedValues.get(i));
                column.setType(valueType);
                column.setName("column" + i);
                columns.add(column);
            }
        }
        return columns;
    }

    private List<List<Object>> loadSingleFile(String path, FileFormat format) throws IOException {
        switch (format) {
            case XLS:
            case XLSX:
                return POIUtils.loadExcel(path);
            case CSV:
                return CSVParse.create(path).parse();
            default:
                Exceptions.tr(BaseException.class, "message.unsupported.format", format.getFormat());
                return null;
        }

    }

    @Override
    public String getConfigFile() {
        return "file-data-provider.json";
    }

}