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
package org.apache.drill.exec.store;

import java.util.List;

/**
 * Exposes partition information to UDFs to allow queries to limit reading
 * partitions dynamically.
 *
 * In a Drill query, a specific partition can be read by simply
 * using a filter on a directory column. For example, if data is partitioned
 * by year and month using directory names, a particular year/month can be
 * read with the following query.
 *
 * <pre>
 * select * from dfs.my_workspace.data_directory where dir0 = '2014_01';
 * </pre>
 *
 * This assumes that below data_directory there are sub-directories with
 * years and month numbers as folder names, and data stored below them.
 *
 * This works in cases where the partition column is known, but the current
 * implementation does not allow the partition information itself to be queried.
 * An example of such behavior would be a query that should always return the
 * latest month of data, without having to be updated periodically.
 * While it is possible to write a query like the one below, it will be very
 * expensive, as this currently is materialized as a full table scan followed
 * by an aggregation on the partition dir0 column and finally a filter.
 *
 * <pre>
 * select * from dfs.my_workspace.data_directory where dir0 in
 *    (select MAX(dir0) from dfs.my_workspace.data_directory);
 * </pre>
 *
 * This interface allows the definition of a UDF to perform the sub-query
 * on the list of partitions. This UDF can be used at planning time to
 * prune out all of the unnecessary reads of the previous example.
 *
 * <pre>
 * select * from dfs.my_workspace.data_directory
 *    where dir0 = maxdir('dfs.my_workspace', 'data_directory');
 * </pre>
 *
 * Look at {@link org.apache.drill.exec.expr.fn.impl.DirectoryExplorers}
 * for examples of UDFs that use this interface to query against
 * partition information.
 */
public interface PartitionExplorer {
  /**
   * For the schema provided,
   * get a list of sub-partitions of a particular table and the partitions
   * specified by partition columns and values. Individual storage
   * plugins will assign specific meaning to the parameters and return
   * values.
   *
   * A return value of an empty list should be given if the partition has
   * no sub-partitions.
   *
   * Note this does cause a collision between empty partitions and leaf partitions,
   * the interface should be modified if the distinction is meaningful.
   *
   * Example: for a filesystem plugin the partition information can be simply
   * be a path from the root of the given workspace to the desired directory. The
   * return value should be defined as a list of full paths (again from the root
   * of the workspace), which can be passed by into this interface to explore
   * partitions further down. An empty list would be returned if the partition
   * provided was a file, or an empty directory.
   *
   * Note to future devs, keep this doc in sync with
   * {@link SchemaPartitionExplorer}.
   *
   * @param schema schema path, can be complete or relative to the default schema
   * @param partitionColumns a list of partitions to match
   * @param partitionValues list of values of each partition (corresponding
   *                        to the partition column list)
   * @return list of sub-partitions, will be empty if a there is no further
   *         level of sub-partitioning below, i.e. hit a leaf partition
   * @throws PartitionNotFoundException when the partition does not exist in
   *          the given workspace
   */
  Iterable<String> getSubPartitions(String schema,
                                    String table,
                                    List<String> partitionColumns,
                                    List<String> partitionValues)
      throws PartitionNotFoundException;
}
