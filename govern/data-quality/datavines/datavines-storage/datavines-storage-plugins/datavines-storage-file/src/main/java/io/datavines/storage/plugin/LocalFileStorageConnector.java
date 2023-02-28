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
package io.datavines.storage.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.datavines.common.CommonConstants;
import io.datavines.common.param.form.PluginParams;
import io.datavines.common.param.form.PropsType;
import io.datavines.common.param.form.Validate;
import io.datavines.common.param.form.props.InputParamsProps;
import io.datavines.common.param.form.type.InputParam;
import io.datavines.storage.api.StorageConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class LocalFileStorageConnector implements StorageConnector {

    @Override
    public String getConfigJson(boolean isEn) {
        InputParam errorDataFileDir = getInputParam("data_dir",
                        isEn ? "data dir" : "目录路径",
                        isEn ? "data dir" : "请填入目录路径", Validate.newBuilder()
                        .setRequired(true).setMessage(isEn ? "please enter file dir" : "请填入目录路径")
                        .build(),"");

        InputParam lineSeparator = getInputParam("line_separator",
                        isEn ? "line separator" : "行分隔符",
                        isEn ? "please enter line separator" : "请填入行分隔符",  Validate.newBuilder()
                        .setRequired(false).setMessage(isEn ? "please enter line separator" : "请填入行分隔符")
                        .build(), "\r\n");
        
        InputParam columnSeparator = getInputParam("column_separator",
                        isEn ? "column separator" : "列分隔符",
                        isEn ? "please enter column separator" : "请填入列分隔符",  Validate.newBuilder()
                        .setRequired(false).setMessage(isEn ? "please enter column separator" : "请填入列分隔符")
                        .build(),"\001");

        List<PluginParams> params = new ArrayList<>();
        params.add(errorDataFileDir);
        params.add(lineSeparator);
        params.add(columnSeparator);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String result = null;

        try {
            result = mapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            log.error("json parse error : {}", e.getMessage(), e);
        }

        return result;
    }

    private InputParam getInputParam(String field, String title, String placeholder, Validate validate, String defaultValue) {
        return InputParam
                .newBuilder(field, title)
                .addValidate(validate)
                .setProps(new InputParamsProps().setDisabled(false))
                .setSize(CommonConstants.SMALL)
                .setType(PropsType.TEXT)
                .setRows(1)
                .setPlaceholder(placeholder)
                .setEmit(null)
                .setValue(defaultValue)
                .build();
    }

    @Override
    public Map<String, Object> getParamMap(Map<String, Object> parameter) {
        return parameter;
    }

}
