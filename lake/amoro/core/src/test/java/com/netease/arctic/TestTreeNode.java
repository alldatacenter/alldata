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

package com.netease.arctic;

import com.netease.arctic.data.DataTreeNode;
import org.junit.Assert;
import org.junit.Test;

public class TestTreeNode {

  @Test(expected = IllegalArgumentException.class)
  public void testParentNodeIllegal() {
    DataTreeNode.of(0, 0).parent();
  }

  @Test
  public void testParentNode() {
    assertParentNode(DataTreeNode.of(0, 0));
    assertParentNode(DataTreeNode.of(1, 0));
    assertParentNode(DataTreeNode.of(1, 0));
    assertParentNode(DataTreeNode.of(3, 3));
    assertParentNode(DataTreeNode.of(255, 0));
    assertParentNode(DataTreeNode.of(255, 126));
    assertParentNode(DataTreeNode.of(255, 245));
    assertParentNode(DataTreeNode.of(255, 255));
  }

  @Test
  public void testNodeId() {
    Assert.assertEquals(1, DataTreeNode.of(0, 0).getId());
    Assert.assertEquals(2, DataTreeNode.of(1, 0).getId());
    Assert.assertEquals(3, DataTreeNode.of(1, 1).getId());
    Assert.assertEquals(4, DataTreeNode.of(3, 0).getId());
    Assert.assertEquals(7, DataTreeNode.of(3, 3).getId());
    Assert.assertEquals(13, DataTreeNode.of(7, 5).getId());
    Assert.assertEquals(11, DataTreeNode.of(7, 3).getId());

    Assert.assertEquals(DataTreeNode.of(0, 0), DataTreeNode.ofId(1));
    Assert.assertEquals(DataTreeNode.of(1, 0), DataTreeNode.ofId(2));
    Assert.assertEquals(DataTreeNode.of(1, 1), DataTreeNode.ofId(3));
    Assert.assertEquals(DataTreeNode.of(3, 0), DataTreeNode.ofId(4));
    Assert.assertEquals(DataTreeNode.of(3, 3), DataTreeNode.ofId(7));
    Assert.assertEquals(DataTreeNode.of(7, 5), DataTreeNode.ofId(13));
    Assert.assertEquals(DataTreeNode.of(7, 3), DataTreeNode.ofId(11));
  }

  private void assertParentNode(DataTreeNode node) {
    Assert.assertEquals(node, node.left().parent());
    Assert.assertEquals(node, node.right().parent());
  }
}