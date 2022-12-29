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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import datart.core.base.consts.ValueType;
import datart.core.base.exception.BaseException;
import datart.core.base.exception.Exceptions;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.data.provider.jdbc.DataTypeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ResponseJsonParser implements HttpResponseParser {

    private static final String PROPERTY_SPLIT = "\\.";

    @Override
    public Dataframe parseResponse(String targetPropertyName, HttpResponse response, List<Column> columns) throws IOException {
        String jsonString = EntityUtils.toString(response.getEntity());

        JSONArray array;
        if (StringUtils.isEmpty(targetPropertyName)) {
            array = JSON.parseArray(jsonString);
        } else {
            JSONObject jsonObject = JSON.parseObject(jsonString);
            String[] split = targetPropertyName.split(PROPERTY_SPLIT);
            for (int i = 0; i < split.length - 1; i++) {
                jsonObject = jsonObject.getJSONObject(split[i]);
                if (jsonObject == null) {
                    Exceptions.tr(BaseException.class, "message.provider.http.property.miss", targetPropertyName);
                }
            }
            array = jsonObject.getJSONArray(split[split.length - 1]);
            if (array == null) {
                Exceptions.tr(BaseException.class, "message.provider.http.property.miss", targetPropertyName);
            }
        }
        Dataframe dataframe = new Dataframe();
        if (array == null || array.size() == 0) {
            return dataframe;
        }

        if (CollectionUtils.isEmpty(columns)) {
            columns = getSchema(array.getJSONObject(0));
        }

        dataframe.setColumns(columns);

        Set<String> columnKeySet = columns.stream().map(Column::columnName)
                .collect(Collectors.toSet());

        List<List<Object>> rows = array.toJavaList(JSONObject.class).parallelStream()
                .map(item -> {
                    return columnKeySet.stream().map(key -> {
                        Object val = item.get(key);
                        if (val instanceof JSONObject || val instanceof JSONArray) {
                            val = val.toString();
                        }
                        return val;
                    }).collect(Collectors.toList());
                }).collect(Collectors.toList());
        dataframe.setRows(rows);
        return dataframe;
    }

    private ArrayList<Column> getSchema(JSONObject jsonObject) {
        ArrayList<Column> columns = new ArrayList<>();
        for (String key : jsonObject.keySet()) {
            Column column = new Column();
            column.setName(key);
            Object val = jsonObject.get(key);
            if (val != null) {
                if (val instanceof JSONObject || val instanceof JSONArray) {
                    val = val.toString();
                }
                column.setType(DataTypeUtils.javaType2DataType(val));
            } else {
                column.setType(ValueType.STRING);
            }
            columns.add(column);
        }
        return columns;
    }

}
