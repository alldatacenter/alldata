/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.helper;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyData;
import com.netease.arctic.io.writer.TaskWriterKey;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExpectResultHelper {

  public static List<Record> upsertResult(
      List<Record> target, List<Record> source, Function<Record, Object> keyExtractor) {
    Map<Object, Record> expects = Maps.newHashMap();
    target.forEach(r -> {
      Object key = keyExtractor.apply(r);
      expects.put(key, r);
    });

    source.forEach(r -> {
      Object key = keyExtractor.apply(r);
      expects.put(key, r);
    });
    return Lists.newArrayList(expects.values());
  }

  public static List<Record> upsertDeletes(
      List<Record> target, List<Record> source, Function<Record, Object> keyExtractor
  ) {
    Map<Object, Record> expects = Maps.newHashMap();
    Map<Object, Record> deletes = Maps.newHashMap();
    target.forEach(r -> {
      Object key = keyExtractor.apply(r);
      expects.put(key, r);
    });

    source.forEach(r -> {
      Object key = keyExtractor.apply(r);
      if (expects.containsKey(key)) {
        deletes.put(key, expects.get(key));
      }
    });
    return Lists.newArrayList(deletes.values());
  }

  public static List<Record> dynamicOverwriteResult(
      List<Record> target, List<Record> source, Function<Record, Object> partitionExtractor) {
    Map<Object, List<Record>> targetGroupByPartition = Maps.newHashMap();
    target.forEach(r -> {
      Object pt = partitionExtractor.apply(r);
      targetGroupByPartition.computeIfAbsent(pt, x -> Lists.newArrayList());
      targetGroupByPartition.get(pt).add(r);
    });

    source.forEach(r -> {
      Object pt = partitionExtractor.apply(r);
      targetGroupByPartition.remove(pt);
    });

    List<Record> expects = targetGroupByPartition.values().stream()
        .reduce(Lists.newArrayList(), (l, r) -> {
          l.addAll(r);
          return l;
        });
    expects.addAll(source);
    return expects;
  }

  public static int expectOptimizeWriteFileCount(List<Record> sources, ArcticTable table, int bucket) {
    PartitionKey partitionKey = new PartitionKey(table.spec(), table.schema());
    PrimaryKeyData primaryKey = null;
    if (table.isKeyedTable()) {
      primaryKey = new PrimaryKeyData(table.asKeyedTable().primaryKeySpec(), table.schema());
    }
    int mask = bucket - 1;
    Set<TaskWriterKey> writerKeys = Sets.newHashSet();

    for (Record row : sources) {
      partitionKey.partition(row);
      DataTreeNode node;
      if (primaryKey != null) {
        primaryKey.primaryKey(row);
        node = primaryKey.treeNode(mask);
      } else {
        node = DataTreeNode.ROOT;
      }
      writerKeys.add(
          new TaskWriterKey(partitionKey.copy(), node, DataFileType.BASE_FILE)
      );
    }
    return writerKeys.size();
  }

  public static MergeResult expectMergeResult(
      List<Record> target, List<Record> source, Function<Record, Object> keyExtractor) {
    return new MergeResult(target, source, keyExtractor);
  }

  public static class MergeResult {
    private List<Record> target;
    private List<Record> source;
    private Function<Record, Object> keyExtractor;

    private List<Pair<BiFunction<Record, Record, Boolean>, BiFunction<Record, Record, Record>>> matchActions
        = Lists.newArrayList();
    private List<Pair<Predicate<Record>, Function<Record, Record>>> notMatchedActions = Lists.newArrayList();

    protected MergeResult(List<Record> target, List<Record> source, Function<Record, Object> keyExtractor) {
      this.target = target;
      this.source = source;
      this.keyExtractor = keyExtractor;
    }

    /**
     * if condition(target, source) test for true. then apply action(target, source) to target records.
     *
     * @param condition - condition(target, source): bool
     * @param action    -> action(target, source): Record if return null, target will be deleted.
     * @return this.
     */
    public MergeResult whenMatched(
        BiFunction<Record, Record, Boolean> condition,
        BiFunction<Record, Record, Record> action) {
      this.matchActions.add(Pair.of(condition, action));
      return this;
    }

    /**
     * if condition(source) test for true, then action(source) will be added to target records
     *
     * @param condition condition(source): bool
     * @param action    action(source) for insert, result is not-null
     * @return this
     */
    public MergeResult whenNotMatched(Predicate<Record> condition, Function<Record, Record> action) {
      this.notMatchedActions.add(Pair.of(condition, action));
      return this;
    }

    public List<Record> results() {
      Map<Object, Record> targetMap = target.stream().collect(Collectors.toMap(keyExtractor, Function.identity()));
      Map<Object, Record> sourceMap = source.stream().collect(Collectors.toMap(keyExtractor, Function.identity()));

      for (Object key : sourceMap.keySet()) {
        Record source = sourceMap.get(key);
        Record target = targetMap.get(key);
        if (target != null) {
          matchActions.stream()
              .filter(a -> a.getLeft().apply(target, source))
              .map(Pair::getRight)
              .findFirst()
              .ifPresent(trans -> {
                Record r = trans.apply(target, source);
                if (r == null) {
                  targetMap.remove(key);
                } else {
                  targetMap.put(key, r);
                }
              });
        } else {
          notMatchedActions.stream()
              .filter(a -> a.getLeft().test(source))
              .map(Pair::getRight)
              .findFirst()
              .ifPresent(trans -> {
                targetMap.put(key, trans.apply(source));
              });
        }
      }

      return Lists.newArrayList(targetMap.values());
    }
  }
}
