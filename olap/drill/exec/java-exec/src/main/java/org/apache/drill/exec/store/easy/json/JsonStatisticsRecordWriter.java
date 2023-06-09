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
package org.apache.drill.exec.store.easy.json;

import java.io.IOException;
import java.util.Map;

import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.StatisticsRecordWriter;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.store.JSONBaseStatisticsRecordWriter;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class JsonStatisticsRecordWriter extends JSONBaseStatisticsRecordWriter implements StatisticsRecordWriter {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JsonStatisticsRecordWriter.class);

  private String location;
  private String prefix;
  private String extension;
  private FileSystem fs = null;
  private final Configuration fsConf;
  private final FormatPlugin formatPlugin;
  private Path fileName = null;

  private long recordsWritten = -1;

  private final StatisticsCollectorImpl statisticsCollector = new StatisticsCollectorImpl();

  public JsonStatisticsRecordWriter(Configuration fsConf, FormatPlugin formatPlugin) {
    this.fsConf = fsConf;
    this.formatPlugin = formatPlugin;
  }

  @Override
  public void init(Map<String, String> writerOptions) {
    this.location = writerOptions.get("location");
    this.prefix = writerOptions.get("prefix");
    this.extension = writerOptions.get("extension");
    this.skipNullFields = Boolean.parseBoolean(writerOptions.get("skipnulls"));
    String queryId = writerOptions.get("queryid");
     //Write as DRILL process user
    this.fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fsConf);

    fileName = new Path(location, prefix + "." + extension + ".tmp." + queryId);
    // Delete .tmp file if exists. Unexpected error in cleanup during last ANALYZE
    try {
      if (fs.exists(fileName)) {
        fs.delete(fileName, false);
      }
    } catch (IOException ex) {
      throw UserException.dataWriteError(ex)
        .addContext(String.format("Unable to delete tmp statistics file %s", fileName))
        .build(logger);
    }
    try {
      // Delete the tmp file and .stats.drill on exit. After writing out the permanent file
      // we cancel the deleteOnExit. This ensures that if prior to writing out the stats
      // file the process is killed, we perform the cleanup.
      fs.deleteOnExit(fileName);
      fs.deleteOnExit(new Path(location));
      logger.debug("Created file: {}", fileName);
    } catch (IOException ex) {
      throw UserException.dataWriteError(ex)
        .addContext(String.format("Unable to create stistics file %s", fileName))
        .build(logger);
    }
  }

  @Override
  public void updateSchema(VectorAccessible batch) {
    // no op
  }

  @Override
  public boolean isBlockingWriter() {
    return true;
  }

  @Override
  public void checkForNewPartition(int index) {
    // no op
  }

  @Override
  public FieldConverter getNewBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewBigIntConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewIntConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewDateConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewDateConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewVarCharConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewVarCharConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableBigIntConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewNullableBigIntConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableVarBinaryConverter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewNullableVarBinaryConverter(fieldId, fieldName, reader);
  }

  @Override
  public FieldConverter getNewNullableFloat8Converter(int fieldId, String fieldName, FieldReader reader) {
    return statisticsCollector.getNewNullableFloat8Converter(fieldId, fieldName, reader);
  }

  @Override
  public void startStatisticsRecord() {
    statisticsCollector.startStatisticsRecord();
  }

  @Override
  public void endStatisticsRecord() {
    statisticsCollector.endStatisticsRecord();
    ++recordsWritten;
  }

  @Override
  public boolean hasStatistics() {
    return recordsWritten > 0;
  }

  @Override
  public DrillStatsTable.TableStatistics getStatistics() {
    return statisticsCollector.getStatistics();
  }

  @Override
  public void flushBlockingWriter() throws IOException {
    Path permFileName = new Path(location, prefix + "." + extension);
    try {
      if (statisticsCollector.hasErrors()) {
        // Encountered some error
        throw new IOException("Statistics writer encountered unexpected field");
      } else if (recordsWritten < 0) {
        throw new IOException("Statistics writer did not have data");
      }
      if (formatPlugin.supportsStatistics()) {
        // Invoke the format plugin stats API to write out the stats
        formatPlugin.writeStatistics(statisticsCollector.getStatistics(), fs, fileName);
        // Delete existing permanent file and rename .tmp file to permanent file
        // If failed to do so then delete the .tmp file
        fs.delete(permFileName, false);
        fs.rename(fileName, permFileName);
        // Cancel delete once perm file is created
        fs.cancelDeleteOnExit(fileName);
        fs.cancelDeleteOnExit(new Path(location));
      }
      logger.debug("Created file: {}", permFileName);
    } catch(IOException ex) {
      logger.error("Unable to create file: " + permFileName, ex);
      throw ex;
    }
  }

  @Override
  public void abort() {
    // Invoke cleanup to clear any .tmp files and/or empty statistics directory
    cleanup();
  }

  @Override
  public void cleanup() {
    Path permFileName = new Path(location, prefix + "." + extension);
    try {
      // Remove the .tmp file, if any
      if (fs.exists(fileName)) {
        fs.delete(fileName, false);
        logger.debug("Deleted file: {}", fileName);
      }
      // Also delete the .stats.drill directory if no permanent file exists.
      if (!fs.exists(permFileName)) {
        fs.delete(new Path(location), false);
        logger.debug("Deleted directory: {}", location);
      }
    } catch (IOException ex) {
      // Warn but continue
      logger.warn("Unable to delete tmp satistics file: " + fileName, ex);
    }
  }
}
