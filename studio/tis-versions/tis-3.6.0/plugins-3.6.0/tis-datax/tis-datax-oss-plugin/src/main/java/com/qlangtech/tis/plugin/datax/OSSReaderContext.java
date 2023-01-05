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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.config.aliyun.IHttpToken;
import com.qlangtech.tis.datax.IDataxReaderContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-13 18:27
 **/
public class OSSReaderContext implements IDataxReaderContext {
    private final DataXOssReader reader;

    @Override
    public String getTaskName() {
        //throw new UnsupportedOperationException();
        return reader.getTaskName();
        // return StringUtils.replace(StringUtils.remove(reader.object, "*"), "/", "-");
    }

    @Override
    public String getSourceEntityName() {
        return "oss";
    }

    @Override
    public String getSourceTableName() {
        return this.getSourceEntityName();
    }

    public OSSReaderContext(DataXOssReader reader) {
        this.reader = reader;
    }

    public IHttpToken getOss() {
        return reader.getOSSConfig();
    }

    public String getObject() {
        return reader.object;
    }

    public String getBucket() {
        return reader.bucket;
    }

    public String getCols() {
        return reader.column;
    }

    public String getFieldDelimiter() {
        return reader.fieldDelimiter;
    }

    public String getCompress() {
        return reader.compress;
    }

    public boolean isContainCompress() {
        return StringUtils.isNotBlank(reader.compress);
    }

    public boolean isContainEncoding() {
        return StringUtils.isNotBlank(reader.encoding);
    }

    public boolean isContainNullFormat() {
        return StringUtils.isNotBlank(reader.nullFormat);
    }

    public String getEncoding() {
        return reader.encoding;
    }

    public String getNullFormat() {
        // \N  -> \\N
        return StringEscapeUtils.escapeJava(reader.nullFormat);
    }

    public Boolean getSkipHeader() {
        return reader.skipHeader;
    }

    public boolean isContainCsvReaderConfig() {
        try {
            JSONObject o = JSON.parseObject(reader.csvReaderConfig);
            return o.keySet().size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getCsvReaderConfig() {
        // 为一个json格式
        return reader.csvReaderConfig;
    }


}
