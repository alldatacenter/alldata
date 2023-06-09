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
package org.apache.drill.exec.planner.common;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.record.MajorTypeSerDe;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.parquet.ParquetFormatConfig;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.metastore.statistics.Histogram;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Wraps the stats table info including schema and tableName. Also materializes stats from storage
 * and keeps them in memory.
 */
public class DrillStatsTable {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillStatsTable.class);
  // All the statistics versions till date
  public enum STATS_VERSION {V0, V1}
  // The current version
  public static final STATS_VERSION CURRENT_VERSION = STATS_VERSION.V1;
  // 10 histogram buckets (TODO: can make this configurable later)
  public static final int NUM_HISTOGRAM_BUCKETS = 10;

  private final FileSystem fs;
  private final Path tablePath;
  private final String schemaName;
  private final String tableName;
  private final Map<SchemaPath, Long> ndv = new HashMap<>();
  private final Map<SchemaPath, Histogram> histogram = new HashMap<>();
  private double rowCount = -1;
  private final Map<SchemaPath, Long> nnRowCount = new HashMap<>();
  private boolean materialized = false;
  private DrillTable table;
  private TableStatistics statistics = null;

  public DrillStatsTable(DrillTable table, String schemaName, String tableName, Path tablePath, FileSystem fs) {
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.tablePath = tablePath;
    this.fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fs.getConf());
    this.table = table;
  }

  public DrillStatsTable(TableStatistics statistics) {
    this.statistics = statistics;
    this.schemaName = null;
    this.tableName = null;
    this.tablePath = null;
    this.fs = null;
    materializeFromStatistics();
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getTableName() {
    return tableName;
  }

  /*
   * Returns whether statistics have materialized or not i.e. were the table statistics successfully read
   * from the persistent store?
   */
  public boolean isMaterialized() { return materialized; }
  /**
   * Get the approximate number of distinct values of given column. If stats are not present for the
   * given column, a null is returned.
   *
   * Note: returned data may not be accurate. Accuracy depends on whether the table data has changed
   * after the stats are computed.
   *
   * @param col - column for which approximate count distinct is desired
   * @return approximate count distinct of the column, if available. NULL otherwise.
   */
  public Double getNdv(SchemaPath col) {
    // Stats might not have materialized because of errors.
    if (!materialized) {
      return null;
    }
    Long ndvCol = ndv.get(col);
    // Ndv estimation techniques like HLL may over-estimate, hence cap it at rowCount
    if (ndvCol != null) {
      return (double) Math.min(ndvCol, rowCount);
    }
    return null;
  }

  public Set<SchemaPath> getColumns() {
    return ndv.keySet();
  }

  /**
   * Get row count of the table. Returns null if stats are not present.
   *
   * Note: returned data may not be accurate. Accuracy depends on whether the table data has
   * changed after the stats are computed.
   *
   * @return rowcount for the table, if available. NULL otherwise.
   */
  public Double getRowCount() {
    // Stats might not have materialized because of errors.
    if (!materialized) {
      return null;
    }
    return rowCount > 0 ? rowCount : null;
  }

  /**
   * Get non-null rowcount for the column If stats are not present for the given column, a null is returned.
   *
   * Note: returned data may not be accurate. Accuracy depends on whether the table data has changed
   * after the stats are computed.
   *
   * @param col - column for which non-null rowcount is desired
   * @return non-null rowcount of the column, if available. NULL otherwise.
   */
  public Double getNNRowCount(SchemaPath col) {
    // Stats might not have materialized because of errors.
    if (!materialized) {
      return null;
    }
    Long nnRowCntCol = nnRowCount.get(col);
    // Cap it at row count (just in case)
    if (nnRowCntCol != null) {
      return Math.min(nnRowCntCol, rowCount);
    }
    return null;
  }

  /**
   * Get the histogram of a given column. If stats are not present for the given column,
   * a null is returned.
   * <p>
   * Note: returned data may not be accurate. Accuracy depends on whether the table data has changed after the
   * stats are computed.
   *
   * @param column path to the column whose histogram should be obtained
   * @return Histogram for this column
   */
  public Histogram getHistogram(SchemaPath column) {
    // Stats might not have materialized because of errors.
    if (!materialized) {
      return null;
    }
    return histogram.get(column);
  }

  /**
   * Read the stats from storage and keep them in memory.
   */
  public void materialize() {
    try {
      // for the case when tablePath is not set, or it does not exists, statistics cannot be read
      if (materialized || tablePath == null || !fs.exists(tablePath)) {
        return;
      }
      // Deserialize statistics from JSON
      this.statistics = readStatistics(table, tablePath);
      // Handle based on the statistics version read from the file
      materializeFromStatistics();
    } catch (FileNotFoundException ex) {
      logger.debug(String.format("Did not find statistics file %s", tablePath.toString()), ex);
    } catch (IOException ex) {
      logger.debug(String.format("Error trying to read statistics table %s", tablePath.toString()), ex);
    }
  }

  private void materializeFromStatistics() {
    if (statistics instanceof Statistics_v0) {
      // Do nothing
    } else if (statistics instanceof Statistics_v1) {
      for (DirectoryStatistics_v1 ds : ((Statistics_v1) statistics).getDirectoryStatistics()) {
        for (ColumnStatistics_v1 cs : ds.getColumnStatistics()) {
          ndv.put(cs.getName(), cs.getNdv());
          nnRowCount.put(cs.getName(), (long) cs.getNonNullCount());
          rowCount = Math.max(rowCount, cs.getCount());

          // get the histogram for this column
          Histogram hist = cs.getHistogram();
          histogram.put(cs.getName(), hist);
        }
      }
    }
    if (statistics != null) { // See stats are available before setting materialized
      materialized = true;
    }
  }

  /* Each change to the format SHOULD increment the default and/or the max values of the option
   * exec.statistics.capability_version
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
      property = "statistics_version")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = DrillStatsTable.Statistics_v1.class, name="v0"),
      @JsonSubTypes.Type(value = DrillStatsTable.Statistics_v1.class, name="v1")
  })
  public static abstract class TableStatistics {
    @JsonIgnore public abstract List<? extends DirectoryStatistics> getDirectoryStatistics();
  }

  public static abstract class DirectoryStatistics {
  }

  public static abstract class ColumnStatistics {
  }

  @JsonTypeName("v0")
  public static class Statistics_v0 extends TableStatistics {
    @JsonProperty ("directories") List<DirectoryStatistics_v0> directoryStatistics;
    // Default constructor required for deserializer
    public Statistics_v0 () { }
    @Override
    @JsonGetter ("directories")
    public List<DirectoryStatistics_v0> getDirectoryStatistics() {
      return directoryStatistics;
    }
    @JsonSetter ("directories")
    public void setDirectoryStatistics(List<DirectoryStatistics_v0> directoryStatistics) {
      this.directoryStatistics = directoryStatistics;
    }
  }

  public static class DirectoryStatistics_v0 extends DirectoryStatistics {
    @JsonProperty private double computed;
    // Default constructor required for deserializer
    public DirectoryStatistics_v0() { }
    @JsonGetter ("computed")
    public double getComputedTime() {
      return this.computed;
    }
    @JsonSetter ("computed")
    public void setComputedTime(double computed) {
      this.computed = computed;
    }
  }

  /**
   * Struct which contains the statistics for the entire directory structure
   */
  @JsonTypeName("v1")
  public static class Statistics_v1 extends TableStatistics {
    @JsonProperty ("directories")
    List<DirectoryStatistics_v1> directoryStatistics;
    // Default constructor required for deserializer
    public Statistics_v1 () { }
    @Override
    @JsonGetter ("directories")
    public List<DirectoryStatistics_v1> getDirectoryStatistics() {
      return directoryStatistics;
    }
    @JsonSetter ("directories")
    public void setDirectoryStatistics(List<DirectoryStatistics_v1> directoryStatistics) {
      this.directoryStatistics = directoryStatistics;
    }
  }

  public static class DirectoryStatistics_v1 extends DirectoryStatistics {
    @JsonProperty private String computed;
    @JsonProperty ("columns") private List<ColumnStatistics_v1> columnStatistics;
    // Default constructor required for deserializer
    public DirectoryStatistics_v1() { }
    @JsonGetter ("computed")
    public String getComputedTime() {
      return this.computed;
    }
    @JsonSetter ("computed")
    public void setComputedTime(String computed) {
      this.computed = computed;
    }
    @JsonGetter ("columns")
    public List<ColumnStatistics_v1> getColumnStatistics() {
      return this.columnStatistics;
    }
    @JsonSetter ("columns")
    public void setColumnStatistics(List<ColumnStatistics_v1> columnStatistics) {
      this.columnStatistics = columnStatistics;
    }
  }

  public static class ColumnStatistics_v1 extends ColumnStatistics {
    @JsonProperty ("column") private SchemaPath name = null;
    @JsonProperty ("majortype")   private TypeProtos.MajorType type = null;
    @JsonProperty ("schema") private long schema = 0;
    @JsonProperty ("rowcount") private long count = 0;
    @JsonProperty ("nonnullrowcount") private long nonNullCount = 0;
    @JsonProperty ("ndv") private long ndv = 0;
    @JsonProperty ("avgwidth") private double width = 0;
    @JsonProperty ("histogram") private Histogram histogram = null;

    public ColumnStatistics_v1() {}
    @JsonGetter ("column")
    public SchemaPath getName() { return this.name; }
    @JsonSetter ("column")
    public void setName(SchemaPath name) {
      this.name = name;
    }
    @JsonGetter ("majortype")
    public TypeProtos.MajorType getType() { return this.type; }
    @JsonSetter ("type")
    public void setType(TypeProtos.MajorType type) {
      this.type = type;
    }
    @JsonGetter ("schema")
    public long getSchema() {
      return this.schema;
    }
    @JsonSetter ("schema")
    public void setSchema(long schema) {
      this.schema = schema;
    }
    @JsonGetter ("rowcount")
    public double getCount() {
      return this.count;
    }
    @JsonSetter ("rowcount")
    public void setCount(long count) {
      this.count = count;
    }
    @JsonGetter ("nonnullrowcount")
    public double getNonNullCount() {
      return this.nonNullCount;
    }
    @JsonSetter ("nonnullrowcount")
    public void setNonNullCount(long nonNullCount) {
      this.nonNullCount = nonNullCount;
    }
    @JsonGetter ("ndv")
    public long getNdv() {
      return this.ndv;
    }
    @JsonSetter ("ndv")
    public void setNdv(long ndv) { this.ndv = ndv; }
    @JsonGetter ("avgwidth")
    public double getAvgWidth() {
      return this.width;
    }
    @JsonSetter ("avgwidth")
    public void setAvgWidth(double width) { this.width = width; }
    @JsonGetter("histogram")
    public Histogram getHistogram() { return this.histogram; }
    @JsonSetter("histogram")
    public void setHistogram(Histogram histogram) {
      this.histogram = histogram;
    }
    @JsonIgnore
    public void buildHistogram(byte[] tdigest_bytearray) {
      int num_buckets = (int) Math.min(ndv, DrillStatsTable.NUM_HISTOGRAM_BUCKETS);
      this.histogram = HistogramUtils.buildHistogramFromTDigest(tdigest_bytearray, this.getType(),
              num_buckets, nonNullCount);
    }
  }

  private TableStatistics readStatistics(DrillTable drillTable, Path path) throws IOException {
    final Object selection = drillTable.getSelection();

    if (selection instanceof FormatSelection) {
      StoragePlugin storagePlugin =  drillTable.getPlugin();
      FormatSelection formatSelection = (FormatSelection) selection;
      FormatPluginConfig formatConfig = formatSelection.getFormat();

      if (storagePlugin instanceof FileSystemPlugin
          && (formatConfig instanceof ParquetFormatConfig)) {
        FormatPlugin fmtPlugin = storagePlugin.getFormatPlugin(formatConfig);
        if (fmtPlugin.supportsStatistics()) {
          return fmtPlugin.readStatistics(fs, path);
        }
      }
    }
    return null;
  }

  public static TableStatistics generateDirectoryStructure(String dirComputedTime,
      List<ColumnStatistics> columnStatisticsList) {
    // TODO: Split up columnStatisticsList() based on directory names. We assume only
    // one directory right now but this WILL change in the future
    // HashMap<String, Boolean> dirNames = new HashMap<String, Boolean>();
    Statistics_v1 statistics = new Statistics_v1();
    List<DirectoryStatistics_v1> dirStats = new ArrayList<>();
    List<ColumnStatistics_v1> columnStatisticsV1s = new ArrayList<>();
    // Create dirStats
    DirectoryStatistics_v1 dirStat = new DirectoryStatistics_v1();
    // Add columnStats corresponding to this dirStats
    for (ColumnStatistics colStats : columnStatisticsList) {
      columnStatisticsV1s.add((ColumnStatistics_v1) colStats);
    }
    dirStat.setComputedTime(dirComputedTime);
    dirStat.setColumnStatistics(columnStatisticsV1s);
    // Add this dirStats to the list of dirStats
    dirStats.add(dirStat);
    // Add list of dirStats to tableStats
    statistics.setDirectoryStatistics(dirStats);
    return statistics;
  }

  public static PhysicalPlan direct(QueryContext context, boolean outcome, String message, Object... values) {
    return DirectPlan.createDirectPlan(context, outcome, String.format(message, values));
  }

  /* Helper function to generate error - statistics not supported on non-parquet tables */
  public static PhysicalPlan notSupported(QueryContext context, String tbl) {
    return direct(context, false, "Table %s is not supported by ANALYZE."
        + " Support is currently limited to directory-based Parquet tables.", tbl);
  }

  public static PhysicalPlan notRequired(QueryContext context, String tbl) {
    return direct(context, false, "Table %s has not changed since last ANALYZE!", tbl);
  }

  /**
   * This method returns the statistics (de)serializer which can be used to (de)/serialize the
   * {@link TableStatistics} from/to JSON
   */
  public static ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule deModule = new SimpleModule("StatisticsSerDeModule")
        .addSerializer(TypeProtos.MajorType.class, new MajorTypeSerDe.Se())
        .addDeserializer(TypeProtos.MajorType.class, new MajorTypeSerDe.De())
        .addDeserializer(SchemaPath.class, new SchemaPath.De());
    mapper.registerModule(deModule);
    mapper.registerSubtypes(new NamedType(NumericEquiDepthHistogram.class, "numeric-equi-depth"));
    return mapper;
  }

  /**
   * Returns list of {@link StatisticsKind} and statistics values obtained from specified {@link DrillStatsTable}.
   *
   * @param statsProvider the source of statistics
   * @return list of {@link StatisticsKind} and statistics values
   */
  public static List<StatisticsHolder<?>> getEstimatedTableStats(DrillStatsTable statsProvider) {
    if (statsProvider != null && statsProvider.isMaterialized()) {
      List<StatisticsHolder<?>> tableStatistics = Arrays.asList(
          new StatisticsHolder<>(statsProvider.getRowCount(), TableStatisticsKind.EST_ROW_COUNT),
          new StatisticsHolder<>(Boolean.TRUE, TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS));
      return tableStatistics;
    }
    return Collections.emptyList();
  }

  /**
   * Returns list of {@link StatisticsKind} and statistics values obtained from specified {@link DrillStatsTable} for specified column.
   *
   * @param statsProvider the source of statistics
   * @param fieldName     name of the columns whose statistics should be obtained
   * @return list of {@link StatisticsKind} and statistics values
   */
  public static List<StatisticsHolder<?>> getEstimatedColumnStats(DrillStatsTable statsProvider, SchemaPath fieldName) {
    if (statsProvider != null && statsProvider.isMaterialized()) {
      List<StatisticsHolder<?>> statisticsValues = new ArrayList<>();
      Double ndv = statsProvider.getNdv(fieldName);
      if (ndv != null) {
        statisticsValues.add(new StatisticsHolder<>(ndv, ColumnStatisticsKind.NDV));
      }
      Double nonNullCount = statsProvider.getNNRowCount(fieldName);
      if (nonNullCount != null) {
        statisticsValues.add(new StatisticsHolder<>(nonNullCount, ColumnStatisticsKind.NON_NULL_COUNT));
      }
      Histogram histogram = statsProvider.getHistogram(fieldName);
      if (histogram != null) {
        statisticsValues.add(new StatisticsHolder<>(histogram, ColumnStatisticsKind.HISTOGRAM));
      }
      Double rowcount = statsProvider.getRowCount();
      if (rowcount != null) {
        statisticsValues.add(new StatisticsHolder<>(rowcount, ColumnStatisticsKind.ROWCOUNT));
      }
      return statisticsValues;
    }
    return Collections.emptyList();
  }
}
