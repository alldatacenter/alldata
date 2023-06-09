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
package org.apache.drill.exec.store.mapr.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractDbGroupScan;
import org.apache.calcite.rel.RelNode;
import org.apache.drill.exec.planner.index.IndexCollection;

import org.apache.drill.exec.planner.cost.PluginCost;
import org.apache.drill.exec.planner.index.IndexDiscover;
import org.apache.drill.exec.planner.index.IndexDiscoverFactory;
import org.apache.drill.exec.planner.index.MapRDBIndexDiscover;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.AbstractStoragePlugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public abstract class MapRDBGroupScan extends AbstractDbGroupScan {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MapRDBGroupScan.class);

  private static final Comparator<List<MapRDBSubScanSpec>> LIST_SIZE_COMPARATOR = Comparator.comparingInt(List::size);
  private static final Comparator<List<MapRDBSubScanSpec>> LIST_SIZE_COMPARATOR_REV = Collections.reverseOrder(LIST_SIZE_COMPARATOR);

  protected AbstractStoragePlugin storagePlugin;

  protected MapRDBFormatPlugin formatPlugin;

  protected MapRDBFormatPluginConfig formatPluginConfig;

  protected List<SchemaPath> columns;

  protected Map<Integer, List<MapRDBSubScanSpec>> endpointFragmentMapping;

  protected NavigableMap<TabletFragmentInfo, String> doNotAccessRegionsToScan;

  protected double costFactor = 1.0;

  private boolean filterPushedDown = false;

  private Stopwatch watch = Stopwatch.createUnstarted();

  private TableMetadataProvider metadataProvider;

  private TableMetadata tableMetadata;

  public MapRDBGroupScan(MapRDBGroupScan that) {
    super(that);
    this.columns = that.columns;
    this.formatPlugin = that.formatPlugin;
    this.formatPluginConfig = that.formatPluginConfig;
    this.storagePlugin = that.storagePlugin;
    this.filterPushedDown = that.filterPushedDown;
    this.costFactor = that.costFactor;
    /* this is the only place we access the field `doNotAccessRegionsToScan` directly
     * because we do not want the sub-scan spec for JSON tables to be calculated
     * during the copy-constructor
     */
    this.doNotAccessRegionsToScan = that.doNotAccessRegionsToScan;
    this.metadataProvider = that.metadataProvider;
  }

  public MapRDBGroupScan(AbstractStoragePlugin storagePlugin,
      MapRDBFormatPlugin formatPlugin, List<SchemaPath> columns, String userName,
      TableMetadataProvider metadataProvider) {
    super(userName);
    this.storagePlugin = storagePlugin;
    this.formatPlugin = formatPlugin;
    this.formatPluginConfig = formatPlugin.getConfig();
    this.columns = columns;
    this.metadataProvider = metadataProvider;
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    watch.reset();
    watch.start();
    Map<String, DrillbitEndpoint> endpointMap = new HashMap<>();
    for (DrillbitEndpoint ep : formatPlugin.getContext().getBits()) {
      endpointMap.put(ep.getAddress(), ep);
    }

    final Map<DrillbitEndpoint, EndpointAffinity> affinityMap = new HashMap<>();
    for (String serverName : getRegionsToScan().values()) {
      DrillbitEndpoint ep = endpointMap.get(serverName);
      if (ep != null) {
        EndpointAffinity affinity = affinityMap.get(ep);
        if (affinity == null) {
          affinityMap.put(ep, new EndpointAffinity(ep, 1));
        } else {
          affinity.addAffinity(1);
        }
      }
    }
    logger.debug("Took {} µs to get operator affinity", watch.elapsed(TimeUnit.NANOSECONDS)/1000);
    return Lists.newArrayList(affinityMap.values());
  }

  /**
   *
   * @param incomingEndpoints
   */
  @Override
  public void applyAssignments(List<DrillbitEndpoint> incomingEndpoints) {
    watch.reset();
    watch.start();

    final NavigableMap<TabletFragmentInfo, String> regionsToScan = getRegionsToScan();
    final int numSlots = incomingEndpoints.size();
    Preconditions.checkArgument(numSlots <= regionsToScan.size(),
        String.format("Incoming endpoints %d is greater than number of scan regions %d", numSlots, regionsToScan.size()));

    /*
     * Minimum/Maximum number of assignment per slot
     */
    final int minPerEndpointSlot = (int) Math.floor((double)regionsToScan.size() / numSlots);
    final int maxPerEndpointSlot = (int) Math.ceil((double)regionsToScan.size() / numSlots);

    /*
     * initialize (endpoint index => HBaseSubScanSpec list) map
     */
    endpointFragmentMapping = Maps.newHashMapWithExpectedSize(numSlots);

    /*
     * another map with endpoint (hostname => corresponding index list) in 'incomingEndpoints' list
     */
    Map<String, Queue<Integer>> endpointHostIndexListMap = Maps.newHashMap();

    /*
     * Initialize these two maps
     */
    for (int i = 0; i < numSlots; ++i) {
      endpointFragmentMapping.put(i, new ArrayList<MapRDBSubScanSpec>(maxPerEndpointSlot));
      String hostname = incomingEndpoints.get(i).getAddress();
      Queue<Integer> hostIndexQueue = endpointHostIndexListMap.get(hostname);
      if (hostIndexQueue == null) {
        hostIndexQueue = Lists.newLinkedList();
        endpointHostIndexListMap.put(hostname, hostIndexQueue);
      }
      hostIndexQueue.add(i);
    }

    Set<Entry<TabletFragmentInfo, String>> regionsToAssignSet = Sets.newLinkedHashSet(regionsToScan.entrySet());

    /*
     * First, we assign regions which are hosted on region servers running on drillbit endpoints
     */
    for (Iterator<Entry<TabletFragmentInfo, String>> regionsIterator = regionsToAssignSet.iterator(); regionsIterator.hasNext(); /*nothing*/) {
      Entry<TabletFragmentInfo, String> regionEntry = regionsIterator.next();
      /*
       * Test if there is a drillbit endpoint which is also an HBase RegionServer that hosts the current HBase region
       */
      Queue<Integer> endpointIndexlist = endpointHostIndexListMap.get(regionEntry.getValue());
      if (endpointIndexlist != null) {
        Integer slotIndex = endpointIndexlist.poll();
        List<MapRDBSubScanSpec> endpointSlotScanList = endpointFragmentMapping.get(slotIndex);
        MapRDBSubScanSpec subScanSpec = getSubScanSpec(regionEntry.getKey());
        endpointSlotScanList.add(subScanSpec);
        // add to the tail of the slot list, to add more later in round robin fashion
        endpointIndexlist.offer(slotIndex);
        // this region has been assigned
        regionsIterator.remove();
      }
    }

    /*
     * Build priority queues of slots, with ones which has tasks lesser than 'minPerEndpointSlot' and another which have more.
     */
    PriorityQueue<List<MapRDBSubScanSpec>> minHeap = new PriorityQueue<List<MapRDBSubScanSpec>>(numSlots, LIST_SIZE_COMPARATOR);
    PriorityQueue<List<MapRDBSubScanSpec>> maxHeap = new PriorityQueue<List<MapRDBSubScanSpec>>(numSlots, LIST_SIZE_COMPARATOR_REV);
    for(List<MapRDBSubScanSpec> listOfScan : endpointFragmentMapping.values()) {
      if (listOfScan.size() <= minPerEndpointSlot) {
        minHeap.offer(listOfScan);
      } else if (listOfScan.size() > minPerEndpointSlot){
        maxHeap.offer(listOfScan);
      }
    }

    /*
     * Now, let's process any regions which remain unassigned and assign them to slots with minimum number of assignments.
     */
    if (regionsToAssignSet.size() > 0) {
      for (Entry<TabletFragmentInfo, String> regionEntry : regionsToAssignSet) {
        List<MapRDBSubScanSpec> smallestList = minHeap.poll();
        MapRDBSubScanSpec subScanSpec = getSubScanSpec(regionEntry.getKey());
        smallestList.add(subScanSpec);
        if (smallestList.size() < maxPerEndpointSlot) {
          minHeap.offer(smallestList);
        }
      }
    }

    /*
     * While there are slots with lesser than 'minPerEndpointSlot' unit work, balance from those with more.
     */
    while(minHeap.peek() != null && minHeap.peek().size() < minPerEndpointSlot) {
      List<MapRDBSubScanSpec> smallestList = minHeap.poll();
      List<MapRDBSubScanSpec> largestList = maxHeap.poll();
      smallestList.add(largestList.remove(largestList.size() - 1));
      if (largestList.size() > minPerEndpointSlot) {
        maxHeap.offer(largestList);
      }
      if (smallestList.size() < minPerEndpointSlot) {
        minHeap.offer(smallestList);
      }
    }

    for (Entry<Integer, List<MapRDBSubScanSpec>> endpoint : endpointFragmentMapping.entrySet()) {
      Collections.sort(endpoint.getValue());
    }

    /* no slot should be empty at this point */
    assert (minHeap.peek() == null || minHeap.peek().size() > 0) : String.format(
        "Unable to assign tasks to some endpoints.\nEndpoints: %s.\nAssignment Map: %s.",
        incomingEndpoints, endpointFragmentMapping.toString());

    logger.debug("Built assignment map in {} µs.\nEndpoints: {}.\nAssignment Map: {}",
        watch.elapsed(TimeUnit.NANOSECONDS)/1000, incomingEndpoints, endpointFragmentMapping.toString());
  }

  @Override
  public int getMaxParallelizationWidth() {
    return getRegionsToScan().size();
  }

  @JsonIgnore
  public MapRDBFormatPlugin getFormatPlugin() {
    return formatPlugin;
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @JsonProperty("storage")
  public StoragePluginConfig getStorageConfig() {
    return storagePlugin.getConfig();
  }

  @JsonIgnore
  public AbstractStoragePlugin getStoragePlugin(){
    return storagePlugin;
  }

  @Override
  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @JsonIgnore
  public void setFilterPushedDown(boolean b) {
    this.filterPushedDown = true;
  }

  public String getIndexHint() { return this.formatPluginConfig.getIndex(); }

  @JsonIgnore
  @Override
  public boolean isFilterPushedDown() {
    return filterPushedDown;
  }

  protected abstract MapRDBSubScanSpec getSubScanSpec(TabletFragmentInfo key);

  public void setCostFactor(double sel) {
    this.costFactor = sel;
  }

  @Override
  public IndexCollection getSecondaryIndexCollection(RelNode scanRel) {
    IndexDiscover discover = IndexDiscoverFactory.getIndexDiscover(
        getStorageConfig(), this, scanRel, MapRDBIndexDiscover.class);

    if (discover == null) {
      logger.error("Null IndexDiscover was found for {}!", scanRel);
    }
    return discover.getTableIndex(getTableName());
  }

  @JsonIgnore
  public abstract String getTableName();

  @JsonIgnore
  public int getRowKeyOrdinal() {
    return 0;
  }

  protected NavigableMap<TabletFragmentInfo, String> getRegionsToScan() {
    return doNotAccessRegionsToScan;
  }

  protected void resetRegionsToScan() {
    this.doNotAccessRegionsToScan = null;
  }

  protected void setRegionsToScan(NavigableMap<TabletFragmentInfo, String> regionsToScan) {
    this.doNotAccessRegionsToScan = regionsToScan;
  }

  @Override
  public PluginCost getPluginCostModel() {
    return formatPlugin.getPluginCostModel();
  }

  @JsonProperty
  public TupleMetadata getSchema() {
    return getTableMetadata().getSchema();
  }

  @Override
  @JsonIgnore
  public TableMetadataProvider getMetadataProvider() {
    return metadataProvider;
  }

  @Override
  @JsonIgnore
  public TableMetadata getTableMetadata() {
    if (tableMetadata == null) {
      tableMetadata = metadataProvider.getTableMetadata();
    }
    return tableMetadata;
  }

}
