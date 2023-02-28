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

import com.qlangtech.tis.datax.IDataxContext;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-01 16:52
 **/
public class DataXFtpWriterContext implements IDataxContext {
    private final DataXFtpWriter writer;
    private final IDataxProcessor.TableMap tableMapper;

    public DataXFtpWriterContext(DataXFtpWriter writer, IDataxProcessor.TableMap tableMapper) {
        this.writer = writer;
        Objects.requireNonNull(writer.fileFormat, "prop fileFormat can not be null");
        Objects.requireNonNull(writer.linker, "prop linker can not be null");
        this.tableMapper = tableMapper;
    }

    public String getProtocol() {
        return this.writer.linker.protocol;
    }

    public String getHost() {
        return this.writer.linker.host;
    }

    public boolean isContainPort() {
        return this.writer.linker.port != null;
    }

    public Integer getPort() {
        return this.writer.linker.port;
    }

    public boolean isContainTimeout() {
        return this.writer.linker.timeout != null;
    }

    public Integer getTimeout() {
        return this.writer.linker.timeout;
    }

    public String getUsername() {
        return this.writer.linker.username;
    }

    public String getPassword() {
        return this.writer.linker.password;
    }

    public boolean isContainConnectPattern() {
        return StringUtils.isNotBlank(this.writer.linker.connectPattern);
    }

    public String getConnectPattern() {
        return this.writer.linker.connectPattern;
    }

    public String getPath() {
        if (StringUtils.isEmpty(this.writer.path)) {
            throw new IllegalStateException("writer path can not be null");
        }
        return this.writer.path;
    }

    public String getFileName() {
        return this.tableMapper.getTo();
    }

    public String getWriteMode() {
        return this.writer.writeMode;
    }

    public boolean isContainFieldDelimiter() {
        return StringUtils.isNotBlank(this.writer.fileFormat.getFieldDelimiter());
    }

    public String getFieldDelimiter() {
        return this.writer.fileFormat.getFieldDelimiter();
    }

    public boolean isContainEncoding() {
        return StringUtils.isNotBlank(this.writer.encoding);
    }

    public String getEncoding() {
        return this.writer.encoding;
    }

    public boolean isContainNullFormat() {
        return StringUtils.isNotBlank(this.writer.nullFormat);
    }

    public String getNullFormat() {
        return this.writer.nullFormat;
    }

    public boolean isContainDateFormat() {
        return StringUtils.isNotBlank(this.writer.dateFormat);
    }

    public String getDateFormat() {
        return this.writer.dateFormat;
    }

    public boolean isContainFileFormat() {
        // return StringUtils.isNotBlank(this.writer.fileFormat);
        return true;
    }

//    public boolean isContainCompress() {
//        return StringUtils.isNotBlank(this.writer.compress);
//    }

    public String getCompress() {
        if (StringUtils.isEmpty(this.writer.compress)) {
            throw new IllegalStateException("param compress can not be empty");
        }
        return this.writer.compress;
    }

    public String getFileFormat() {
        return this.writer.fileFormat.getFormat();
    }

    public boolean isContainSuffix() {
        return StringUtils.isNotBlank(this.writer.fileFormat.getSuffix());
    }

    public String getSuffix() {
        return this.writer.fileFormat.getSuffix();
    }

    public boolean isContainHeader() {
        List<CMeta> cols = tableMapper.getSourceCols();
        return (this.writer.fileFormat.containHeader() && cols.size() > 0);
    }

    public String getHeader() {
        return tableMapper.getSourceCols().stream().map((c) -> "'" + c.getName() + "'").collect(Collectors.joining(","));
    }

}
