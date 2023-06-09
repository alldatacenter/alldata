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
package org.apache.drill.exec.store.hive;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JacksonInject;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.commons.codec.binary.Base64;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.mapred.InputSplit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.io.ByteArrayDataInput;
import org.apache.drill.shaded.guava.com.google.common.io.ByteStreams;

@JsonTypeName("hive-sub-scan")
public class HiveSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "HIVE_SUB_SCAN";

  private final HiveReadEntry hiveReadEntry;
  private final List<List<InputSplit>> inputSplits = new ArrayList<>();
  private final HiveStoragePlugin hiveStoragePlugin;
  private final List<List<String>> splits;
  private final List<String> splitClasses;
  private final HiveTableWithColumnCache table;
  private final List<HivePartition> partitions;
  private final List<SchemaPath> columns;
  private final Map<String, String> confProperties;

  @JsonCreator
  public HiveSubScan(@JacksonInject StoragePluginRegistry registry,
                     @JsonProperty("userName") String userName,
                     @JsonProperty("splits") List<List<String>> splits,
                     @JsonProperty("hiveReadEntry") HiveReadEntry hiveReadEntry,
                     @JsonProperty("splitClasses") List<String> splitClasses,
                     @JsonProperty("columns") List<SchemaPath> columns,
                     @JsonProperty("hiveStoragePluginConfig") HiveStoragePluginConfig hiveStoragePluginConfig,
                     @JsonProperty("confProperties") Map<String, String> confProperties)
      throws IOException, ExecutionSetupException, ReflectiveOperationException {
    this(userName,
        splits,
        hiveReadEntry,
        splitClasses,
        columns,
        registry.resolve(hiveStoragePluginConfig, HiveStoragePlugin.class),
        confProperties);
  }

  public HiveSubScan(final String userName,
                     final List<List<String>> splits,
                     final HiveReadEntry hiveReadEntry,
                     final List<String> splitClasses,
                     final List<SchemaPath> columns,
                     final HiveStoragePlugin hiveStoragePlugin,
                     final Map<String, String> confProperties)
    throws IOException, ReflectiveOperationException {
    super(userName);
    this.hiveReadEntry = hiveReadEntry;
    this.table = hiveReadEntry.getTable();
    this.partitions = hiveReadEntry.getPartitions();
    this.splits = splits;
    this.splitClasses = splitClasses;
    this.columns = columns;
    this.hiveStoragePlugin = hiveStoragePlugin;
    this.confProperties = confProperties;

    for (int i = 0; i < splits.size(); i++) {
      inputSplits.add(deserializeInputSplit(splits.get(i), splitClasses.get(i)));
    }
  }

  @JsonProperty
  public List<List<String>> getSplits() {
    return splits;
  }

  @JsonProperty
  public HiveReadEntry getHiveReadEntry() {
    return hiveReadEntry;
  }

  @JsonProperty
  public List<String> getSplitClasses() {
    return splitClasses;
  }

  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public HiveStoragePluginConfig getHiveStoragePluginConfig() {
    return hiveStoragePlugin.getConfig();
  }

  @JsonProperty
  public Map<String, String> getConfProperties() {
    return confProperties;
  }

  @JsonIgnore
  public HiveTableWithColumnCache getTable() {
    return table;
  }

  @JsonIgnore
  public List<HivePartition> getPartitions() {
    return partitions;
  }

  @JsonIgnore
  public List<List<InputSplit>> getInputSplits() {
    return inputSplits;
  }

  @JsonIgnore
  public HiveStoragePlugin getStoragePlugin() {
    return hiveStoragePlugin;
  }

  @JsonIgnore
  public HiveConf getHiveConf() {
    return HiveUtilities.generateHiveConf(hiveStoragePlugin.getHiveConf(), confProperties);
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException {
    try {
      return new HiveSubScan(getUserName(), splits, hiveReadEntry, splitClasses, columns, hiveStoragePlugin, confProperties);
    } catch (IOException | ReflectiveOperationException e) {
      throw new ExecutionSetupException(e);
    }
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return ImmutableSet.<PhysicalOperator>of().iterator();
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  private static List<InputSplit> deserializeInputSplit(List<String> base64, String className)
      throws IOException, ReflectiveOperationException{
    Constructor<?> constructor = Class.forName(className).getDeclaredConstructor();
    if (constructor == null) {
      throw new ReflectiveOperationException("Class " + className + " does not implement a default constructor.");
    }
    constructor.setAccessible(true);
    List<InputSplit> splits = new ArrayList<>();
    for (String str : base64) {
      InputSplit split = (InputSplit) constructor.newInstance();
      ByteArrayDataInput byteArrayDataInput = ByteStreams.newDataInput(Base64.decodeBase64(str));
      split.readFields(byteArrayDataInput);
      splits.add(split);
    }
    return splits;
  }

}
