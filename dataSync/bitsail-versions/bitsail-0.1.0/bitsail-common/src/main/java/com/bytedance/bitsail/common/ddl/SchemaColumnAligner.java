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

package com.bytedance.bitsail.common.ddl;

import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnector;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.model.ColumnInfo;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SchemaColumnAligner {

  private final SourceEngineConnector sourceEngineConnector;
  private final SinkEngineConnector sinkEngineConnector;

  public SchemaColumnAligner(SourceEngineConnector sourceEngineConnector,
                             SinkEngineConnector sinkEngineConnector) {

    this.sourceEngineConnector = sourceEngineConnector;
    this.sinkEngineConnector = sinkEngineConnector;
  }

  public SchemaHelper.SchemaIntersectionResponse doIntersectStrategy() throws Exception {
    List<ColumnInfo> sourceExternalColumnInfos = sourceEngineConnector
        .getExternalColumnInfos();
    List<ColumnInfo> sinkExternalColumnInfos = sinkEngineConnector
        .getExternalColumnInfos();
    return intersectColumns(sourceExternalColumnInfos, sinkExternalColumnInfos);
  }

  public List<ColumnInfo> doSourceOnlyStrategy() throws Exception {
    return sourceEngineConnector.getExternalColumnInfos();
  }

  SchemaHelper.SchemaIntersectionResponse intersectColumns(List<ColumnInfo> readerColumns,
                                                           List<ColumnInfo> writerColumns) {
    return SchemaHelper.intersectColumns(readerColumns, writerColumns);
  }

  public enum ColumnAlignmentStrategy {
    /**
     * Disable column alignment action
     */
    disable,

    /**
     * Intersect source & sink columns
     */
    intersect,

    /**
     * Use source columns instead of sink columns
     */
    source_only
  }
}
