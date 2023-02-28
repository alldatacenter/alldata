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
import com.qlangtech.tis.hdfs.impl.HdfsFileSystemFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-13 13:31
 **/
public class HdfsReaderContext implements IDataxReaderContext {
    private final DataXHdfsReader reader;
    private final HdfsFileSystemFactory fileSystemFactory;

    public HdfsReaderContext(DataXHdfsReader reader) {
        this.reader = reader;
        this.fileSystemFactory = (HdfsFileSystemFactory) this.reader.getFs();
    }

    public String getDataXName() {
        return reader.dataXName;
    }

    @Override
    public String getTaskName() {
        return reader.dataXName;
    }

    @Override
    public String getSourceEntityName() {
        return reader.dataXName;
    }

    @Override
    public String getSourceTableName() {
        return this.getSourceEntityName();
    }

    public String getDefaultFS() {
        return this.fileSystemFactory.getFSAddress();
    }

    public String getPath() {
        Path path = new Path(new Path(this.fileSystemFactory.rootDir), new Path(this.reader.path));
        return path.toString();
    }

    public String getFileType() {
        return this.reader.fileType;
    }

    public String getColumn() {
        return this.reader.column;
    }

    public boolean isContainFieldDelimiter() {
        return StringUtils.isNotBlank(this.reader.fieldDelimiter);
    }

    public String getFieldDelimiter() {
        return this.reader.fieldDelimiter;
    }

    public boolean isContainEncoding() {
        return StringUtils.isNotBlank(this.reader.encoding);
    }

    public String getEncoding() {
        return this.reader.encoding;
    }

    public boolean isContainNullFormat() {
        return StringUtils.isNotBlank(this.reader.nullFormat);
    }

    public String getNullFormat() {
        return this.reader.nullFormat;
    }

    public boolean isContainCompress() {
        return StringUtils.isNotBlank(this.reader.compress);
    }

    public String getCompress() {
        return this.reader.compress;
    }

    public boolean isContainCsvReaderConfig() {
        return StringUtils.isNotBlank(this.reader.csvReaderConfig);
    }

    public String getCsvReaderConfig() {
        return this.reader.csvReaderConfig;
    }

    public String getTemplate() {
        return this.reader.template;
    }
}
