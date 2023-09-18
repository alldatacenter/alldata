/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.offline.module.action;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.meta.NodeType;

import java.util.Map;
import java.util.Set;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-02 12:34
 **/
public class DuplicateEntitisJudge {
  private Set<String> relevantEntitis = Sets.newHashSet();

  private Set<String> duplicateEntities = Sets.newHashSet();

  private Map<String, DependencyNode> dumpNodes = Maps.newHashMap();

  private Map<String, SqlTaskNodeMeta> joinNodes = Maps.newHashMap();

  private final Context context;
  private final IControlMsgHandler module;

  public DuplicateEntitisJudge(IControlMsgHandler module, Context context) {
    this.context = context;
    this.module = module;
  }

  public void add(DependencyNode dnode) {
    this.add(dnode.getName());
    this.dumpNodes.put(dnode.getId(), dnode);

  }

  public void add(SqlTaskNodeMeta pnode) {
    this.add(pnode.getExportName());
    this.joinNodes.put(pnode.getId(), pnode);
  }

  public boolean isSuccess() {
    boolean hasDuplicate = !getDuplicateEntities().isEmpty();
    if (hasDuplicate) {
      this.module.addErrorMessage(context
        , "名为" + String.join(",", this.getDuplicateEntities()) + " 的节点存在重复");
      return false;
    }
    NodeType type = null;
    // 校验每个依赖节点是否 都存在
    for (SqlTaskNodeMeta joinNode : joinNodes.values()) {

      for (DependencyNode dpt : joinNode.getDependencies()) {
        type = dpt.parseNodeType(false);
        if (type == NodeType.DUMP || type == NodeType.JOINER_SQL) {

          if (dumpNodes.get(dpt.getId()) == null && joinNodes.get(dpt.getId()) == null) {
            //lack dump error
            addLackNodeError(joinNode, dpt);
          }

        } else {
          throw new UnsupportedOperationException(String.valueOf(type));
        }
      }
    }

    return !this.context.hasErrors();
  }

  public void addLackNodeError(SqlTaskNodeMeta joinNode, DependencyNode dpt) {
    this.module.addErrorMessage(context
      , "名为'" + joinNode.getExportName() + "' 的节的依赖节点'" + dpt.getName() + "'不存在,请确认是否已经删除？");
  }

  public Set<String> getDuplicateEntities() {
    return this.duplicateEntities;
  }


  private void add(String entity) {
    if (!relevantEntitis.add(entity)) {
      duplicateEntities.add(entity);
    }

  }
}
