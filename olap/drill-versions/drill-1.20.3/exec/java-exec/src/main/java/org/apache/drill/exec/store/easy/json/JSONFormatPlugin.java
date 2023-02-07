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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.QueryContext.SqlStatementType;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.planner.common.DrillStatsTable.TableStatistics;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.RecordWriter;
import org.apache.drill.exec.store.StatisticsRecordWriter;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.EasyFormatPlugin;
import org.apache.drill.exec.store.dfs.easy.EasyWriter;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.exec.store.easy.json.JSONFormatPlugin.JSONFormatConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONFormatPlugin extends EasyFormatPlugin<JSONFormatConfig> {

  private static final Logger logger = LoggerFactory.getLogger(JSONFormatPlugin.class);
  public static final String DEFAULT_NAME = "json";

  private static final boolean IS_COMPRESSIBLE = true;

  public static final String OPERATOR_TYPE = "JSON_SUB_SCAN";

  public JSONFormatPlugin(String name, DrillbitContext context,
      Configuration fsConf, StoragePluginConfig storageConfig) {
    this(name, context, fsConf, storageConfig, new JSONFormatConfig(null));
  }

  public JSONFormatPlugin(String name, DrillbitContext context,
      Configuration fsConf, StoragePluginConfig config, JSONFormatConfig formatPluginConfig) {
    super(name, context, fsConf, config, formatPluginConfig, true,
          false, false, IS_COMPRESSIBLE, formatPluginConfig.getExtensions(), DEFAULT_NAME);
  }

  @Override
  public RecordReader getRecordReader(FragmentContext context,
                                      DrillFileSystem dfs,
                                      FileWork fileWork,
                                      List<SchemaPath> columns,
                                      String userName) {
    return new JSONRecordReader(context, fileWork.getPath(), dfs, columns);
  }

  @Override
  public boolean isStatisticsRecordWriter(FragmentContext context, EasyWriter writer) {
    return context.getSQLStatementType() == SqlStatementType.ANALYZE;
  }

  @Override
  public StatisticsRecordWriter getStatisticsRecordWriter(FragmentContext context, EasyWriter writer)
      throws IOException {
    StatisticsRecordWriter recordWriter;
    //ANALYZE statement requires the special statistics writer
    if (!isStatisticsRecordWriter(context, writer)) {
      return null;
    }
    Map<String, String> options = setupOptions(context, writer, true);
    recordWriter = new JsonStatisticsRecordWriter(getFsConf(), this);
    recordWriter.init(options);
    return recordWriter;
  }

  @Override
  public RecordWriter getRecordWriter(FragmentContext context, EasyWriter writer) throws IOException {
    RecordWriter recordWriter;
    Map<String, String> options = setupOptions(context, writer, true);
    recordWriter = new JsonRecordWriter(writer.getStorageStrategy(), getFsConf());
    recordWriter.init(options);
    return recordWriter;
  }

  private Map<String, String> setupOptions(FragmentContext context, EasyWriter writer, boolean statsOptions) {
    Map<String, String> options = Maps.newHashMap();
    options.put("location", writer.getLocation());

    FragmentHandle handle = context.getHandle();
    String fragmentId = String.format("%d_%d", handle.getMajorFragmentId(), handle.getMinorFragmentId());
    options.put("prefix", fragmentId);
    options.put("separator", " ");
    options.put("extension", "json");
    options.put("extended", Boolean.toString(context.getOptions().getOption(ExecConstants.JSON_EXTENDED_TYPES)));
    options.put("uglify", Boolean.toString(context.getOptions().getOption(ExecConstants.JSON_WRITER_UGLIFY)));
    options.put("skipnulls", Boolean.toString(context.getOptions().getOption(ExecConstants.JSON_WRITER_SKIPNULLFIELDS)));
    options.put("enableNanInf", Boolean.toString(context.getOptions().getOption(ExecConstants.JSON_WRITER_NAN_INF_NUMBERS_VALIDATOR)));
    if (statsOptions) {
      options.put("queryid", context.getQueryIdString());
    }
    return options;
  }

  @Override
  public boolean supportsStatistics() {
    return true;
  }

  @Override
  public TableStatistics readStatistics(FileSystem fs, Path statsTablePath) throws IOException {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void writeStatistics(TableStatistics statistics, FileSystem fs, Path statsTablePath) throws IOException {
    FSDataOutputStream stream = null;
    JsonGenerator generator = null;
    try {
      JsonFactory factory = new JsonFactory();
      stream = fs.create(statsTablePath);
      ObjectMapper mapper = DrillStatsTable.getMapper();
      generator = factory.createGenerator((OutputStream) stream).useDefaultPrettyPrinter().setCodec(mapper);
      mapper.writeValue(generator, statistics);
    } catch (com.fasterxml.jackson.core.JsonGenerationException ex) {
      logger.error("Unable to create file (JSON generation error): " + statsTablePath.getName(), ex);
      throw ex;
    } catch (com.fasterxml.jackson.databind.JsonMappingException ex) {
      logger.error("Unable to create file (JSON mapping error): " + statsTablePath.getName(), ex);
      throw ex;
    } catch (IOException ex) {
      logger.error("Unable to create file " + statsTablePath.getName(), ex);
    } finally {
      if (generator != null) {
        generator.flush();
      }
      if (stream != null) {
        stream.close();
      }
    }
  }

  @JsonTypeName("json")
  public static class JSONFormatConfig implements FormatPluginConfig {
    private static final List<String> DEFAULT_EXTS = ImmutableList.of("json");

    private final List<String> extensions;

    @JsonCreator
    public JSONFormatConfig(
        @JsonProperty("extensions") List<String> extensions) {
      this.extensions = extensions == null ?
          DEFAULT_EXTS : ImmutableList.copyOf(extensions);
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public List<String> getExtensions() {
      return extensions;
    }

    @Override
    public int hashCode() {
      return Objects.hash(extensions);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      JSONFormatConfig other = (JSONFormatConfig) obj;
      return Objects.deepEquals(extensions, other.extensions);
    }

    @Override
    public String toString() {
      return new PlanStringBuilder(this)
          .field("extensions", extensions)
          .toString();
    }
  }

  @Override
  public String getReaderOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public String getWriterOperatorType() {
     return "JSON_WRITER";
  }

  @Override
  public boolean supportsPushDown() {
    return true;
  }
}
