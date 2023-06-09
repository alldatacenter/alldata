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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.calcite.plan.volcano.RelSubset;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinInfo;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.planner.physical.BroadcastExchangePrel;
import org.apache.drill.exec.planner.physical.ExchangePrel;
import org.apache.drill.exec.planner.physical.HashAggPrel;
import org.apache.drill.exec.planner.physical.HashJoinPrel;
import org.apache.drill.exec.planner.physical.JoinPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.RuntimeFilterPrel;
import org.apache.drill.exec.planner.physical.ScanPrel;
import org.apache.drill.exec.planner.physical.SortPrel;
import org.apache.drill.exec.planner.physical.StreamAggPrel;
import org.apache.drill.exec.planner.physical.TopNPrel;
import org.apache.drill.exec.work.filter.BloomFilter;
import org.apache.drill.exec.work.filter.BloomFilterDef;
import org.apache.drill.exec.work.filter.RuntimeFilterDef;
import org.apache.drill.shaded.guava.com.google.common.collect.HashMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This visitor does two major things:
 * 1) Find the possible HashJoinPrel to add a RuntimeFilterDef to it.
 * 2) Generate a RuntimeFilterPrel over the corresponding probe side ScanPrel.
 */
public class RuntimeFilterVisitor extends BasePrelVisitor<Prel, Void, RuntimeException> {

  private final Set<ScanPrel> toAddRuntimeFilter = new HashSet<>();

  private final Multimap<ScanPrel, HashJoinPrel> probeSideScan2hj = HashMultimap.create();

  private final double fpp;

  private final int bloomFilterMaxSizeInBytesDef;

  private static final AtomicLong rfIdCounter = new AtomicLong();

  private RuntimeFilterVisitor(QueryContext queryContext) {
    this.bloomFilterMaxSizeInBytesDef = queryContext.getOption(ExecConstants.HASHJOIN_BLOOM_FILTER_MAX_SIZE_KEY).num_val.intValue();
    this.fpp = queryContext.getOption(ExecConstants.HASHJOIN_BLOOM_FILTER_FPP_KEY).float_val;
  }

  public static Prel addRuntimeFilter(Prel prel, QueryContext queryContext) {
    RuntimeFilterVisitor instance = new RuntimeFilterVisitor(queryContext);
    Prel finalPrel = prel.accept(instance, null);

    RuntimeFilterInfoPaddingHelper runtimeFilterInfoPaddingHelper = new RuntimeFilterInfoPaddingHelper();
    runtimeFilterInfoPaddingHelper.visitPrel(finalPrel, null);
    return finalPrel;
  }

