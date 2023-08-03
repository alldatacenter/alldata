/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Graph, Edge } from '@antv/x6'

export function getDagData(graph: Graph, t: Function) {
  const rootNodes = graph.getRootNodes()
  const checkRoot = rootNodes.every((rootNode) => {
    return rootNode.getData().type === 'source'
  })
  if (!checkRoot) {
    window.$message.error(
      t('project.synchronization_definition.start_node_tips')
    )
    return
  }

  const leafNodes = graph.getLeafNodes()
  const checkLeaf = leafNodes.every((leafNode) => {
    return leafNode.getData().type === 'sink'
  })
  if (!checkLeaf) {
    window.$message.error(t('project.synchronization_definition.end_node_tips'))
    return
  }

  const nodes = graph.getNodes()
  const checkNode = nodes.every((node) => {
    return !node.getData().unsaved
  })
  if (!checkNode) {
    window.$message.error(
      t('project.synchronization_definition.save_node_tips')
    )
    return
  }

  const edgeCells = graph.getEdges()
  const edges = edgeCells.map((edge: Edge) => {
    return {
      inputPluginId: edge.getSourceCell()?.getData().pluginId,
      targetPluginId: edge.getTargetCell()?.getData().pluginId
    }
  })

  return {
    edges
  }
}
