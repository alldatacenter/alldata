/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.op;

import org.apache.iceberg.DataOperations;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.ManifestFile;
import org.apache.iceberg.PublicSnapshotProducer;
import org.apache.iceberg.SnapshotSummary;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableOperations;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Can not confirm the consistency of data, just forced delete manifest.
 */
public class ForcedDeleteManifests extends PublicSnapshotProducer<ForcedDeleteManifests> {

  private static final String DELETE_MANIFEST_COUNT = "manifests_deleted";

  private final SnapshotSummary.Builder summaryBuilder = SnapshotSummary.builder();

  private Set<ManifestFile> deleteManifests = new HashSet<>();

  protected ForcedDeleteManifests(TableOperations ops) {
    super(ops);
  }

  public static ForcedDeleteManifests of(Table table) {
    if (table instanceof HasTableOperations) {
      return new ForcedDeleteManifests(((HasTableOperations)table).operations());
    }
    throw new IllegalArgumentException("only support HasTableOperations table");
  }

  public void deleteManifest(ManifestFile manifestFile) {
    deleteManifests.add(manifestFile);
  }

  @Override
  protected ForcedDeleteManifests self() {
    return this;
  }

  @Override
  protected void cleanUncommitted(Set<ManifestFile> committed) {
  }

  @Override
  protected String operation() {
    return DataOperations.DELETE;
  }

  @Override
  protected List<ManifestFile> apply(TableMetadata metadataToUpdate) {
    return metadataToUpdate.currentSnapshot().allManifests()
        .stream().filter(s -> !deleteManifests.contains(s))
        .collect(Collectors.toList());
  }

  @Override
  protected Map<String, String> summary() {
    summaryBuilder.set(DELETE_MANIFEST_COUNT, String.valueOf(deleteManifests.size()));
    return null;
  }

  @Override
  public ForcedDeleteManifests set(String property, String value) {
    summaryBuilder.set(property, value);
    return this;
  }
}