  public Prel visitPrel(Prel prel, Void value) throws RuntimeException {
    List<RelNode> children = new ArrayList<>();
    for (Prel child : prel) {
      child = child.accept(this, value);
      children.add(child);
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }


  @Override
  public Prel visitJoin(JoinPrel prel, Void value) throws RuntimeException {
    if (prel instanceof HashJoinPrel) {
      HashJoinPrel hashJoinPrel = (HashJoinPrel) prel;
      //Generate possible RuntimeFilterDef to the HashJoinPrel, identify the corresponding
      //probe side ScanPrel.
      RuntimeFilterDef runtimeFilterDef = generateRuntimeFilter(hashJoinPrel);
      hashJoinPrel.setRuntimeFilterDef(runtimeFilterDef);
    }
    return visitPrel(prel, value);
  }

  @Override
  public Prel visitScan(ScanPrel prel, Void value) throws RuntimeException {
    if (toAddRuntimeFilter.contains(prel)) {
      //Spawn a fresh RuntimeFilterPrel over the previous identified probe side scan node or a runtime filter node.
      Collection<HashJoinPrel> hashJoinPrels = probeSideScan2hj.get(prel);
      RuntimeFilterPrel runtimeFilterPrel = null;
      for (HashJoinPrel hashJoinPrel : hashJoinPrels) {
        long identifier = rfIdCounter.incrementAndGet();
        hashJoinPrel.getRuntimeFilterDef().setRuntimeFilterIdentifier(identifier);
        if (runtimeFilterPrel == null) {
          runtimeFilterPrel = new RuntimeFilterPrel(prel, identifier);
        } else {
          runtimeFilterPrel = new RuntimeFilterPrel(runtimeFilterPrel, identifier);
        }
      }
      return runtimeFilterPrel;
    } else {
      return prel;
    }
  }


  /**
   * Generate a possible RuntimeFilter of a HashJoinPrel, left some BF parameters of the generated RuntimeFilter
   * to be set later.
   *
   * @param hashJoinPrel
   * @return null or a partial information RuntimeFilterDef
   */
  private RuntimeFilterDef generateRuntimeFilter(HashJoinPrel hashJoinPrel) {
    JoinRelType joinRelType = hashJoinPrel.getJoinType();
    JoinInfo joinInfo = hashJoinPrel.analyzeCondition();
    boolean allowJoin = (joinInfo.isEqui()) && (joinRelType == JoinRelType.INNER || joinRelType == JoinRelType.RIGHT);
    if (!allowJoin) {
      return null;
    }
    //TODO check whether to enable RuntimeFilter according to the NDV percent
    /**
     double threshold = 0.5;
     double percent = leftNDV / rightDNV;
     if (percent > threshold ) {
     return null;
     }
     */

    List<BloomFilterDef> bloomFilterDefs = new ArrayList<>();
    //find the possible left scan node of the left join key
    ScanPrel probeSideScanPrel = null;
    RelNode left = hashJoinPrel.getLeft();
    RelNode right = hashJoinPrel.getRight();
    ExchangePrel exchangePrel = findRightExchangePrel(right);
    if (exchangePrel == null) {
      //Does not support the single fragment mode ,that is the right build side
      //can only be BroadcastExchangePrel or HashToRandomExchangePrel
      return null;
    }
    List<String> leftFields = left.getRowType().getFieldNames();
    List<String> rightFields = right.getRowType().getFieldNames();
    List<Integer> leftKeys = hashJoinPrel.getLeftKeys();
    List<Integer> rightKeys = hashJoinPrel.getRightKeys();
    RelMetadataQuery metadataQuery = left.getCluster().getMetadataQuery();
    int i = 0;
    for (Integer leftKey : leftKeys) {
      String leftFieldName = leftFields.get(leftKey);
      Integer rightKey = rightKeys.get(i++);
      String rightFieldName = rightFields.get(rightKey);

      //This also avoids the left field of the join condition with a function call.
      ScanPrel scanPrel = findLeftScanPrel(leftFieldName, left);
      if (scanPrel != null) {
        boolean encounteredBlockNode = containBlockNode((Prel) left, scanPrel);
        if (encounteredBlockNode) {
          continue;
        }
        //Collect NDV from the Metadata
        RelDataType scanRowType = scanPrel.getRowType();
        RelDataTypeField field = scanRowType.getField(leftFieldName, true, true);
        int index = field.getIndex();
        Double ndv = metadataQuery.getDistinctRowCount(scanPrel, ImmutableBitSet.of(index), null);
        if (ndv == null) {
          //If NDV is not supplied, we use the row count to estimate the ndv.
          ndv = left.estimateRowCount(metadataQuery) * 0.1;
        }
        int bloomFilterSizeInBytes = BloomFilter.optimalNumOfBytes(ndv.longValue(), fpp);
        bloomFilterSizeInBytes = bloomFilterSizeInBytes > bloomFilterMaxSizeInBytesDef ? bloomFilterMaxSizeInBytesDef : bloomFilterSizeInBytes;
        //left the local parameter to be set later.
        BloomFilterDef bloomFilterDef = new BloomFilterDef(bloomFilterSizeInBytes, false, leftFieldName, rightFieldName);
        bloomFilterDef.setLeftNDV(ndv);
        bloomFilterDefs.add(bloomFilterDef);
        toAddRuntimeFilter.add(scanPrel);
        probeSideScanPrel = scanPrel;
      }
    }
    if (bloomFilterDefs.size() > 0) {
      //left sendToForeman parameter to be set later.
      RuntimeFilterDef runtimeFilterDef = new RuntimeFilterDef(true, false, bloomFilterDefs, false, -1);
      probeSideScan2hj.put(probeSideScanPrel, hashJoinPrel);
      return runtimeFilterDef;
    }
    return null;
  }


  /**
   * Find all the previous defined runtime filters to complement their information.
   */
  private static class RuntimeFilterInfoPaddingHelper extends BasePrelVisitor<Void, RFHelperHolder, RuntimeException> {


    public RuntimeFilterInfoPaddingHelper() {
    }

    @Override
    public Void visitPrel(Prel prel, RFHelperHolder holder) throws RuntimeException {
      for (Prel child : prel) {
        child.accept(this, holder);
      }
      return null;
    }

    @Override
    public Void visitExchange(ExchangePrel exchange, RFHelperHolder holder) throws RuntimeException {
      if (holder != null) {
        if (holder.isFromBuildSide()) {
          holder.setBuildSideExchange(exchange);
        }
      }
      return visitPrel(exchange, holder);
    }

    @Override
    public Void visitJoin(JoinPrel prel, RFHelperHolder holder) throws RuntimeException {
      boolean isHashJoinPrel = prel instanceof HashJoinPrel;
      if (isHashJoinPrel) {
        HashJoinPrel hashJoinPrel = (HashJoinPrel) prel;
        RuntimeFilterDef runtimeFilterDef = hashJoinPrel.getRuntimeFilterDef();
        if (runtimeFilterDef != null) {
          runtimeFilterDef.setGenerateBloomFilter(true);
          if (holder == null) {
            holder = new RFHelperHolder();
          }
          Prel left = (Prel) hashJoinPrel.getLeft();
          left.accept(this, holder);
          //explore the right build side children to find potential RuntimeFilters.
          Prel right = (Prel) hashJoinPrel.getRight();
          holder.setFromBuildSide(true);
          right.accept(this, holder);
          boolean routeToForeman = holder.needToRouteToForeman();
          runtimeFilterDef.setSendToForeman(routeToForeman);
          List<BloomFilterDef> bloomFilterDefs = runtimeFilterDef.getBloomFilterDefs();
          for (BloomFilterDef bloomFilterDef : bloomFilterDefs) {
            bloomFilterDef.setLocal(!routeToForeman);
          }
        }
      }
      return visitPrel(prel, holder);
    }
  }

  /**
   * Find a join condition's left input source scan Prel. If we can't find a target scan Prel then this
   * RuntimeFilter can not pushed down to a probe side scan Prel.
   *
   * @param fieldName   left join condition field Name
   * @param leftRelNode left RelNode of a BiRel or the SingleRel
   * @return a left scan Prel which contains the left join condition name or null
   */
  private ScanPrel findLeftScanPrel(String fieldName, RelNode leftRelNode) {
    if (leftRelNode instanceof ScanPrel) {
      RelDataType scanRowType = leftRelNode.getRowType();
      RelDataTypeField field = scanRowType.getField(fieldName, true, true);
      if (field != null) {
        //found
        return (ScanPrel) leftRelNode;
      } else {
        return null;
      }
    } else if (leftRelNode instanceof RelSubset) {
      RelNode bestNode = ((RelSubset) leftRelNode).getBest();
      if (bestNode != null) {
        return findLeftScanPrel(fieldName, bestNode);
      } else {
        return null;
      }
    } else {
      List<RelNode> relNodes = leftRelNode.getInputs();
      RelNode leftNode = relNodes.get(0);
      return findLeftScanPrel(fieldName, leftNode);
    }
  }

  private ExchangePrel findRightExchangePrel(RelNode rightRelNode) {
    if (rightRelNode instanceof ExchangePrel) {
      return (ExchangePrel) rightRelNode;
    }
    if (rightRelNode instanceof ScanPrel) {
      return null;
    } else if (rightRelNode instanceof RelSubset) {
      RelNode bestNode = ((RelSubset) rightRelNode).getBest();
      if (bestNode != null) {
        return findRightExchangePrel(bestNode);
      } else {
        return null;
      }
    } else {
      List<RelNode> relNodes = rightRelNode.getInputs();
      if (relNodes.size() == 1) {
        RelNode leftNode = relNodes.get(0);
        return findRightExchangePrel(leftNode);
      } else {
        return null;
      }
    }
  }

  private boolean containBlockNode(Prel startNode, Prel endNode) {
    BlockNodeVisitor blockNodeVisitor = new BlockNodeVisitor();
    startNode.accept(blockNodeVisitor, endNode);
    return blockNodeVisitor.isEncounteredBlockNode();
  }

  private static class BlockNodeVisitor extends BasePrelVisitor<Void, Prel, RuntimeException> {

    private boolean encounteredBlockNode;

    @Override
    public Void visitPrel(Prel prel, Prel endValue) throws RuntimeException {
      if (prel == endValue) {
        return null;
      }

      Prel currentPrel;
      if (prel instanceof RelSubset) {
        currentPrel = (Prel) ((RelSubset) prel).getBest();
      } else {
        currentPrel = prel;
      }

      if (currentPrel == null) {
        return null;
      }
      if (currentPrel instanceof StreamAggPrel) {
        encounteredBlockNode = true;
        return null;
      }

      if (currentPrel instanceof HashAggPrel) {
        encounteredBlockNode = true;
        return null;
      }

      if (currentPrel instanceof SortPrel) {
        encounteredBlockNode = true;
        return null;
      }

      if (currentPrel instanceof TopNPrel) {
        encounteredBlockNode = true;
        return null;
      }

      if (currentPrel instanceof HashJoinPrel) {
        encounteredBlockNode = true;
        return null;
      }

      for (Prel subPrel : currentPrel) {
        visitPrel(subPrel, endValue);
      }
      return null;
    }

    public boolean isEncounteredBlockNode() {
      return encounteredBlockNode;
    }
  }


  /**
   * RuntimeFilter helper util holder
   */
  private static class RFHelperHolder {

    private boolean fromBuildSide;

    private ExchangePrel exchangePrel;

    public void setBuildSideExchange(ExchangePrel exchange){
      this.exchangePrel = exchange;
    }

    public boolean needToRouteToForeman() {
      return exchangePrel != null && !(exchangePrel instanceof BroadcastExchangePrel);
    }

    public boolean isFromBuildSide() {
      return fromBuildSide;
    }

    public void setFromBuildSide(boolean fromBuildSide) {
      this.fromBuildSide = fromBuildSide;
    }

  }
}
