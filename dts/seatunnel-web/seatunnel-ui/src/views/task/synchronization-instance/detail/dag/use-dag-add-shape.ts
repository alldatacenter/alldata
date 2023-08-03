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

import { DagEdgeName, DagNodeName } from './dag-setting'

const stateColor = {
  failed: {
    fill: '#ffced7',
    stroke: '#ffa8b7'
  },
  running: {
    fill: '#ceebff',
    stroke: '#b0deff'
  },
  finished: {
    fill: '#ceffee',
    stroke: '#a8ffe0'
  },
  canceled: {
    fill: '#d5d5d5',
    stroke: '#b6b6b6'
  }
}

export function useDagAddShape(
  graph: any,
  nodes: any,
  edges: Array<any>,
  t: any
) {
  for (const i in nodes) {
    const group = graph.addNode({
      x: 40,
      y: 40,
      width: 360,
      height: 160,
      zIndex: 1,
      attrs: {
        body:
          String(nodes[i].status.toLowerCase()) === 'failed' &&
          'finished' &&
          'canceled'
            ? stateColor.running
            : stateColor[
                nodes[i].status.toLowerCase() as
                  | 'failed'
                  | 'finished'
                  | 'canceled'
              ]
      }
    })

    group.addTools({
      name: 'button',
      args: {
        markup: [
          {
            tagName: 'text',
            textContent: `pipeline#${nodes[i].pipelineId}`,
            attrs: {
              fill: '#333333',
              'font-size': 14,
              'text-anchor': 'center',
              stroke: 'black'
            }
          },
          {
            tagName: 'text',
            textContent: `${t('project.synchronization_instance.state')}: ${
              nodes[i].status
            }`,
            attrs: {
              fill: '#868686',
              'font-size': 12,
              'text-anchor': 'start',
              x: '7em'
            }
          },
          {
            tagName: 'text',
            textContent: `${t('project.synchronization_instance.read')} ${
              nodes[i].readRowCount
            }${t('project.synchronization_instance.line')}/${t(
              'project.synchronization_instance.write'
            )} ${nodes[i].writeRowCount}${t(
              'project.synchronization_instance.line'
            )}`,
            attrs: {
              fill: '#868686',
              'font-size': 12,
              'text-anchor': 'start',
              x: '20em'
            }
          }
        ],
        x: 0,
        y: 0,
        offset: { x: 0, y: -18 }
      }
    })

    nodes[i].child.forEach((n: any) => {
      group.addChild(
        graph.addNode({
          id: n.id,
          // x: 50,
          // y: 50,
          // width: 120,
          // height: 40,
          shape: DagNodeName,
          // label: n.label,
          zIndex: 10,
          // attrs: {
          //   body: {
          //     stroke: 'none',
          //     fill: '#858585'
          //   },
          //   label: {
          //     fill: '#fff',
          //     fontSize: 12
          //   }
          // },
          data: {
            name: n.label
          }
        })
      )
    })
  }

  edges.forEach((e: any) => {
    graph.addEdge({
      shape: DagEdgeName,
      source: {
        cell: e.source
      },
      target: {
        cell: e.target
      },
      id: e.id
    })
  })
}
