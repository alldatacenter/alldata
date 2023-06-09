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
package org.apache.drill.exec.store.iceberg;

import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.TimedCallable;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.drill.exec.store.schedule.EndpointByteMapImpl;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableRangeMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Range;
import org.apache.drill.shaded.guava.com.google.common.collect.RangeMap;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.BaseCombinedScanTask;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.FileScanTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IcebergBlockMapBuilder {

  private static final Logger logger = LoggerFactory.getLogger(IcebergBlockMapBuilder.class);

  private final Map<FileScanTask, RangeMap<Long, BlockLocation>> blockMapMap;

  private final Map<String, DrillbitEndpoint> endPointMap;

  private final FileSystem fs;

  public IcebergBlockMapBuilder(FileSystem fs, Collection<DrillbitEndpoint> endpoints) {
    this.fs = fs;
    this.blockMapMap = new ConcurrentHashMap<>();
    this.endPointMap = endpoints.stream()
      .collect(Collectors.toMap(DrillbitEndpoint::getAddress, endpoint -> endpoint, (a, b) -> b));
  }

  public List<IcebergCompleteWork> generateFileWork(Iterable<CombinedScanTask> combinedScanTasks) throws IOException {
    List<TimedCallable<IcebergCompleteWork>> readers = new ArrayList<>();
    combinedScanTasks.forEach(task -> readers.add(new BlockMapReader(task)));

    if (readers.isEmpty()) {
      readers.add(new BlockMapReader(new BaseCombinedScanTask()));
    }

    return TimedCallable.run("Get block maps", logger, readers, 16);
  }

  /**
   * Builds a mapping of block locations to file byte range
   */
  private RangeMap<Long, BlockLocation> buildBlockMap(FileScanTask work) throws IOException {
    BlockLocation[] blocks = fs.getFileBlockLocations(
      new Path(work.file().path().toString()), work.start(), work.length());
    ImmutableRangeMap.Builder<Long, BlockLocation> blockMapBuilder = new ImmutableRangeMap.Builder<>();
    for (BlockLocation block : blocks) {
      long start = block.getOffset();
      long end = start + block.getLength();
      Range<Long> range = Range.closedOpen(start, end);
      blockMapBuilder.put(range, block);
    }
    RangeMap<Long, BlockLocation> blockMap = blockMapBuilder.build();
    blockMapMap.put(work, blockMap);
    return blockMap;
  }

  private RangeMap<Long, BlockLocation> getBlockMap(FileScanTask work) throws IOException {
    RangeMap<Long, BlockLocation> blockMap = blockMapMap.get(work);
    if (blockMap == null) {
      blockMap = buildBlockMap(work);
    }
    return blockMap;
  }

  /**
   * For a given FileWork, calculate how many bytes are available on each on drillbit endpoint
   *
   * @param scanTask the CombinedScanTask to calculate endpoint bytes for
   */
  public EndpointByteMap getEndpointByteMap(CombinedScanTask scanTask) throws IOException {
    EndpointByteMapImpl endpointByteMap = new EndpointByteMapImpl();
    for (FileScanTask work : scanTask.files()) {
      RangeMap<Long, BlockLocation> blockMap = getBlockMap(work);
      long start = work.start();
      long end = start + work.length();
      Range<Long> scanTaskRange = Range.closedOpen(start, end);

      // Find sub-map of ranges that intersect with the scan task
      RangeMap<Long, BlockLocation> subRangeMap = blockMap.subRangeMap(scanTaskRange);

      // Iterate through each block in this sub-map and get the host for the block location
      for (Entry<Range<Long>, BlockLocation> block : subRangeMap.asMapOfRanges().entrySet()) {
        Range<Long> intersection = scanTaskRange.intersection(block.getKey());
        long bytes = intersection.upperEndpoint() - intersection.lowerEndpoint();

        // For each host in the current block location, add the intersecting bytes to the corresponding endpoint
        for (String host : block.getValue().getHosts()) {
          DrillbitEndpoint endpoint = endPointMap.get(host);
          if (endpoint != null) {
            endpointByteMap.add(endpoint, bytes);
          }
        }
      }

      logger.debug("FileScanTask group ({},{}) max bytes {}", work.file().path(), work.start(), endpointByteMap.getMaxBytes());
    }
    return endpointByteMap;
  }

  private class BlockMapReader extends TimedCallable<IcebergCompleteWork> {
    private final CombinedScanTask scanTask;

    public BlockMapReader(CombinedScanTask scanTask) {
      this.scanTask = scanTask;
    }

    @Override
    protected IcebergCompleteWork runInner() throws Exception {
      return new IcebergCompleteWork(getEndpointByteMap(scanTask), scanTask);
    }
  }

}
