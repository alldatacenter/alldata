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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker.FileImplicitMarker;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker.InternalColumnMarker;
import org.apache.drill.exec.physical.impl.scan.v3.file.ImplicitColumnMarker.PartitionColumnMarker;
import org.apache.drill.exec.physical.impl.scan.v3.schema.MutableTupleSchema;
import org.apache.drill.exec.physical.impl.scan.v3.schema.MutableTupleSchema.ColumnHandle;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ProjectedColumn;
import org.apache.drill.exec.physical.impl.scan.v3.schema.ScanSchemaTracker;
import org.apache.drill.exec.physical.impl.scan.v3.schema.SchemaUtils;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitFileColumn;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitFileColumns;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitInternalFileColumns;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the resolution of implicit file metadata and partition columns.
 * Parses the file metadata columns from the projection list. Creates a parse
 * result which drives loading data into vectors. Supports renaming the columns
 * via session options.
 * <p>
 * Lifecycle:
 * <ul>
 * <li>At the start of the scan, the parser looks for implicit and partition
 * columns in the scan schema, resolving matching columns to be implicit
 * columns, and building up a description of those columns for use later.</li>
 * <li>If the projection list contains a wildcard, it can also contain implicit
 * columns. If running in legacy mode, inserts partition columns when the query
 * contains a wildcard.</li>
 * <li>On each file (on each reader), the parse result allows generating actual
 * values for each column, which are then written into the corresponding value
 * vectors.</li>
 * </ul>
 * <p>
 * Assumes that the partition count is fixed at runtime; that it is determined
 * at plan time and provided in the plan. This ensures that the schema is stable
 * across readers: even a reader at the top-most partition will produce columns
 * for all partitions if using legacy mode wildcard expansion.
 */
public class ImplicitColumnResolver {
  private static final Logger logger = LoggerFactory.getLogger(ImplicitColumnResolver.class);

  public static class ImplicitColumnOptions {

    protected OptionSet optionSet;

    protected int maxPartitionDepth;

    /**
     * Historically Drill will expand partition columns (dir0, dir1, ...)
     * when the project list includes a wildcard.
     */
    protected boolean useLegacyWildcardExpansion = true;

    protected DrillFileSystem dfs;

    public ImplicitColumnOptions optionSet(OptionSet optionSet) {
      this.optionSet = optionSet;
      return this;
   }

   /**
    * The maximum partition depth for any file in this query. Specifies
    * the maximum number of {@code diri} columns that this parser will
    * recognize or generate.
    */
   public ImplicitColumnOptions maxPartitionDepth(int maxPartitionDepth) {
     this.maxPartitionDepth = maxPartitionDepth;
     return this;
   }

    /**
     * Indicates whether to expand partition columns when the query contains a wildcard.
     * Supports queries such as the following:<code><pre>
     * select * from dfs.`partitioned-dir`</pre></code>
     * In which the output columns will be (columns, dir0) if the partitioned directory
     * has one level of nesting.
     *
     * See {@link TestImplicitFileColumns#testImplicitColumns}
     */
    public ImplicitColumnOptions useLegacyWildcardExpansion(boolean flag) {
      useLegacyWildcardExpansion = flag;
      return this;
    }

    public ImplicitColumnOptions dfs(DrillFileSystem dfs) {
      this.dfs = dfs;
      return this;
    }
  }

  /**
   * The result of scanning the scan output schema to identify implicit and
   * partition columns. Defines a sub-schema of just these columns, along with
   * column markers which resolve the columns for each file.
   */
  public static class ParseResult {
    private final List<ImplicitColumnMarker> columns;
    private final TupleMetadata schema;
    private final boolean isMetadataScan;

    protected ParseResult(List<ImplicitColumnMarker> columns, TupleMetadata schema,
        boolean isMetadataScan) {
      ImplicitColumnMarker reordered[] = new ImplicitColumnMarker[columns.size()];
      for (ImplicitColumnMarker col : columns) {
        reordered[col.index()] = col;
      }
      this.columns = Arrays.asList(reordered);
      this.schema = schema;
      this.isMetadataScan = isMetadataScan;
    }

    public TupleMetadata schema() { return schema; }
    public List<ImplicitColumnMarker> columns() { return columns; }
    public boolean isMetadataScan() { return isMetadataScan; }

    public Object[] resolve(FileDescrip fileInfo) {
      Object values[] = new Object[columns.size()];
      for (int i = 0; i < values.length; i++) {
        values[i] = columns.get(i).resolve(fileInfo);
      }
      return values;
    }
  }

  private static class ImplicitColumnParser {
    private final ImplicitColumnResolver parser;
    private final ScanSchemaTracker tracker;
    private final MutableTupleSchema scanSchema;
    private final List<ImplicitColumnMarker> columns = new ArrayList<>();
    private final Set<Integer> referencedPartitions = new HashSet<>();
    private boolean isMetadataScan;

    protected ImplicitColumnParser(ImplicitColumnResolver parser, ScanSchemaTracker tracker) {
      this.parser = parser;
      this.tracker = tracker;
      this.scanSchema = tracker.internalSchema();
    }

