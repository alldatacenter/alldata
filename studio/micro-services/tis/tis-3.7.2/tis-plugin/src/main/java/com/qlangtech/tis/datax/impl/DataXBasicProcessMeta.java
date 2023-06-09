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

package com.qlangtech.tis.datax.impl;

import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-18 09:50
 **/
public class DataXBasicProcessMeta {
    private boolean readerRDBMS;
    private boolean explicitTable;
    private boolean writerRDBMS;
    private boolean isWriterSupportMultiTableInReader;

    public static DataXBasicProcessMeta getDataXBasicProcessMetaByReader(
      Optional<DataxReader.BaseDataxReaderDescriptor> readerDesc) {
      Objects.requireNonNull(readerDesc, "readerDesc can not be null");
      DataXBasicProcessMeta processMeta = new DataXBasicProcessMeta();
      if (readerDesc.isPresent()) {
        DataxReader.BaseDataxReaderDescriptor rd = readerDesc.get();
        processMeta.setReaderHasExplicitTable(rd.hasExplicitTable());
        processMeta.setReaderRDBMS(rd.isRdbms());
      }
      return processMeta;
    }

    public boolean isWriterSupportMultiTableInReader() {
        return isWriterSupportMultiTableInReader;
    }

    public void setWriterSupportMultiTableInReader(boolean writerSupportMultiTableInReader) {
        isWriterSupportMultiTableInReader = writerSupportMultiTableInReader;
    }

    /**
     * 从非结构化的数据源导入到结构化的数据源，例如从OSS导入到MySQL
     *
     * @return
     */
    public boolean isReaderUnStructed() {
        return !readerRDBMS;
    }

    public boolean isWriterRDBMS() {
        return this.writerRDBMS;
    }

    public boolean isReaderRDBMS() {
        return readerRDBMS;
    }

    public boolean isExplicitTable() {
        return explicitTable;
    }

    public void setReaderRDBMS(boolean readerRDBMS) {
        this.readerRDBMS = readerRDBMS;
    }

    public void setReaderHasExplicitTable(boolean explicitTable) {
        this.explicitTable = explicitTable;
    }

    public void setWriterRDBMS(boolean writerRDBMS) {
        this.writerRDBMS = writerRDBMS;
    }

    @Override
    public String toString() {
        return "ProcessMeta{" +
                "readerRDBMS=" + readerRDBMS +
                ", writerRDBMS=" + writerRDBMS +
                '}';
    }
}
