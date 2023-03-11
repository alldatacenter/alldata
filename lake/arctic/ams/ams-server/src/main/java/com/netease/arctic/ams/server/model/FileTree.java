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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class FileTree {
  private final DataTreeNode node;
  private final List<DataFileWithSequence> deleteFiles = new ArrayList<>();
  private final List<DataFileWithSequence> insertFiles = new ArrayList<>();
  private final List<DataFileWithSequence> baseFiles = new ArrayList<>();
  private final List<DeleteFileWithSequence> posDeleteFiles = new ArrayList<>();

  private FileTree left;
  private FileTree right;

  public FileTree(DataTreeNode node) {
    this.node = node;
  }

  public FileTree getLeft() {
    return left;
  }

  public FileTree getRight() {
    return right;
  }

  public static FileTree newTreeRoot() {
    return new FileTree(DataTreeNode.of(0, 0));
  }

  public FileTree putNodeIfAbsent(@Nonnull DataTreeNode newNode) {
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

  public void collectInsertFiles(List<DataFile> collector) {
    collector.addAll(insertFiles);
    if (left != null) {
      left.collectInsertFiles(collector);
    }
    if (right != null) {
      right.collectInsertFiles(collector);
    }
  }

  public void collectParentInsertFilesOf(DataTreeNode son, List<DataFile> collector) {
    if (son.mask() <= node.mask()) {
      return;
    }
    if (!son.isSonOf(this.node)) {
      return;
    }
    collector.addAll(insertFiles);
    if (left != null) {
      left.collectParentInsertFilesOf(son, collector);
    }
    if (right != null) {
      right.collectParentInsertFilesOf(son, collector);
    }
  }

  public void collectParentDeleteFilesOf(DataTreeNode son, List<DataFile> collector) {
    if (son.mask() <= node.mask()) {
      return;
    }
    if (!son.isSonOf(this.node)) {
      return;
    }
    collector.addAll(deleteFiles);
    if (left != null) {
      left.collectParentDeleteFilesOf(son, collector);
    }
    if (right != null) {
      right.collectParentDeleteFilesOf(son, collector);
    }
  }

  public void collectDeleteFiles(List<DataFile> collector) {
    collector.addAll(deleteFiles);
    if (left != null) {
      left.collectDeleteFiles(collector);
    }
    if (right != null) {
      right.collectDeleteFiles(collector);
    }
  }

  public void collectBaseFiles(List<DataFile> collector) {
    collector.addAll(baseFiles);
    if (left != null) {
      left.collectBaseFiles(collector);
    }
    if (right != null) {
      right.collectBaseFiles(collector);
    }
  }

  public void collectPosDeleteFiles(List<DeleteFile> collector) {
    collector.addAll(posDeleteFiles);
    if (left != null) {
      left.collectPosDeleteFiles(collector);
    }
    if (right != null) {
      right.collectPosDeleteFiles(collector);
    }
  }

  public long accumulate(Function<FileTree, Long> function) {
    Long apply = function.apply(this);
    if (left != null) {
      apply += left.accumulate(function);
    }
    if (right != null) {
      apply += right.accumulate(function);
    }
    return apply;
  }

  public List<DataFileWithSequence> getDeleteFiles() {
    return deleteFiles;
  }

  public List<DataFileWithSequence> getInsertFiles() {
    return insertFiles;
  }

  public List<DataFileWithSequence> getBaseFiles() {
    return baseFiles;
  }

  public List<DeleteFileWithSequence> getPosDeleteFiles() {
    return posDeleteFiles;
  }

  public void addFile(ContentFileWithSequence<?> file, DataFileType fileType) {
    switch (fileType) {
      case BASE_FILE:
        baseFiles.add((DataFileWithSequence) file);
        break;
      case EQ_DELETE_FILE:
        deleteFiles.add((DataFileWithSequence) file);
        break;
      case INSERT_FILE:
        insertFiles.add((DataFileWithSequence) file);
        break;
      case POS_DELETE_FILE:
        posDeleteFiles.add((DeleteFileWithSequence) file);
        break;
      default:
    }
  }

  public DataTreeNode getNode() {
    return node;
  }

  public boolean isRootEmpty() {
    return baseFiles.isEmpty() && insertFiles.isEmpty() && deleteFiles.isEmpty() && posDeleteFiles.isEmpty();
  }
  
  public boolean isLeaf() {
    return left == null && right == null;
  }

  /**
   * Complete this binary tree to make every subTree of this Tree As a Full Binary Tree(FBT), if any data exists in this
   * subTree.
   * <p>
   * A Full Binary Tree(FBT) is a binary tree in which all the nodes have either 0 or 2 offspring. In other terms, it is
   * a binary tree in which all nodes, except the leaf nodes, have two offspring.
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
    return !baseFiles.isEmpty() || !insertFiles.isEmpty() || !deleteFiles.isEmpty();
  }

  public boolean anyMatch(Predicate<FileTree> predicate) {
    if (predicate.test(this)) {
      return true;
    }
    if (left != null && left.anyMatch(predicate)) {
      return true;
    }
    return right != null && right.anyMatch(predicate);
  }
}