    protected ParseResult parse() {
      for (ColumnHandle col : tracker.internalSchema().columns()) {
        matchColumn(col);
      }
      if (tracker.internalSchema().projectionType() == ScanSchemaTracker.ProjectionType.ALL) {
        expandWildcard();
      }

      // Have the tracker gather the implicit columns so they appear
      // in the same order as the output schema, even if a wildcard
      // appears out-of-order:
      // SELECT *, fileName
      // SELECT fileName, *
      return new ParseResult(columns, tracker.applyImplicitCols(), isMetadataScan);
    }

    private void expandWildcard() {
      if (!parser.useLegacyWildcardExpansion) {
        return;
      }

      // Legacy wildcard expansion: include the file partitions for this file.
      // This is a disadvantage for a * query: files at different directory
      // levels will have different numbers of columns. Would be better to
      // return this data as an array at some point.
      // Append this after the *, keeping the * for later expansion.
      for (int i = 0; i < parser.maxPartitionDepth; i++) {
        if (referencedPartitions.contains(i)) {
          continue;
        }
        ImplicitColumnMarker marker = new PartitionColumnMarker(i);
        ColumnMetadata resolved = MetadataUtils.newScalar(parser.partitionName(i), PARTITION_COL_TYPE);
        columns.add(marker);
        tracker.expandImplicitCol(resolved, marker);
        referencedPartitions.add(i);
      }
    }

    private void matchColumn(ColumnHandle col) {
      String colType = SchemaUtils.implicitColType(col.column());
      if (colType != null) {
        resolveTaggedColumn(parser, col, colType);
        return;
      } else if (col.column().isDynamic()) {
        matchByName(col);
      }
    }

    private void resolveTaggedColumn(ImplicitColumnResolver parser,
        ColumnHandle col, String colType) {
      Matcher m = parser.partitionTypePattern.matcher(colType);
      if (m.matches()) {
        resolvePartitionColumn(m, parser, col);
        return;
      }

      ImplicitFileColumn defn = parser.typeDefs.get(colType);
      if (defn != null) {
        resolveImplicitColumn((ImplicitFileColumns) defn, col, colType);
        return;
      }
      resolveUnknownColumn(col, colType);
    }

    private void resolvePartitionColumn(Matcher m, ImplicitColumnResolver parser,
        ColumnHandle col) {

      // The provided schema column must be of the correct type and mode.
      ColumnMetadata colSchema = col.column();
      if (colSchema.type() != MinorType.VARCHAR ||
          colSchema.mode() != DataMode.OPTIONAL) {
        throw UserException.validationError()
            .message("Provided column `%s` is marked as a parition column, but is of the wrong type",
                colSchema.columnString())
            .addContext("Expected type", MinorType.VARCHAR.name())
            .addContext("Expected cardinality", DataMode.OPTIONAL.name())
            .addContext(parser.errorContext)
            .build(logger);
      }

      // Partition column
      int partitionIndex = Integer.parseInt(m.group(1));
      markImplicit(col, new PartitionColumnMarker(partitionIndex));

      // Remember the partition for later wildcard expansion
      referencedPartitions.add(partitionIndex);
    }

    private void resolveImplicitColumn(ImplicitFileColumns defn,
        ColumnHandle col, String colType) {

      // The provided schema column must be of the correct type and mode.
      ColumnMetadata colSchema = col.column();
      if (colSchema.type() != MinorType.VARCHAR ||
          colSchema.mode() == DataMode.REPEATED) {
        throw UserException.validationError()
            .message("Provided column `%s` is marked as implicit '%s', but is of the wrong type",
                colSchema.columnString(), defn.propertyValue())
            .addContext("Expected type", MinorType.VARCHAR.name())
            .addContext("Expected cardinality", String.format("%s or %s",
                DataMode.REQUIRED.name(), DataMode.OPTIONAL.name()))
            .addContext(parser.errorContext)
            .build(logger);
      }
      markImplicit(col, new FileImplicitMarker(defn));
    }

    private void markImplicit(ColumnHandle col, ImplicitColumnMarker marker) {
      columns.add(marker);
      col.markImplicit(marker);
    }

    private void resolveUnknownColumn(ColumnHandle col, String colType) {
      throw UserException.validationError()
          .message("Provided column %s references an undefined implicit column type '%s'",
              col.column().columnString(), colType)
          .addContext("Expected type", MinorType.VARCHAR.name())
          .addContext("Expected cardinality", String.format("%s or %s",
              DataMode.REQUIRED.name(), DataMode.OPTIONAL.name()))
          .addContext(parser.errorContext)
          .build(logger);
    }

    private void matchByName(ColumnHandle col) {
      Matcher m = parser.partitionPattern.matcher(col.name());
      if (m.matches()) {
        buildPartitionColumn(m, col);
        return;
      }

      ImplicitFileColumn defn = parser.colDefs.get(col.name());
      if (defn != null) {
        buildImplicitColumn(defn, col);
      }
    }

