/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.collect.Lists;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.DataXReaderColType;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-01 13:02
 **/
public class ParseColsResult {

    public static final String KEY_TYPE = "type";
    public static final String KEY_INDEX = "index";
    public static final String KEY_VALUE = "value";


    private static final Logger logger = LoggerFactory.getLogger(ParseColsResult.class);

    public DataXReaderTabMeta tabMeta;
    public boolean success;

    public interface IColProcessor {
        public boolean process(int colIndex, JSONObject col);
    }

    public static ParseColsResult parseColsCfg(String tabName, IFieldErrorHandler msgHandler
            , Context context, String fieldName, String value) {
        return parseColsCfg(tabName, msgHandler, context, fieldName, value, (index, col) -> true);
    }

    public static ParseColsResult parseColsCfg(String tabName, IFieldErrorHandler msgHandler
            , Context context, String fieldName, String value, IColProcessor colProcessor) {
        ParseColsResult parseOSSColsResult = new ParseColsResult();

        DataXReaderTabMeta tabMeta = new DataXReaderTabMeta(tabName);
        parseOSSColsResult.tabMeta = tabMeta;
        DataXColMeta colMeta = null;
        try {
            JSONArray cols = JSONArray.parseArray(value);
            if (cols.size() < 1) {
                msgHandler.addFieldError(context, fieldName, "请填写读取字段列表内容");
                return parseOSSColsResult;
            }
            Object firstElement = null;
            if (cols.size() == 1 && (firstElement = cols.get(0)) != null && "*".equals(String.valueOf(firstElement))) {
                tabMeta.allCols = true;
                return parseOSSColsResult.ok();
            }
            JSONObject col = null;
            String type = null;
            DataType parseType = null;
            Integer index = null;
            String appValue = null;
            for (int i = 0; i < cols.size(); i++) {
                col = cols.getJSONObject(i);
                type = getColType(col);
                if (StringUtils.isEmpty(type)) {
                    msgHandler.addFieldError(context, fieldName, "index为" + i + "的字段列中，属性type不能为空");
                    return parseOSSColsResult.faild();
                }
                parseType = DataXReaderColType.parse(type);
                if (parseType == null) {
                    msgHandler.addFieldError(context, fieldName
                            , "index为" + i + "的字段列中，属性type必须为:" + DataXReaderColType.toDesc() + "中之一");
                    return parseOSSColsResult.faild();
                }

                colMeta = new DataXColMeta(parseType);
                tabMeta.cols.add(colMeta);
                index = col.getInteger(KEY_INDEX);
                appValue = col.getString(KEY_VALUE);

                if (index == null && appValue == null) {
                    msgHandler.addFieldError(context, fieldName, "index为" + i + "的字段列中，index/value必须选择其一");
                    return parseOSSColsResult.faild();
                }

                if (!colProcessor.process(i, col)) {
                    return parseOSSColsResult.faild();
                }

                if (index != null) {
                    colMeta.index = index;
                }
                if (appValue != null) {
                    colMeta.value = appValue;
                }
            }
        } catch (Exception e) {
            logger.error(value, e);
            msgHandler.addFieldError(context, fieldName, "请检查内容格式是否有误:" + e.getMessage());
            return parseOSSColsResult.faild();
        }

        return parseOSSColsResult.ok();
    }

    public static String getColType(JSONObject col) {
        return col.getString(KEY_TYPE);
    }

    public ParseColsResult ok() {
        this.success = true;
        return this;
    }

    public ParseColsResult faild() {
        this.success = false;
        return this;
    }

    static class DataXColMeta {
        public final DataType parseType;

        // index和value两个属性为2选1
        private int index;
        private String value;

        public DataXColMeta(DataType parseType) {
            this.parseType = parseType;
        }
    }

    public static class DataXReaderTabMeta implements ISelectedTab {
        public boolean allCols = false;
        public final List<DataXColMeta> cols = Lists.newArrayList();
        private final String name;

        public DataXReaderTabMeta(String name) {
            this.name = name;
        }

        @Override
        @JSONField(serialize = false)
        public String getName() {
            return this.name;
        }

        @Override
        @JSONField(serialize = false)
        public String getWhere() {
            return StringUtils.EMPTY;
            // throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAllCols() {
            return this.allCols;
        }

        @Override
        public List<CMeta> getCols() {
            if (isAllCols()) {
                return Collections.emptyList();
            }
            return cols.stream().map((c) -> {
                CMeta cmeta = new CMeta();
                cmeta.setName(null);
                cmeta.setType(c.parseType);
                return cmeta;
            }).collect(Collectors.toList());
        }
    }
}
