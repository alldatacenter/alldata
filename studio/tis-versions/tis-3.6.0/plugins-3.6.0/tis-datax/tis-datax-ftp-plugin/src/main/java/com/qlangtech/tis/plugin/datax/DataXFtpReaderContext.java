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

import com.qlangtech.tis.datax.IDataxReaderContext;
import com.qlangtech.tis.plugin.datax.format.CSVFormat;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-01 14:52
 **/
public class DataXFtpReaderContext implements IDataxReaderContext {

    public static final String FTP_TASK = "ftp_datax";
    private final DataXFtpReader reader;


    public DataXFtpReaderContext(DataXFtpReader reader) {
        this.reader = reader;
        Objects.requireNonNull(this.reader.linker, "reader.linker can not be null");
    }

    public String getProtocol() {
        return this.reader.linker.protocol;
    }

    public String getHost() {
        return this.reader.linker.host;
    }

    public boolean isContainPort() {
        return this.reader.linker.port != null;
    }

    public Integer getPort() {
        return this.reader.linker.port;
    }

    public boolean isContainTimeout() {
        return this.reader.linker.timeout != null;
    }

    public Integer getTimeout() {
        return this.reader.linker.timeout;
    }

    public boolean isContainConnectPattern() {
        return StringUtils.isNotBlank(this.reader.linker.connectPattern);
    }

    public String getConnectPattern() {
        return this.reader.linker.connectPattern;
    }

    public String getFormat() {
        return this.reader.fileFormat.getFormat();
    }

    public String getUsername() {
        return this.reader.linker.username;
    }

    public String getPassword() {
        return this.reader.linker.password;
    }

    public String getPath() {
        return this.reader.path;
    }

    public String getColumn() {
        return this.reader.column;
    }

    public String getFieldDelimiter() {
        return this.reader.fileFormat.getFieldDelimiter();
    }

    public boolean isContainFieldDelimiter() {
        return StringUtils.isNotBlank(this.reader.fileFormat.getFieldDelimiter());
    }

    public boolean isContainCompress() {
        return StringUtils.isNotBlank(this.reader.compress);
    }

    public String getCompress() {
        return this.reader.compress;
    }

    public boolean isContainEncoding() {
        return StringUtils.isNotBlank(this.reader.encoding);
    }

    public String getEncoding() {
        return this.reader.encoding;
    }

    public boolean isContainSkipHeader() {
        return true;
    }

    public Boolean getSkipHeader() {
        return !this.reader.fileFormat.containHeader();
    }

    public boolean isContainNullFormat() {
        return StringUtils.isNotBlank(this.reader.nullFormat);
    }

    public String getNullFormat() {
        return this.reader.nullFormat;
    }

    public boolean isContainMaxTraversalLevel() {
        return StringUtils.isNotBlank(this.reader.maxTraversalLevel);
    }

    public String getMaxTraversalLevel() {
        return this.reader.maxTraversalLevel;
    }

    public boolean isContainCsvReaderConfig() {
        if (!(this.reader.fileFormat instanceof CSVFormat)) {
            return false;
        }
        return StringUtils.isNotBlank(((CSVFormat) this.reader.fileFormat).csvReaderConfig);
    }

    public String getCsvReaderConfig() {
        return ((CSVFormat) this.reader.fileFormat).csvReaderConfig;
    }

    @Override
    public String getTaskName() {
        return FTP_TASK;
    }

    @Override
    public String getSourceEntityName() {
        return null;
    }

    @Override
    public String getSourceTableName() {
        return null;
    }
}
