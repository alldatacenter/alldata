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

package org.apache.drill.exec.store.xml;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;

import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;


public class XMLBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(XMLBatchReader.class);

  private FileSplit split;
  private RowSetLoader rootRowWriter;
  private CustomErrorContext errorContext;

  private XMLReader reader;
  private final int maxRecords;
  private final int dataLevel;


  static class XMLReaderConfig {
    final XMLFormatPlugin plugin;
    final int dataLevel;

    XMLReaderConfig(XMLFormatPlugin plugin) {
      this.plugin = plugin;
      dataLevel = plugin.getConfig().dataLevel;
    }
  }

  public XMLBatchReader(XMLReaderConfig readerConfig, EasySubScan scan) {
    this.maxRecords = scan.getMaxRecords();
    this.dataLevel = readerConfig.dataLevel;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    ResultSetLoader loader = negotiator.build();
    errorContext = negotiator.parentErrorContext();
    rootRowWriter = loader.writer();

    openFile(negotiator);
    return true;
  }

  @Override
  public boolean next() {
    return reader.next();
  }

  @Override
  public void close() {
    reader.close();
  }

  private void openFile(FileScanFramework.FileSchemaNegotiator negotiator) {
    try {
      InputStream fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      reader = new XMLReader(fsStream, dataLevel, maxRecords);
      reader.open(rootRowWriter, errorContext);
    } catch (Exception e) {
      throw UserException
        .dataReadError(e)
        .message(String.format("Failed to open input file: %s", split.getPath().toString()))
        .addContext(errorContext)
        .addContext(e.getMessage())
        .build(logger);
    }
  }
}
