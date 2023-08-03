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

import { Graph, Cell } from '@antv/x6'
import { DagreLayout, GridLayout } from '@antv/layout'
import _ from 'lodash'
import { uuid } from '@/common/common'
import { DagNodeName, DagEdgeName, PortGroupsConfig } from './dag-setting'
import type { InputEdge, NodeType } from '../types'

export function addNode(graph: Graph, cell: Cell.Metadata) {
  const id = uuid(String(new Date().getTime()))
  const nodeShape = {
    id,
    ...cell,
    zIndex: 1,
    shape: DagNodeName,
    ports: {
      groups: PortGroupsConfig,
      items: getPorts(id, cell.node)
    },
    data: {
      type: cell.node,
      unsaved: true,
      isError: false,
      name: cell.label,
      pluginId: id,
      connectorType: cell.node === 'transform' ? cell.label : ''
    }
  } as Cell.Metadata
  ;(graph as Graph).addNode(nodeShape)
}

export function initNodesAndEdges(
  graph: Graph,
  cells: Cell.Metadata[],
  edges: InputEdge[]
) {
  graph.addNodes(
    cells.map((cell: Cell.Metadata) => ({
      shape: DagNodeName,
      node: cell.type.toLowerCase(),
      id: cell.pluginId,
      label: cell.name,
      ports: {
        groups: PortGroupsConfig,
        items: getPorts(cell.pluginId, cell.type.toLowerCase())
      },
      data: {
        ...cell,
        isError: false,
        type: cell.type.toLowerCase()
      }
    }))
  )
  graph.addEdges(
    edges.map((edge) => ({
      shape: DagEdgeName,
      source: {
        cell: edge.inputPluginId,
        port: edge.inputPluginId + '-out-port'
      },
      target: {
        cell: edge.targetPluginId,
        port: edge.targetPluginId + '-in-port'
      }
    }))
  )
  formatLayout(graph, 'dagre')
}

export function updateNode(graph: Graph) {
  graph.resetCells(graph.getCells())
}

export function formatLayout(
  graph: any,
  formatType: 'grid' | 'dagre' = 'dagre',
  cols?: number,
  rows?: number
) {
  let layoutFunc = null
  const layoutConfig: any = {
    nodesep: 50,
    padding: 50,
    ranksep: 50,
    type: formatType
  }

  if (formatType === 'grid') {
    layoutConfig['cols'] = cols
    layoutConfig['rows'] = rows
  }

  if (!graph) {
    return
  }

  graph.cleanSelection()

  if (layoutConfig.type === 'dagre') {
    layoutFunc = new DagreLayout({
      type: 'dagre',
      rankdir: 'LR',
      align: 'UL',
      // Calculate the node spacing based on the edge label length
      ranksepFunc: (d) => {
        const edges = graph.getOutgoingEdges(d.id)
        let max = 0
        if (edges && edges.length > 0) {
          edges.forEach((edge: any) => {
            const edgeView = graph.findViewByCell(edge)
            const labelView = edgeView?.findAttr(
              'width',
              _.get(edgeView, ['labelSelectors', '0', 'body'], null)
            )
            const labelWidth = labelView ? +labelView : 0
            max = Math.max(max, labelWidth)
          })
        }
        return layoutConfig.ranksep + max
      },
      nodesep: layoutConfig.nodesep,
      controlPoints: true
    })
  } else if (layoutConfig.type === 'grid') {
    layoutFunc = new GridLayout({
      type: 'grid',
      preventOverlap: true,
      preventOverlapPadding: layoutConfig.padding,
      sortBy: '_index',
      rows: layoutConfig.rows || undefined,
      cols: layoutConfig.cols || undefined,
      nodeSize: 220
    })
  }

  const json = graph.toJSON()
  const nodes = json.cells
    .filter((cell: any) => cell.shape === DagNodeName)
    .map((item: any) => {
      return {
        ...item,
        // sort by code aesc
        _index: -(item.id as string)
      }
    })

  const edges = json.cells.filter((cell: any) => cell.shape === DagEdgeName)

  const newModel: any = layoutFunc?.layout({
    nodes,
    edges
  } as any)
  graph.fromJSON(newModel)
}

function getPorts(id: string, type: NodeType) {
  if (type === 'source') {
    return [
      {
        id: id + '-out-port',
        group: 'out'
      }
    ]
  }
  if (type === 'sink') {
    return [
      {
        id: id + '-in-port',
        group: 'in'
      }
    ]
  }
  if (type === 'transform') {
    return [
      {
        id: id + '-out-port',
        group: 'out'
      },
      {
        id: id + '-in-port',
        group: 'in'
      }
    ]
  }
  return []
}
