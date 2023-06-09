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
package org.apache.drill.exec.store.schedule;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.TimedCallable;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableRangeMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Range;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class BlockMapBuilder {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BlockMapBuilder.class);
  static final MetricRegistry metrics = DrillMetrics.getRegistry();
  static final String BLOCK_MAP_BUILDER_TIMER = MetricRegistry.name(BlockMapBuilder.class, "blockMapBuilderTimer");

  private final Map<Path,ImmutableRangeMap<Long,BlockLocation>> blockMapMap = Maps.newConcurrentMap();
  private final FileSystem fs;
  private final ImmutableMap<String,DrillbitEndpoint> endPointMap;
  private final CompressionCodecFactory codecFactory;

  public BlockMapBuilder(FileSystem fs, Collection<DrillbitEndpoint> endpoints) {
    this.fs = fs;
    this.codecFactory = new CompressionCodecFactory(fs.getConf());
    this.endPointMap = buildEndpointMap(endpoints);
  }

  private boolean compressed(FileStatus fileStatus) {
    return codecFactory.getCodec(fileStatus.getPath()) != null;
  }

  public List<CompleteFileWork> generateFileWork(List<FileStatus> files, boolean blockify) throws IOException {

    List<TimedCallable<List<CompleteFileWork>>> readers = new ArrayList<>(files.size());
    for(FileStatus status : files){
      readers.add(new BlockMapReader(status, blockify));
    }
    List<List<CompleteFileWork>> work = TimedCallable.run("Get block maps", logger, readers, 16);
    List<CompleteFileWork> singleList = Lists.newArrayList();
    for(List<CompleteFileWork> innerWorkList : work){
      singleList.addAll(innerWorkList);
    }

    return singleList;

  }

  private class BlockMapReader extends TimedCallable<List<CompleteFileWork>> {
    final FileStatus status;

    // This variable blockify indicates if a single file can be read by multiple threads
    // For examples, for CSV, it is set as true
    // because each row in a CSV file can be considered as an independent record;
    // for json, it is set as false
    // because each row in a json file cannot be determined as a record or not simply by that row alone
    final boolean blockify;

    public BlockMapReader(FileStatus status, boolean blockify) {
      super();
      this.status = status;
      this.blockify = blockify;
    }


    @Override
    protected List<CompleteFileWork> runInner() throws Exception {
      final List<CompleteFileWork> work = new ArrayList<>();

      final Set<String> noDrillbitHosts = logger.isDebugEnabled() ? new HashSet<>() : null;

      boolean error = false;
      if (blockify && !compressed(status)) {
        try {
          ImmutableRangeMap<Long, BlockLocation> rangeMap = getBlockMap(status);
          for (Entry<Range<Long>, BlockLocation> l : rangeMap.asMapOfRanges().entrySet()) {
            work.add(new CompleteFileWork(getEndpointByteMap(noDrillbitHosts, new FileStatusWork(status)),
                l.getValue().getOffset(), l.getValue().getLength(), status.getPath()));
          }
        } catch (IOException e) {
          logger.warn("failure while generating file work.", e);
          error = true;
        }
      }


      if (!blockify || error || compressed(status)) {
        work.add(new CompleteFileWork(getEndpointByteMap(noDrillbitHosts, new FileStatusWork(status)), 0,
            status.getLen(), status.getPath()));
      }

      // This if-condition is specific for empty CSV file
      // For CSV files, the global variable blockify is set as true
      // And if this CSV file is empty, rangeMap would be empty also
      // Therefore, at the point before this if-condition, work would not be populated
      if(work.isEmpty()) {
        work.add(new CompleteFileWork(getEndpointByteMap(noDrillbitHosts, new FileStatusWork(status)), 0, 0,
            status.getPath()));
      }

      if (noDrillbitHosts != null) {
        for (String host: noDrillbitHosts) {
          logger.debug("Failure finding Drillbit running on host {}. Skipping affinity to that host.", host);
        }
      }

      return work;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("path", status.getPath()).toString();
    }
  }

  private class FileStatusWork implements FileWork{
    private FileStatus status;

    public FileStatusWork(FileStatus status) {
      Preconditions.checkArgument(!status.isDir(), "FileStatus work only works with files, not directories.");
      this.status = status;
    }

    @Override
    public Path getPath() {
      return status.getPath();
    }

    @Override
    public long getStart() {
      return 0;
    }

    @Override
    public long getLength() {
      return status.getLen();
    }



  }

  private ImmutableRangeMap<Long,BlockLocation> buildBlockMap(Path path) throws IOException {
    FileStatus status = fs.getFileStatus(path);
    return buildBlockMap(status);
  }

  /**
   * Builds a mapping of block locations to file byte range
   */
  private ImmutableRangeMap<Long,BlockLocation> buildBlockMap(FileStatus status) throws IOException {
    final Timer.Context context = metrics.timer(BLOCK_MAP_BUILDER_TIMER).time();
    BlockLocation[] blocks;
    ImmutableRangeMap<Long,BlockLocation> blockMap;
    blocks = fs.getFileBlockLocations(status, 0, status.getLen());
    ImmutableRangeMap.Builder<Long, BlockLocation> blockMapBuilder = new ImmutableRangeMap.Builder<Long,BlockLocation>();
    for (BlockLocation block : blocks) {
      long start = block.getOffset();
      long end = start + block.getLength();
      Range<Long> range = Range.closedOpen(start, end);
      blockMapBuilder = blockMapBuilder.put(range, block);
    }
    blockMap = blockMapBuilder.build();
    blockMapMap.put(status.getPath(), blockMap);
    context.stop();
    return blockMap;
  }

  private ImmutableRangeMap<Long,BlockLocation> getBlockMap(Path path) throws IOException{
    ImmutableRangeMap<Long,BlockLocation> blockMap  = blockMapMap.get(path);
    if(blockMap == null) {
      blockMap = buildBlockMap(path);
    }
    return blockMap;
  }

  private ImmutableRangeMap<Long,BlockLocation> getBlockMap(FileStatus status) throws IOException{
    ImmutableRangeMap<Long,BlockLocation> blockMap  = blockMapMap.get(status.getPath());
    if (blockMap == null) {
      blockMap = buildBlockMap(status);
    }
    return blockMap;
  }


  /**
   * For a given FileWork, calculate how many bytes are available on each on drillbit endpoint
   *
   * @param work the FileWork to calculate endpoint bytes for
   * @throws IOException
   */
  public EndpointByteMap getEndpointByteMap(Set<String> noDrillbitHosts, FileWork work) throws IOException {
    Stopwatch watch = Stopwatch.createStarted();
    Path fileName = work.getPath();


    ImmutableRangeMap<Long, BlockLocation> blockMap = getBlockMap(fileName);
    EndpointByteMapImpl endpointByteMap = new EndpointByteMapImpl();
    long start = work.getStart();
    long end = start + work.getLength();
    Range<Long> rowGroupRange = Range.closedOpen(start, end);

    // Find submap of ranges that intersect with the rowGroup
    ImmutableRangeMap<Long, BlockLocation> subRangeMap = blockMap.subRangeMap(rowGroupRange);

    // Iterate through each block in this submap and get the host for the block location
    for (Map.Entry<Range<Long>, BlockLocation> block : subRangeMap.asMapOfRanges().entrySet()) {
      String[] hosts;
      Range<Long> blockRange = block.getKey();
      try {
        hosts = block.getValue().getHosts();
      } catch (IOException ioe) {
        throw new RuntimeException("Failed to get hosts for block location", ioe);
      }
      Range<Long> intersection = rowGroupRange.intersection(blockRange);
      long bytes = intersection.upperEndpoint() - intersection.lowerEndpoint();

      // For each host in the current block location, add the intersecting bytes to the corresponding endpoint
      for (String host : hosts) {
        DrillbitEndpoint endpoint = getDrillBitEndpoint(host);
        if (endpoint != null) {
          endpointByteMap.add(endpoint, bytes);
        } else if (noDrillbitHosts != null) {
          noDrillbitHosts.add(host);
        }
      }
    }

    logger.debug("FileWork group ({},{}) max bytes {}", work.getPath(), work.getStart(), endpointByteMap.getMaxBytes());

    logger.debug("Took {} ms to set endpoint bytes", watch.stop().elapsed(TimeUnit.MILLISECONDS));
    return endpointByteMap;
  }

  private DrillbitEndpoint getDrillBitEndpoint(String hostName) {
    return endPointMap.get(hostName);
  }

  /**
   * Builds a mapping of Drillbit endpoints to hostnames
   */
  private static ImmutableMap<String, DrillbitEndpoint> buildEndpointMap(Collection<DrillbitEndpoint> endpoints) {
    Stopwatch watch = Stopwatch.createStarted();
    HashMap<String, DrillbitEndpoint> endpointMap = Maps.newHashMap();
    for (DrillbitEndpoint d : endpoints) {
      String hostName = d.getAddress();
      endpointMap.put(hostName, d);
    }
    watch.stop();
    logger.debug("Took {} ms to build endpoint map", watch.elapsed(TimeUnit.MILLISECONDS));
    return ImmutableMap.copyOf(endpointMap);
  }
}
