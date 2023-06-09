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
package org.apache.drill.exec.physical.impl.common;

import java.io.IOException;
import java.util.List;

import com.sun.codemodel.JExpression;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.sig.GeneratorMapping;
import org.apache.drill.exec.compile.sig.MappingSet;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.expr.fn.FunctionGenerationHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.impl.join.JoinUtils;
import org.apache.drill.exec.planner.physical.HashPrelUtil;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.vector.ValueVector;

import com.sun.codemodel.JConditional;
import com.sun.codemodel.JExpr;

/**
 * This is a master class used to generate code for {@link HashTable}s.
 */
public class ChainedHashTable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChainedHashTable.class);

  private static final GeneratorMapping KEY_MATCH_BUILD =
      GeneratorMapping.create("setupInterior" /* setup method */, "isKeyMatchInternalBuild" /* eval method */,
          null /* reset */, null /* cleanup */);

  private static final GeneratorMapping BOTH_KEYS_NULL =
    GeneratorMapping.create("setupInterior" /* setup method */, "areBothKeysNull" /* eval method */,
      null /* reset */, null /* cleanup */);

  private static final GeneratorMapping KEY_MATCH_PROBE =
      GeneratorMapping.create("setupInterior" /* setup method */, "isKeyMatchInternalProbe" /* eval method */,
          null /* reset */, null /* cleanup */);

  private static final GeneratorMapping GET_HASH_BUILD =
      GeneratorMapping.create("doSetup" /* setup method */, "getHashBuild" /* eval method */,
          null /* reset */, null /* cleanup */);

  private static final GeneratorMapping GET_HASH_PROBE =
      GeneratorMapping.create("doSetup" /* setup method */, "getHashProbe" /* eval method */, null /* reset */,
          null /* cleanup */);

  private static final GeneratorMapping SET_VALUE =
      GeneratorMapping.create("setupInterior" /* setup method */, "setValue" /* eval method */, null /* reset */,
          null /* cleanup */);

  private static final GeneratorMapping OUTPUT_KEYS =
      GeneratorMapping.create("setupInterior" /* setup method */, "outputRecordKeys" /* eval method */,
          null /* reset */, null /* cleanup */);

  // GM for putting constant expression into method "setupInterior"
  private static final GeneratorMapping SETUP_INTERIOR_CONSTANT =
      GeneratorMapping.create("setupInterior" /* setup method */, "setupInterior" /* eval method */,
          null /* reset */, null /* cleanup */);

  // GM for putting constant expression into method "doSetup"
  private static final GeneratorMapping DO_SETUP_CONSTANT =
      GeneratorMapping.create("doSetup" /* setup method */, "doSetup" /* eval method */, null /* reset */,
          null /* cleanup */);

  private final MappingSet KeyMatchIncomingBuildMapping =
      new MappingSet("incomingRowIdx", null, "incomingBuild", null, SETUP_INTERIOR_CONSTANT, KEY_MATCH_BUILD);
  private final MappingSet bothKeysNullIncomingBuildMapping =
    new MappingSet("incomingRowIdx", null, "incomingBuild", null, SETUP_INTERIOR_CONSTANT, BOTH_KEYS_NULL);
  private final MappingSet KeyMatchIncomingProbeMapping =
      new MappingSet("incomingRowIdx", null, "incomingProbe", null, SETUP_INTERIOR_CONSTANT, KEY_MATCH_PROBE);
  private final MappingSet KeyMatchHtableMapping =
      new MappingSet("htRowIdx", null, "htContainer", null, SETUP_INTERIOR_CONSTANT, KEY_MATCH_BUILD);
  private final MappingSet bothKeysNullHtableMapping =
    new MappingSet("htRowIdx", null, "htContainer", null, SETUP_INTERIOR_CONSTANT, BOTH_KEYS_NULL);
  private final MappingSet KeyMatchHtableProbeMapping =
      new MappingSet("htRowIdx", null, "htContainer", null, SETUP_INTERIOR_CONSTANT, KEY_MATCH_PROBE);
  private final MappingSet GetHashIncomingBuildMapping =
      new MappingSet("incomingRowIdx", null, "incomingBuild", null, DO_SETUP_CONSTANT, GET_HASH_BUILD);
  private final MappingSet GetHashIncomingProbeMapping =
      new MappingSet("incomingRowIdx", null, "incomingProbe", null, DO_SETUP_CONSTANT, GET_HASH_PROBE);
  private final MappingSet SetValueMapping =
      new MappingSet("incomingRowIdx" /* read index */, "htRowIdx" /* write index */,
          "incomingBuild" /* read container */, "htContainer" /* write container */, SETUP_INTERIOR_CONSTANT,
          SET_VALUE);

  private final MappingSet OutputRecordKeysMapping =
      new MappingSet("htRowIdx" /* read index */, "outRowIdx" /* write index */, "htContainer" /* read container */,
          "outgoing" /* write container */, SETUP_INTERIOR_CONSTANT, OUTPUT_KEYS);

  private HashTableConfig htConfig;
  private final FragmentContext context;
  private final BufferAllocator allocator;
  private RecordBatch incomingBuild;
  private RecordBatch incomingProbe;
  private final RecordBatch outgoing;

  private enum SetupWork {DO_BUILD, DO_PROBE, CHECK_BOTH_NULLS};

  public ChainedHashTable(HashTableConfig htConfig, FragmentContext context, BufferAllocator allocator,
                          RecordBatch incomingBuild, RecordBatch incomingProbe, RecordBatch outgoing) {

    this.htConfig = htConfig;
    this.context = context;
    this.allocator = allocator;
    this.incomingBuild = incomingBuild;
    this.incomingProbe = incomingProbe;
    this.outgoing = outgoing;
  }

  public void updateIncoming(RecordBatch incomingBuild, RecordBatch incomingProbe) {
    this.incomingBuild = incomingBuild;
    this.incomingProbe = incomingProbe;
  }

  public HashTable createAndSetupHashTable(TypedFieldId[] outKeyFieldIds) throws ClassTransformationException,
      IOException, SchemaChangeException {
    CodeGenerator<HashTable> top = CodeGenerator.get(HashTable.TEMPLATE_DEFINITION, context.getOptions());
    top.plainJavaCapable(true);
    // Uncomment out this line to debug the generated code.
    // This code is called from generated code, so to step into this code,
    // persist the code generated in HashAggBatch also.
    // top.saveCodeForDebugging(true);
    top.preferPlainJava(true); // use a subclass
    ClassGenerator<HashTable> cg = top.getRoot();
    ClassGenerator<HashTable> cgInner = cg.getInnerGenerator("BatchHolder");

    LogicalExpression[] keyExprsBuild = new LogicalExpression[htConfig.getKeyExprsBuild().size()];
    LogicalExpression[] keyExprsProbe = null;
    boolean isProbe = (htConfig.getKeyExprsProbe() != null);
    if (isProbe) {
      keyExprsProbe = new LogicalExpression[htConfig.getKeyExprsProbe().size()];
    }

    ErrorCollector collector = new ErrorCollectorImpl();
    VectorContainer htContainerOrig = new VectorContainer(); // original ht container from which others may be cloned
    TypedFieldId[] htKeyFieldIds = new TypedFieldId[htConfig.getKeyExprsBuild().size()];

    int i = 0;
    for (NamedExpression ne : htConfig.getKeyExprsBuild()) {
      final LogicalExpression expr = ExpressionTreeMaterializer.materialize(ne.getExpr(), incomingBuild, collector, context.getFunctionRegistry());
      if (collector.hasErrors()) {
        throw new SchemaChangeException("Failure while materializing expression. " + collector.toErrorString());
      }
      if (expr == null) {
        continue;
      }
      keyExprsBuild[i] = expr;
      i++;
    }

    if (isProbe) {
      i = 0;
      for (NamedExpression ne : htConfig.getKeyExprsProbe()) {
        final LogicalExpression expr = ExpressionTreeMaterializer.materialize(ne.getExpr(), incomingProbe, collector, context.getFunctionRegistry());
        if (collector.hasErrors()) {
          throw new SchemaChangeException("Failure while materializing expression. " + collector.toErrorString());
        }
        if (expr == null) {
          continue;
        }
        keyExprsProbe[i] = expr;
        i++;
      }
      JoinUtils.addLeastRestrictiveCasts(keyExprsProbe, incomingProbe, keyExprsBuild, incomingBuild, context);
    }

    i = 0;
    /*
     * Once the implicit casts have been added, create the value vectors for the corresponding
     * type and add it to the hash table's container.
     * Note: Adding implicit casts may have a minor impact on the memory foot print. For example
     * if we have a join condition with bigint on the probe side and int on the build side then
     * after this change we will be allocating a bigint vector in the hashtable instead of an int
     * vector.
     */
    for (NamedExpression ne : htConfig.getKeyExprsBuild()) {
      LogicalExpression expr = keyExprsBuild[i];
      final MaterializedField outputField = MaterializedField.create(ne.getRef().getLastSegment().getNameSegment().getPath(),
                                                                      expr.getMajorType());
      ValueVector vv = TypeHelper.getNewVector(outputField, allocator);
      htKeyFieldIds[i] = htContainerOrig.add(vv);
      i++;
    }

    // Only in case of a join: Generate a special method to check if both the new key and the existing key (in this HT bucket) are nulls
    // (used by Hash-Join to avoid creating a long hash-table chain of null keys, which can lead to useless O(n^2) work on that chain.)
    // The logic is: Nulls match on build, and don't match on probe. Note that this logic covers outer joins as well.
    setupIsKeyMatchInternal(cgInner, bothKeysNullIncomingBuildMapping, bothKeysNullHtableMapping, keyExprsBuild,
        htConfig.getComparators(), htKeyFieldIds, SetupWork.CHECK_BOTH_NULLS);
    // generate code for isKeyMatch(), setValue(), getHash() and outputRecordKeys()
    setupIsKeyMatchInternal(cgInner, KeyMatchIncomingBuildMapping, KeyMatchHtableMapping, keyExprsBuild,
        htConfig.getComparators(), htKeyFieldIds, SetupWork.DO_BUILD);
    setupIsKeyMatchInternal(cgInner, KeyMatchIncomingProbeMapping, KeyMatchHtableProbeMapping, keyExprsProbe,
        htConfig.getComparators(), htKeyFieldIds, SetupWork.DO_PROBE);

    setupSetValue(cgInner, keyExprsBuild, htKeyFieldIds);
    if (outgoing != null) {

      if (outKeyFieldIds.length > htConfig.getKeyExprsBuild().size()) {
        throw new IllegalArgumentException("Mismatched number of output key fields.");
      }
    }
    setupOutputRecordKeys(cgInner, htKeyFieldIds, outKeyFieldIds);

    setupGetHash(cg /* use top level code generator for getHash */, GetHashIncomingBuildMapping, incomingBuild, keyExprsBuild);
    setupGetHash(cg /* use top level code generator for getHash */, GetHashIncomingProbeMapping, incomingProbe, keyExprsProbe);

    HashTable ht = context.getImplementationClass(top);
    ht.setup(htConfig, allocator, incomingBuild.getContainer(), incomingProbe, outgoing, htContainerOrig, context, cgInner);

    return ht;
  }

  private void setupIsKeyMatchInternal(ClassGenerator<HashTable> cg, MappingSet incomingMapping, MappingSet htableMapping,
      LogicalExpression[] keyExprs, List<Comparator> comparators, TypedFieldId[] htKeyFieldIds, SetupWork work) {

    boolean checkIfBothNulls = work == SetupWork.CHECK_BOTH_NULLS;

    // Regular key matching may return false in the middle (i.e., some pair of columns did not match), and true only if all matched;
    // but "both nulls" check returns the opposite logic (i.e., true when one pair of nulls is found, need check no more)
    JExpression midPointResult = checkIfBothNulls ? JExpr.TRUE : JExpr.FALSE;
    JExpression finalResult = checkIfBothNulls ? JExpr.FALSE : JExpr.TRUE;

    cg.setMappingSet(incomingMapping);

    if (keyExprs == null || keyExprs.length == 0 ||
        checkIfBothNulls && ! comparators.contains(Comparator.EQUALS)) { // e.g. for Hash-Aggr, or non-equi join
      cg.getEvalBlock()._return(JExpr.FALSE);
      return;
    }

    for (int i = 0; i < keyExprs.length; i++) {
      final LogicalExpression expr = keyExprs[i];
      cg.setMappingSet(incomingMapping);
      HoldingContainer left = cg.addExpr(expr, ClassGenerator.BlkCreateMode.TRUE_IF_BOUND);

      cg.setMappingSet(htableMapping);
      ValueVectorReadExpression vvrExpr = new ValueVectorReadExpression(htKeyFieldIds[i]);
      HoldingContainer right = cg.addExpr(vvrExpr, ClassGenerator.BlkCreateMode.FALSE);

      JConditional jc;

      if ( work != SetupWork.DO_BUILD ) {  // BUILD runs this logic in a separate method - areBothKeysNull()
        // codegen for the special case when both columns are null (i.e., return early with midPointResult)
        if (comparators.get(i) == Comparator.EQUALS
            && left.isOptional() && right.isOptional()) {
          jc = cg.getEvalBlock()._if(left.getIsSet().eq(JExpr.lit(0)).
            cand(right.getIsSet().eq(JExpr.lit(0))));
          jc._then()._return(midPointResult);
        }
      }
      if ( ! checkIfBothNulls ) { // generate comparison code (at least one of the two columns' values is non-null)
        final LogicalExpression f = FunctionGenerationHelper.getOrderingComparatorNullsHigh(left, right, context.getFunctionRegistry());

        HoldingContainer out = cg.addExpr(f, ClassGenerator.BlkCreateMode.FALSE);

        // check if two values are not equal (comparator result != 0)
        jc = cg.getEvalBlock()._if(out.getValue().ne(JExpr.lit(0)));

        jc._then()._return(midPointResult);
      }
    }

    // All key expressions compared the same way, so return the appropriate final result
    cg.getEvalBlock()._return(finalResult);
  }

  private void setupSetValue(ClassGenerator<HashTable> cg, LogicalExpression[] keyExprs,
                             TypedFieldId[] htKeyFieldIds) {

    cg.setMappingSet(SetValueMapping);

    int i = 0;
    for (LogicalExpression expr : keyExprs) {
      ValueVectorWriteExpression vvwExpr = new ValueVectorWriteExpression(htKeyFieldIds[i++], expr, true);
      cg.addExpr(vvwExpr, ClassGenerator.BlkCreateMode.TRUE_IF_BOUND);
    }
  }

  private void setupOutputRecordKeys(ClassGenerator<HashTable> cg, TypedFieldId[] htKeyFieldIds, TypedFieldId[] outKeyFieldIds) {

    cg.setMappingSet(OutputRecordKeysMapping);

    if (outKeyFieldIds != null) {
      for (int i = 0; i < outKeyFieldIds.length; i++) {
        ValueVectorReadExpression vvrExpr = new ValueVectorReadExpression(htKeyFieldIds[i]);
        boolean useSetSafe = !Types.isFixedWidthType(vvrExpr.getMajorType()) || Types.isRepeated(vvrExpr.getMajorType());
        ValueVectorWriteExpression vvwExpr = new ValueVectorWriteExpression(outKeyFieldIds[i], vvrExpr, useSetSafe);
        cg.addExpr(vvwExpr, ClassGenerator.BlkCreateMode.TRUE);
      }

    }
  }

  private void setupGetHash(ClassGenerator<HashTable> cg, MappingSet incomingMapping, VectorAccessible batch, LogicalExpression[] keyExprs)
    throws SchemaChangeException {

    cg.setMappingSet(incomingMapping);

    if (keyExprs == null || keyExprs.length == 0) {
      cg.getEvalBlock()._return(JExpr.lit(0));
      return;
    }

    /*
     * We use the same logic to generate run time code for the hash function both for hash join and hash
     * aggregate. For join we need to hash everything as double (both for distribution and for comparison) but
     * for aggregation we can avoid the penalty of casting to double
     */


    /*
      Generate logical expression for each key so expression can be split into blocks if number of expressions in method exceeds upper limit.
      `seedValue` is used as holder to pass generated seed value for the new methods.
    */
    String seedValue = "seedValue";
    LogicalExpression seed = ValueExpressions.getParameterExpression(seedValue, Types.required(TypeProtos.MinorType.INT));

    for (LogicalExpression expr : keyExprs) {
      LogicalExpression hashExpression = HashPrelUtil.getHashExpression(expr, seed, incomingProbe != null);
      LogicalExpression materializedExpr = ExpressionTreeMaterializer.materializeAndCheckErrors(hashExpression, batch, context.getFunctionRegistry());
      HoldingContainer hash = cg.addExpr(materializedExpr, ClassGenerator.BlkCreateMode.TRUE_IF_BOUND);
      cg.getEvalBlock().assign(JExpr.ref(seedValue), hash.getValue());
    }

    cg.getEvalBlock()._return(JExpr.ref(seedValue));
  }
}
