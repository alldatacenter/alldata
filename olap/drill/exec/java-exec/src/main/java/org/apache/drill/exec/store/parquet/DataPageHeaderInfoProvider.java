/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.parquet;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.parquet.format.DataPageHeaderV2;
import org.apache.parquet.format.Encoding;
import org.apache.parquet.format.PageHeader;
import org.apache.parquet.format.Statistics;

public interface DataPageHeaderInfoProvider {
  int getNumValues();

  Encoding getEncoding();

  Encoding getDefinitionLevelEncoding();

  Encoding getRepetitionLevelEncoding();

  Statistics getStatistics();

  class DataPageHeaderV1InfoProvider implements DataPageHeaderInfoProvider {
    private final org.apache.parquet.format.DataPageHeader dataPageHeader;

    private DataPageHeaderV1InfoProvider(org.apache.parquet.format.DataPageHeader dataPageHeader) {
      this.dataPageHeader = dataPageHeader;
    }

    @Override
    public int getNumValues() {
      return dataPageHeader.getNum_values();
    }

    @Override
    public Encoding getEncoding() {
      return dataPageHeader.getEncoding();
    }

    @Override
    public Encoding getDefinitionLevelEncoding() {
      return dataPageHeader.getDefinition_level_encoding();
    }

    @Override
    public Encoding getRepetitionLevelEncoding() {
      return dataPageHeader.getRepetition_level_encoding();
    }

    @Override
    public Statistics getStatistics() {
      return dataPageHeader.getStatistics();
    }
  }

  class DataPageHeaderV2InfoProvider implements DataPageHeaderInfoProvider {
    private final DataPageHeaderV2 dataPageHeader;

    private DataPageHeaderV2InfoProvider(DataPageHeaderV2 dataPageHeader) {
      this.dataPageHeader = dataPageHeader;
    }

    @Override
    public int getNumValues() { return dataPageHeader.getNum_values(); }

    @Override
    public Encoding getEncoding() {
      return dataPageHeader.getEncoding();
    }

    @Override
    public Encoding getDefinitionLevelEncoding() { return Encoding.RLE; }

    @Override
    public Encoding getRepetitionLevelEncoding() { return Encoding.RLE; }

    @Override
    public Statistics getStatistics() {
      return dataPageHeader.getStatistics();
    }
  }

  static DataPageHeaderInfoProvider builder(PageHeader pageHeader) {
    switch (pageHeader.getType()) {
      case DATA_PAGE:
        return new DataPageHeaderV1InfoProvider(pageHeader.getData_page_header());
      case DATA_PAGE_V2:
        return new DataPageHeaderV2InfoProvider(pageHeader.getData_page_header_v2());
      default:
        throw new DrillRuntimeException("Unsupported page header type:" + pageHeader.getType());
    }
  }
}
