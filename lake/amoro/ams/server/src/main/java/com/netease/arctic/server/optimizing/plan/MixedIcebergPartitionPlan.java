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

package com.netease.arctic.server.optimizing.plan;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.optimizing.MixFormatRewriteExecutorFactory;
import com.netease.arctic.optimizing.OptimizingInputProperties;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.BinPacking;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MixedIcebergPartitionPlan extends AbstractPartitionPlan {

  public MixedIcebergPartitionPlan(TableRuntime tableRuntime,
                                   ArcticTable table, String partition, long planTime) {
    super(tableRuntime, table, partition, planTime);
  }

  @Override
  public void addFile(IcebergDataFile dataFile, List<IcebergContentFile<?>> deletes) {
    super.addFile(dataFile, deletes);
    if (evaluator().isChangeFile(dataFile)) {
      markSequence(dataFile.getSequenceNumber());
    }
    for (IcebergContentFile<?> deleteFile : deletes) {
      if (deleteFile.content() == FileContent.DATA) {
        markSequence(deleteFile.getSequenceNumber());
      }
    }
  }

  @Override
  protected MixedIcebergPartitionEvaluator evaluator() {
    return ((MixedIcebergPartitionEvaluator) super.evaluator());
  }

  @Override
  protected boolean taskNeedExecute(SplitTask task) {
    if (super.taskNeedExecute(task)) {
      return true;
    } else {
      return task.getRewriteDataFiles().stream().anyMatch(evaluator()::isChangeFile);
    }
  }

  @Override
  protected OptimizingInputProperties buildTaskProperties() {
    OptimizingInputProperties properties = new OptimizingInputProperties();
    properties.setExecutorFactoryImpl(MixFormatRewriteExecutorFactory.class.getName());
    return properties;
  }
  
  protected boolean isKeyedTable() {
    return tableObject.isKeyedTable();
  }

  @Override
  protected TaskSplitter buildTaskSplitter() {
    if (isKeyedTable()) {
      return new TreeNodeTaskSplitter();
    } else {
      return new BinPackingTaskSplitter();
    }
  }

  @Override
  protected CommonPartitionEvaluator buildEvaluator() {
    return new MixedIcebergPartitionEvaluator(tableRuntime, partition, planTime, isKeyedTable());
  }

  protected static class MixedIcebergPartitionEvaluator extends CommonPartitionEvaluator {
    protected final boolean keyedTable;
    protected boolean hasChangeFiles = false;
    // partition property
    protected long lastBaseOptimizedTime;

    public MixedIcebergPartitionEvaluator(TableRuntime tableRuntime, String partition, long planTime,
                                          boolean keyedTable) {
      super(tableRuntime, partition, planTime);
      this.keyedTable = keyedTable;
    }

    @Override
    public void addFile(IcebergDataFile dataFile, List<IcebergContentFile<?>> deletes) {
      super.addFile(dataFile, deletes);
      if (!hasChangeFiles && isChangeFile(dataFile)) {
        hasChangeFiles = true;
      }
    }

    @Override
    public void addPartitionProperties(Map<String, String> properties) {
      super.addPartitionProperties(properties);
      String optimizedTime = properties.get(TableProperties.PARTITION_BASE_OPTIMIZED_TIME);
      if (optimizedTime != null) {
        this.lastBaseOptimizedTime = Long.parseLong(optimizedTime);
      }
    }

    protected boolean isChangeFile(IcebergDataFile dataFile) {
      if (!keyedTable) {
        return false;
      }
      PrimaryKeyedFile file = (PrimaryKeyedFile) dataFile.internalFile();
      return file.type() == DataFileType.INSERT_FILE || file.type() == DataFileType.EQ_DELETE_FILE;
    }

    @Override
    protected boolean isFragmentFile(IcebergDataFile dataFile) {
      PrimaryKeyedFile file = (PrimaryKeyedFile) dataFile.internalFile();
      if (file.type() == DataFileType.BASE_FILE) {
        return dataFile.fileSizeInBytes() <= fragmentSize;
      } else if (file.type() == DataFileType.INSERT_FILE) {
        // for keyed table, we treat all insert files as fragment files
        return true;
      } else {
        throw new IllegalStateException("unexpected file type " + file.type() + " of " + file);
      }
    }

    @Override
    public boolean isMinorNecessary() {
      if (keyedTable) {
        int smallFileCount = fragmentFileCount + equalityDeleteFileCount;
        int baseSplitCount = getBaseSplitCount();
        if (smallFileCount >= Math.max(baseSplitCount, config.getMinorLeastFileCount())) {
          return true;
        } else if ((smallFileCount > baseSplitCount || hasChangeFiles) && reachMinorInterval()) {
          return true;
        } else if (hasChangeFiles && reachBaseRefreshInterval()) {
          return true;
        } else {
          return false;
        }
      } else {
        return super.isMinorNecessary();
      }
    }

    @Override
    public boolean shouldRewritePosForSegmentFile(IcebergDataFile dataFile, List<IcebergContentFile<?>> deletes) {
      if (deletes.stream().anyMatch(
          delete -> delete.content() == FileContent.EQUALITY_DELETES || delete.content() == FileContent.DATA)) {
        // change equality delete file's content is DATA
        return true;
      } else if (deletes.stream().filter(delete -> delete.content() == FileContent.POSITION_DELETES).count() >= 2) {
        return true;
      } else {
        return false;
      }
    }

    protected boolean reachBaseRefreshInterval() {
      return config.getBaseRefreshInterval() >= 0 && planTime - lastBaseOptimizedTime > config.getBaseRefreshInterval();
    }

    protected int getBaseSplitCount() {
      if (keyedTable) {
        return config.getBaseHashBucket();
      } else {
        return 1;
      }
    }

    @Override
    public boolean isFullNecessary() {
      if (!reachFullInterval()) {
        return false;
      }
      return anyDeleteExist() || fragmentFileCount > getBaseSplitCount() || hasChangeFiles;
    }

    @Override
    public PartitionEvaluator.Weight getWeight() {
      return new Weight(getCost(), hasChangeFiles && reachBaseRefreshInterval());
    }

    protected static class Weight implements PartitionEvaluator.Weight {
      private final long cost;
      private final boolean reachDelay;

      public Weight(long cost, boolean reachDelay) {
        this.cost = cost;
        this.reachDelay = reachDelay;
      }

      @Override
      public int compareTo(PartitionEvaluator.Weight o) {
        Weight that = (Weight) o;
        int compare = Boolean.compare(this.reachDelay, that.reachDelay);
        if (compare != 0) {
          return compare;
        }
        return Long.compare(this.cost, that.cost);
      }
    }
  }

  /**
   * split task with bin-packing
   */
  private class BinPackingTaskSplitter implements TaskSplitter {

    @Override
    public List<SplitTask> splitTasks(int targetTaskCount) {
      // bin-packing
      List<FileTask> allDataFiles = Lists.newArrayList();
      segmentFiles.forEach((dataFile, deleteFiles) ->
          allDataFiles.add(new FileTask(dataFile, deleteFiles, false)));
      fragmentFiles.forEach((dataFile, deleteFiles) ->
          allDataFiles.add(new FileTask(dataFile, deleteFiles, true)));

      long taskSize = config.getTargetSize();
      Long sum = allDataFiles.stream().map(f -> f.getFile().fileSizeInBytes()).reduce(0L, Long::sum);
      int taskCnt = (int) (sum / taskSize) + 1;
      List<List<FileTask>> packed = new BinPacking.ListPacker<FileTask>(taskSize, taskCnt, true)
          .pack(allDataFiles, f -> f.getFile().fileSizeInBytes());

      // collect
      List<SplitTask> results = Lists.newArrayList();
      for (List<FileTask> fileTasks : packed) {
        Map<IcebergDataFile, List<IcebergContentFile<?>>> fragmentFiles = com.google.common.collect.Maps.newHashMap();
        Map<IcebergDataFile, List<IcebergContentFile<?>>> segmentFiles = com.google.common.collect.Maps.newHashMap();
        fileTasks.stream().filter(FileTask::isFragment)
            .forEach(f -> fragmentFiles.put(f.getFile(), f.getDeleteFiles()));
        fileTasks.stream().filter(FileTask::isSegment)
            .forEach(f -> segmentFiles.put(f.getFile(), f.getDeleteFiles()));
        results.add(new SplitTask(fragmentFiles, segmentFiles));
      }
      return results;
    }
  }

  /**
   * util class for bin-pack
   */
  private static class FileTask {
    private final IcebergDataFile file;
    private final List<IcebergContentFile<?>> deleteFiles;
    private final boolean isFragment;

    public FileTask(IcebergDataFile file, List<IcebergContentFile<?>> deleteFiles, boolean isFragment) {
      this.file = file;
      this.deleteFiles = deleteFiles;
      this.isFragment = isFragment;
    }

    public IcebergDataFile getFile() {
      return file;
    }

    public List<IcebergContentFile<?>> getDeleteFiles() {
      return deleteFiles;
    }

    public boolean isFragment() {
      return isFragment;
    }

    public boolean isSegment() {
      return !isFragment;
    }
  }

  /**
   * split task with {@link DataTreeNode}
   */
  private class TreeNodeTaskSplitter implements TaskSplitter {
    @Override
    public List<SplitTask> splitTasks(int targetTaskCount) {
      List<SplitTask> result = Lists.newArrayList();
      FileTree rootTree = FileTree.newTreeRoot();
      segmentFiles.forEach(rootTree::addSegmentFile);
      fragmentFiles.forEach(rootTree::addFragmentFile);
      rootTree.completeTree();
      List<FileTree> subTrees = Lists.newArrayList();
      rootTree.splitFileTree(subTrees, new SplitIfNoFileExists());
      for (FileTree subTree : subTrees) {
        Map<IcebergDataFile, List<IcebergContentFile<?>>> fragmentFiles = Maps.newHashMap();
        Map<IcebergDataFile, List<IcebergContentFile<?>>> segmentFiles = Maps.newHashMap();
        subTree.collectFragmentFiles(fragmentFiles);
        subTree.collectSegmentFiles(segmentFiles);
        result.add(new SplitTask(fragmentFiles, segmentFiles));
      }
      return result;
    }
  }

  private static class FileTree {

    private final DataTreeNode node;
    private final Map<IcebergDataFile, List<IcebergContentFile<?>>> fragmentFiles = Maps.newHashMap();
    private final Map<IcebergDataFile, List<IcebergContentFile<?>>> segmentFiles = Maps.newHashMap();

    private FileTree left;
    private FileTree right;

    public FileTree(DataTreeNode node) {
      this.node = node;
    }

    public static FileTree newTreeRoot() {
      return new FileTree(DataTreeNode.of(0, 0));
    }

    private FileTree putNodeIfAbsent(@Nonnull DataTreeNode newNode) {
      if (newNode.equals(node)) {
        return this;
      }
      if (newNode.isSonOf(node.left())) {
        if (left == null) {
          left = new FileTree(node.left());
        }
        return left.putNodeIfAbsent(newNode);
      } else if (newNode.isSonOf(node.right())) {
        if (right == null) {
          right = new FileTree(node.right());
        }
        return right.putNodeIfAbsent(newNode);
      } else {
        throw new IllegalArgumentException(newNode + " is not son of " + node);
      }
    }

    /**
     * split file tree with split condition.
     *
     * @param collector - collect result
     * @param canSplit  - if this tree can split
     */
    public void splitFileTree(List<FileTree> collector, Predicate<FileTree> canSplit) {
      if (canSplit.test(this)) {
        if (left != null) {
          left.splitFileTree(collector, canSplit);
        }
        if (right != null) {
          right.splitFileTree(collector, canSplit);
        }
      } else {
        collector.add(this);
      }
    }

    public void collectFragmentFiles(Map<IcebergDataFile, List<IcebergContentFile<?>>> collector) {
      collector.putAll(fragmentFiles);
      if (left != null) {
        left.collectFragmentFiles(collector);
      }
      if (right != null) {
        right.collectFragmentFiles(collector);
      }
    }

    public void collectSegmentFiles(Map<IcebergDataFile, List<IcebergContentFile<?>>> collector) {
      collector.putAll(segmentFiles);
      if (left != null) {
        left.collectSegmentFiles(collector);
      }
      if (right != null) {
        right.collectSegmentFiles(collector);
      }
    }

    public void addSegmentFile(IcebergDataFile file, List<IcebergContentFile<?>> deleteFiles) {
      PrimaryKeyedFile primaryKeyedFile = (PrimaryKeyedFile) file.internalFile();
      FileTree node = putNodeIfAbsent(primaryKeyedFile.node());
      node.segmentFiles.put(file, deleteFiles);
    }

    public void addFragmentFile(IcebergDataFile file, List<IcebergContentFile<?>> deleteFiles) {
      PrimaryKeyedFile primaryKeyedFile = (PrimaryKeyedFile) file.internalFile();
      FileTree node = putNodeIfAbsent(primaryKeyedFile.node());
      node.fragmentFiles.put(file, deleteFiles);
    }

    public boolean isRootEmpty() {
      return segmentFiles.isEmpty() && fragmentFiles.isEmpty();
    }

    public boolean isLeaf() {
      return left == null && right == null;
    }

    /**
     * Complete this binary tree to make every subTree of this Tree As a Full Binary Tree(FBT), if any data exists in
     * this subTree.
     * <p>
     * A Full Binary Tree(FBT) is a binary tree in which all the nodes have either 0 or 2 offspring. In other terms, it
     * is a binary tree in which all nodes, except the leaf nodes, have two offspring.
     * <p>
     * To Complete the tree is to avoid ancestor node's data can't be covered when split subTree.
     */
    public void completeTree() {
      completeTree(false);
    }

    private void completeTree(boolean ancestorFileExist) {
      if (left == null && right == null) {
        return;
      }
      // if any ancestor of this node or this node itself contains any file, this node must be balance
      boolean thisNodeMustBalance = ancestorFileExist || fileExist();
      if (thisNodeMustBalance) {
        // fill and empty node if left or right node not exist
        if (left == null) {
          left = new FileTree(node.left());
        }
        if (right == null) {
          right = new FileTree(node.right());
        }
      }
      if (left != null) {
        left.completeTree(ancestorFileExist || fileExist());
      }
      if (right != null) {
        right.completeTree(ancestorFileExist || fileExist());
      }
    }

    private boolean fileExist() {
      return !segmentFiles.isEmpty() || !fragmentFiles.isEmpty();
    }
  }

  private static class SplitIfNoFileExists implements Predicate<FileTree> {

    public SplitIfNoFileExists() {
    }

    /**
     * file tree can split if:
     * - root node isn't leaf node
     * - and no file exists in the root node
     *
     * @param fileTree - file tree to split
     * @return true if this fileTree need split
     */
    @Override
    public boolean test(FileTree fileTree) {
      return !fileTree.isLeaf() && fileTree.isRootEmpty();
    }
  }
}
