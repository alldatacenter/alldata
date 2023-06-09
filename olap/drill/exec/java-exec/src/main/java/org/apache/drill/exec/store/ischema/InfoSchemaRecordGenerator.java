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
package org.apache.drill.exec.store.ischema;

import org.apache.calcite.jdbc.DynamicRootSchema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.store.pojo.PojoRecordReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates records for POJO RecordReader by scanning the given schema. At every level (catalog, schema, table, field, partition, file),
 * level specific object is visited and decision is taken to visit the contents of the object. Object here is catalog,
 * schema, table, field, partition, file.
 */
public abstract class InfoSchemaRecordGenerator<S> {

  protected List<S> records = new ArrayList<>();

  private final FilterEvaluator filterEvaluator;
  private final List<RecordCollector> recordCollectors = new ArrayList<>();

  public InfoSchemaRecordGenerator(FilterEvaluator filterEvaluator) {
    this.filterEvaluator = filterEvaluator;
  }

  public void registerRecordCollector(RecordCollector recordCollector) {
    recordCollectors.add(recordCollector);
  }

  public void scanSchema(SchemaPlus root) {
    records = new ArrayList<>(); // reset on new scan
    if (filterEvaluator.shouldVisitCatalog()) {
      scanSchema(root.getName(), root);
    }
  }

  /**
   * Recursively scans the given schema, invoking the visitor as appropriate.
   * @param  schemaPath  the path to the given schema, so far
   * @param  schema  the given schema
   */
  protected void scanSchema(String schemaPath, SchemaPlus schema) {
    scanSchemaImpl(schemaPath, schema, new HashSet<>());
  }

  /**
   *  Recursively scan given schema and any sub-schema in it. In case when scan schema is root,
   *  set of visited paths is used to prevent visiting same sub-schema twice.
   *
   * @param schemaPath path to scan
   * @param schema schema associated with path
   * @param visitedPaths set used to ensure same path won't be visited twice
   */
  private void scanSchemaImpl(String schemaPath, SchemaPlus schema, Set<String> visitedPaths) {
    Set<String> subSchemaNames = schema.getParentSchema() == null
      ? schema.unwrap(DynamicRootSchema.class).schema.getSubSchemaNames()
      : schema.getSubSchemaNames();

    for (String name: subSchemaNames) {
      String subSchemaPath = schemaPath.isEmpty() ? name : schemaPath + "." + name;
      if (!filterEvaluator.shouldPruneSchema(subSchemaPath)) {
        scanSchemaImpl(subSchemaPath, schema.getSubSchema(name), visitedPaths);
      }
    }

    if (filterEvaluator.shouldVisitSchema(schemaPath, schema) && visitedPaths.add(schemaPath)) {
      visit(schemaPath, schema);
    }
  }

  protected final void visit(String schemaPath, SchemaPlus schema) {
    records.addAll(recordCollectors.parallelStream()
      .map(recordCollector -> collect(recordCollector, schemaPath, schema))
      .flatMap(Collection::stream)
      .collect(Collectors.toList()));
  }

  public abstract PojoRecordReader<S> getRecordReader();

  protected abstract List<S> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema);

  public static class Catalogs extends InfoSchemaRecordGenerator<Records.Catalog> {

    public Catalogs(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.Catalog> getRecordReader() {
      return new PojoRecordReader<>(Records.Catalog.class, records);
    }

    @Override
    protected List<Records.Catalog> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.catalogs(schemaPath, schema);
    }

    @Override
    protected void scanSchema(String schemaPath, SchemaPlus schema) {
      visit(schemaPath, schema);
    }
  }

  public static class Schemata extends InfoSchemaRecordGenerator<Records.Schema> {

    public Schemata(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.Schema> getRecordReader() {
      return new PojoRecordReader<>(Records.Schema.class, records);
    }

    @Override
    protected List<Records.Schema> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.schemas(schemaPath, schema);
    }
  }

  public static class Tables extends InfoSchemaRecordGenerator<Records.Table> {

    public Tables(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.Table> getRecordReader() {
      return new PojoRecordReader<>(Records.Table.class, records);
    }

    @Override
    protected List<Records.Table> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.tables(schemaPath, schema);
    }
  }

  public static class Views extends InfoSchemaRecordGenerator<Records.View> {

    public Views(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.View> getRecordReader() {
      return new PojoRecordReader<>(Records.View.class, records);
    }

    @Override
    protected List<Records.View> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.views(schemaPath, schema);
    }
  }

  public static class Columns extends InfoSchemaRecordGenerator<Records.Column> {

    public Columns(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.Column> getRecordReader() {
      return new PojoRecordReader<>(Records.Column.class, records);
    }

    @Override
    protected List<Records.Column> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.columns(schemaPath, schema);
    }
  }

  public static class Partitions extends InfoSchemaRecordGenerator<Records.Partition> {

    public Partitions(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.Partition> getRecordReader() {
      return new PojoRecordReader<>(Records.Partition.class, records);
    }

    @Override
    protected List<Records.Partition> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.partitions(schemaPath, schema);
    }
  }

  public static class Files extends InfoSchemaRecordGenerator<Records.File> {

    public Files(FilterEvaluator filterEvaluator) {
      super(filterEvaluator);
    }

    @Override
    public PojoRecordReader<Records.File> getRecordReader() {
      return new PojoRecordReader<>(Records.File.class, records);
    }

    @Override
    protected List<Records.File> collect(RecordCollector recordCollector, String schemaPath, SchemaPlus schema) {
      return recordCollector.files(schemaPath, schema);
    }
  }
}
