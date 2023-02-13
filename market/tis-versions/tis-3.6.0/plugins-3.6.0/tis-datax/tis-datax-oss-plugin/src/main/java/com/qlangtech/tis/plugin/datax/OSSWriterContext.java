/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.config.aliyun.IHttpToken;
import com.qlangtech.tis.datax.IDataxReaderContext;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-14 11:59
 **/
public class OSSWriterContext implements IDataxReaderContext {

    private final DataXOssWriter writer;

    public OSSWriterContext(DataXOssWriter ossWriter) {
        this.writer = ossWriter;
    }

    public IHttpToken getOss() {
        return writer.getOSSConfig();
    }

    public String getBucket() {
        return writer.bucket;
    }

    public String getObject() {
        return writer.object;
    }

    public String getWriteMode() {
        return writer.writeMode;
    }

    public String getFieldDelimiter() {
        return writer.fieldDelimiter;
    }

    public String getEncoding() {
        return writer.encoding;
    }

    public String getNullFormat() {
        return writer.nullFormat;
    }

    public String getDateFormat() {
        return this.writer.dateFormat;
    }

    public String getFileFormat() {
        return this.writer.fileFormat;
    }

    public String getHeader() {
        return this.writer.header;
    }

    public Integer getMaxFileSize() {
        return this.writer.maxFileSize;
    }

    @Override
    public String getTaskName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceTableName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSourceEntityName() {
        throw new UnsupportedOperationException();
    }
}