    private void buildPartitionColumn(Matcher m, ColumnHandle col) {

      // If the projected column is a map or array, then it shadows the
      // partition column. Example: dir0.x, dir0[2].
      ProjectedColumn projCol = (ProjectedColumn) col.column();
      if (!projCol.isSimple()) {
        logger.warn("Projected column {} shadows partition column {}",
            projCol.projectString(), col.name());
        return;
      }

      // Partition column
      int partitionIndex = Integer.parseInt(m.group(1));
      resolve(col,
          MetadataUtils.newScalar(col.name(), PARTITION_COL_TYPE),
          new PartitionColumnMarker(partitionIndex));

      // Remember the partition for later wildcard expansion
      referencedPartitions.add(partitionIndex);
    }

    private void resolve(ColumnHandle col, ColumnMetadata resolved, ImplicitColumnMarker marker) {
      columns.add(marker);
      scanSchema.resolveImplicit(col, resolved, marker);
    }

    private void buildImplicitColumn(ImplicitFileColumn defn,
        ColumnHandle col) {

      // If the projected column is a map or array, then it shadows the
      // metadata column. Example: filename.x, filename[2].
      ProjectedColumn projCol = (ProjectedColumn) col.column();
      if (!projCol.isSimple()) {
        logger.warn("Projected column {} shadows implicit column {}",
            projCol.projectString(), col.name());
      } else if (defn instanceof ImplicitInternalFileColumns) {
        resolveInternalColumn(col, (ImplicitInternalFileColumns) defn);
      } else {
        resolve(col,
            MetadataUtils.newScalar(col.name(), IMPLICIT_COL_TYPE),
            new FileImplicitMarker((ImplicitFileColumns) defn));
      }
    }

    private void resolveInternalColumn(ColumnHandle col,
        ImplicitInternalFileColumns defn) {

      // Tests may not provide the DFS, real code must
      if (defn == ImplicitInternalFileColumns.LAST_MODIFIED_TIME &&
          parser.dfs == null) {
        throw new IllegalStateException(
            "Must provide a file system to use " + defn.name());
      }

      // Check if this is an implied metadata scan
      if (defn == ImplicitInternalFileColumns.PROJECT_METADATA) {
        isMetadataScan = true;
      }

      // TODO: Internal columns are VARCHAR for historical reasons.
      // Better to use a type that fits the column purposes.
      resolve(col,
          MetadataUtils.newScalar(col.name(),
              defn.isOptional() ? OPTIONAL_INTERNAL_COL_TYPE : IMPLICIT_COL_TYPE),
          new InternalColumnMarker(defn));
    }
  }

  public static final MajorType IMPLICIT_COL_TYPE = Types.required(MinorType.VARCHAR);
  public static final MajorType PARTITION_COL_TYPE =  Types.optional(MinorType.VARCHAR);
  public static final MajorType OPTIONAL_INTERNAL_COL_TYPE =  Types.optional(MinorType.VARCHAR);

  private final int maxPartitionDepth;
  private final boolean useLegacyWildcardExpansion;
  private final String partitionDesignator;
  private final Pattern partitionPattern;
  private final Pattern partitionTypePattern;
  private final Map<String, ImplicitFileColumn> colDefs = CaseInsensitiveMap.newHashMap();
  private final Map<String, ImplicitFileColumn> typeDefs = CaseInsensitiveMap.newHashMap();
  private final CustomErrorContext errorContext;
  private final DrillFileSystem dfs;

  public ImplicitColumnResolver(ImplicitColumnOptions options, CustomErrorContext errorContext) {
    this.errorContext = errorContext;
    this.maxPartitionDepth = options.maxPartitionDepth;
    this.useLegacyWildcardExpansion = options.useLegacyWildcardExpansion;
    this.dfs = options.dfs;
    this.partitionDesignator = options.optionSet.getString(ExecConstants.FILESYSTEM_PARTITION_COLUMN_LABEL);
    this.partitionPattern = Pattern.compile(partitionDesignator + "(\\d+)", Pattern.CASE_INSENSITIVE);
    if (partitionDesignator.equals(ColumnMetadata.IMPLICIT_PARTITION_PREFIX)) {
      this.partitionTypePattern = partitionPattern;
    } else {
      this.partitionTypePattern = Pattern.compile(ColumnMetadata.IMPLICIT_PARTITION_PREFIX + "(\\d+)",
          Pattern.CASE_INSENSITIVE);
    }

    // File implicit columns: can be defined in the provided schema
    for (ImplicitFileColumns defn : ImplicitFileColumns.values()) {
      String colName = options.optionSet.getString(defn.optionName());
      if (!Strings.isNullOrEmpty(colName)) {
        this.colDefs.put(colName, defn);
      }
      this.typeDefs.put(defn.propertyValue(), defn);
    }

    // Internal implicit cols: cannot be defined in the provided schema
    for (ImplicitInternalFileColumns defn : ImplicitInternalFileColumns.values()) {
      String colName = options.optionSet.getString(defn.optionName());
      if (!Strings.isNullOrEmpty(colName)) {
        this.colDefs.put(colName, defn);
      }
    }
  }

  public ParseResult parse(ScanSchemaTracker tracker) {
    return new ImplicitColumnParser(this, tracker).parse();
  }

  public String partitionName(int partition) {
    return partitionDesignator + partition;
  }
}
