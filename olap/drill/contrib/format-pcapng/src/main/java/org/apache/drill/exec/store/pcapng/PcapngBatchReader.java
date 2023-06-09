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
package org.apache.drill.exec.store.pcapng;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.exec.store.pcap.plugin.PcapFormatConfig;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.bmartel.pcapdecoder.PcapDecoder;
import fr.bmartel.pcapdecoder.structure.types.IPcapngType;
import fr.bmartel.pcapdecoder.structure.types.inter.IEnhancedPacketBLock;

public class PcapngBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(PcapngBatchReader.class);

  private final PcapFormatConfig config;
  private final EasySubScan scan;
  private final int maxRecords;
  private CustomErrorContext errorContext;
  private List<SchemaPath> columns;
  private List<ColumnDefn> projectedColumns;
  private Iterator<IPcapngType> pcapIterator;
  private IPcapngType block;
  private RowSetLoader loader;
  private InputStream in;
  private Path path;

  public PcapngBatchReader(final PcapFormatConfig config, final EasySubScan scan) {
    this.config = config;
    this.scan = scan;
    this.maxRecords = scan.getMaxRecords();
    this.columns = scan.getColumns();
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    try {
      // init InputStream for pcap file
      errorContext = negotiator.parentErrorContext();
      DrillFileSystem dfs = negotiator.fileSystem();
      path = dfs.makeQualified(negotiator.split().getPath());
      in = dfs.openPossiblyCompressedStream(path);
      // decode the pcap file
      PcapDecoder decoder = new PcapDecoder(IOUtils.toByteArray(in));
      decoder.decode();
      pcapIterator = decoder.getSectionList().iterator();
      logger.debug("The config is {}, root is {}, columns has {}", config, scan.getSelectionRoot(), columns);
    } catch (IOException e) {
      throw UserException
             .dataReadError(e)
             .message("Failure in initial pcapng inputstream. " + e.getMessage())
             .addContext(errorContext)
             .build(logger);
    } catch (Exception e) {
      throw UserException
             .dataReadError(e)
             .message("Failed to decode the pcapng file. " + e.getMessage())
             .addContext(errorContext)
             .build(logger);
    }
    // define the schema
    negotiator.tableSchema(defineMetadata(), true);
    ResultSetLoader resultSetLoader = negotiator.build();
    loader = resultSetLoader.writer();
    // bind the writer for columns
    bindColumns(loader);
    return true;
  }

  /**
   * The default of the `stat` parameter is false,
   * which means that the packet data is parsed and returned,
   * but if true, will return the statistics data about the each pcapng file only
   * (consist of the information about collect devices and the summary of the packet data above).
   *
   * In addition, a pcapng file contains a single Section Header Block (SHB),
   * a single Interface Description Block (IDB) and a few Enhanced Packet Blocks (EPB).
   * <pre>
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * | SHB | IDB | EPB | EPB |    ...    | EPB |
   * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   * </pre>
   * https://pcapng.github.io/pcapng/draft-tuexen-opsawg-pcapng.html#name-physical-file-layout
   */
  @Override
  public boolean next() {
    while (!loader.isFull()) {
      if (!pcapIterator.hasNext()) {
        return false;
      } else if (config.getStat() && isIEnhancedPacketBlock()) {
        continue;
      } else if (!config.getStat() && !isIEnhancedPacketBlock()) {
        continue;
      }
      processBlock();
      if (loader.limitReached(maxRecords)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(in);
  }

  private boolean isIEnhancedPacketBlock() {
    block = pcapIterator.next();
    return block instanceof IEnhancedPacketBLock;
  }

  private void processBlock() {
    loader.start();
    for (ColumnDefn columnDefn : projectedColumns) {
      // pcapng file name
      if (columnDefn.getName().equals(PcapColumn.PATH_NAME)) {
        columnDefn.load(path.getName());
      } else {
        // pcapng block data
        columnDefn.load(block);
      }
    }
    loader.save();
  }

  private boolean isSkipQuery() {
    return columns.isEmpty();
  }

  private boolean isStarQuery() {
    return Utilities.isStarQuery(columns);
  }

  private TupleMetadata defineMetadata() {
    SchemaBuilder builder = new SchemaBuilder();
    processProjected(columns);
    for (ColumnDefn columnDefn : projectedColumns) {
      columnDefn.define(builder);
    }
    return builder.buildSchema();
  }

  /**
   * <b> Define the schema based on projected </b><br/>
   * 1. SkipQuery: no field specified, such as count(*) <br/>
   * 2. StarQuery: select * <br/>
   * 3. ProjectPushdownQuery: select a,b,c <br/>
   */
  private void processProjected(List<SchemaPath> columns) {
    projectedColumns = new ArrayList<ColumnDefn>();
    if (isSkipQuery()) {
      projectedColumns.add(new ColumnDefn(PcapColumn.DUMMY_NAME, new PcapColumn.PcapDummy()));
    } else if (isStarQuery()) {
      Set<Map.Entry<String, PcapColumn>> pcapColumns;
      if (config.getStat()) {
        pcapColumns = PcapColumn.getSummaryColumns().entrySet();
      } else {
        pcapColumns = PcapColumn.getColumns().entrySet();
      }
      for (Map.Entry<String, PcapColumn> pcapColumn : pcapColumns) {
        makePcapColumns(projectedColumns, pcapColumn.getKey(), pcapColumn.getValue());
      }
    } else {
      for (SchemaPath schemaPath : columns) {
        // Support Case-Insensitive
        String projectedName = schemaPath.rootName().toLowerCase();
        PcapColumn pcapColumn;
        if (config.getStat()) {
          pcapColumn = PcapColumn.getSummaryColumns().get(projectedName);
        } else {
          pcapColumn = PcapColumn.getColumns().get(projectedName);
        }
        if (pcapColumn != null) {
          makePcapColumns(projectedColumns, projectedName, pcapColumn);
        } else {
          makePcapColumns(projectedColumns, projectedName, new PcapColumn.PcapDummy());
          logger.debug("{} missing the PcapColumn implement class.", projectedName);
        }
      }
    }
    Collections.unmodifiableList(projectedColumns);
  }

  private void makePcapColumns(List<ColumnDefn> projectedColumns, String name, PcapColumn column) {
    projectedColumns.add(new ColumnDefn(name, column));
  }

  private void bindColumns(RowSetLoader loader) {
    for (ColumnDefn columnDefn : projectedColumns) {
      columnDefn.bind(loader);
    }
  }

  private static class ColumnDefn {

    private final String name;
    private PcapColumn processor;
    private ScalarWriter writer;

    public ColumnDefn(String name, PcapColumn column) {
      this.name = name;
      this.processor = column;
    }

    public String getName() {
      return name;
    }

    public PcapColumn getProcessor() {
      return processor;
    }

    public void bind(RowSetLoader loader) {
      writer = loader.scalar(getName());
    }

    public void define(SchemaBuilder builder) {
      if (getProcessor().getType().getMode() == DataMode.REQUIRED) {
        builder.add(getName(), getProcessor().getType().getMinorType());
      } else {
        builder.addNullable(getName(), getProcessor().getType().getMinorType());
      }
    }

    public void load(IPcapngType block) {
      getProcessor().process(block, writer);
    }

    public void load(String value) {
      writer.setString(value);
    }
  }
}