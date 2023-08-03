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
import { DagEdgeName } from './dag-setting'

export function useDagGraph(
  graph: any,
  dagContainer: HTMLElement,
  minimapContainer: HTMLElement
) {
  return new Graph({
    container: dagContainer,
    scroller: true,
    grid: {
      size: 10,
      visible: true
    },
    connecting: {
      router: 'manhattan',
      allowBlank: false,
      allowLoop: false,
      allowNode: false,
      snap: true,
      createEdge() {
        return graph.value?.createEdge({ shape: DagEdgeName })
      },
      validateConnection(data) {
        const { sourceCell, targetCell } = data
        if (targetCell?.getData().type === 'source') return false
        if (targetCell?.getData().type === 'sink') {
          return graph.value?.getConnectedEdges(targetCell).length < 1
        }
        
        if (targetCell?.getData().type === 'transform') {
          // The same 'Copy' transform node cannot be connected
          const srcData = sourceCell?.getData(), tgtData = targetCell?.getData()
          if (srcData.type === 'transform' && srcData.connectorType === 'Copy' && tgtData.connectorType === 'Copy') return false

          // don't connect self
          const edges = graph.value?.getConnectedEdges(targetCell)
          return !edges.some((edge: Edge) => {
            return edge.getTargetCellId() === targetCell.id
          })
        }

        return true
      }
    },
    snapline: true,
    minimap: {
      enabled: true,
      width: 200,
      height: 120,
      container: minimapContainer
    },
    selecting: {
      enabled: true,
      rubberband: false,
      movable: true,
      showNodeSelectionBox: true,
      showEdgeSelectionBox: true
    }
  })
}
