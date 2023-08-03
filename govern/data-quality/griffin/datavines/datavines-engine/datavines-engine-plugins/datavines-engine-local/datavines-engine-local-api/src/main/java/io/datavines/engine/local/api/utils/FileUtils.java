/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.engine.local.api.utils;

import io.datavines.connector.api.TypeConverter;
import io.datavines.engine.local.api.entity.QueryColumn;
import io.datavines.engine.local.api.entity.ResultListWithColumns;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.datavines.common.ConfigConstants.DOUBLE_AT;

@Slf4j
public class FileUtils {

    public static void writeToLocal(ResultListWithColumns resultListWithColumns,
                                    String directory,
                                    String name,
                                    boolean needHeader,
                                    TypeConverter typeConverter,
                                    String columnSeparator) {

        BufferedWriter bw = null;
        try {

            File localErrorDir = new File(directory);

            if (!localErrorDir.exists()){
                org.apache.commons.io.FileUtils.forceMkdir(localErrorDir);
            }

            bw = new BufferedWriter(new FileWriter(directory + File.separator + name +".csv",true));

            if (resultListWithColumns != null && CollectionUtils.isNotEmpty(resultListWithColumns.getResultList())) {
                List<QueryColumn> columns = resultListWithColumns.getColumns();
                List<String> headerList = new ArrayList<>();
                columns.forEach(header -> {
                    headerList.add(header.getName().toLowerCase() + DOUBLE_AT + typeConverter.convert(header.getType()).toString().toLowerCase());
                });
                if (needHeader) {
                    bw.write(String.join(columnSeparator,headerList));
                    bw.newLine();
                }

                for (Map<String, Object> row: resultListWithColumns.getResultList()) {
                    List<String> rowDataList = new ArrayList<>();
                    headerList.forEach(header -> {
                        rowDataList.add((String.valueOf(row.get(header.split(DOUBLE_AT)[0]))).toLowerCase());

                    });
                    bw.write(String.join(columnSeparator,rowDataList));
                    bw.newLine();
                }
                bw.flush();
                bw.close();
            }
        } catch (IOException e) {
            log.error("write data error {1}", e);
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException ioe) {
                log.error("close buffer writer error {1}", ioe);
            }
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException ioe) {
                log.error("close buffer writer error {1}", ioe);
            }
        }
    }

    public static void writeToLocal(List<String> resultList,
                                    String directory,
                                    String name) {

        BufferedWriter bw = null;
        try {

            File localErrorDir = new File(directory);

            if (!localErrorDir.exists()){
                org.apache.commons.io.FileUtils.forceMkdir(localErrorDir);
            }

            bw = new BufferedWriter(new FileWriter(directory + File.separator + name +".csv",true));

            if (CollectionUtils.isNotEmpty(resultList)) {

                for(String string: resultList) {
                    bw.write(string);
                    bw.newLine();
                }
                bw.flush();
                bw.close();
            }
        } catch (IOException e) {
            log.error("write data error {1}", e);
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException ioe) {
                log.error("close buffer writer error {1}", ioe);
            }
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException ioe) {
                log.error("close buffer writer error {1}", ioe);
            }
        }
    }

    public static List<String> readPartFileContent(String filePath,
                                             int skipLine,
                                             int limit){
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.skip(skipLine).limit(limit).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("read file error",e);
        }
        return Collections.emptyList();

    }
}
