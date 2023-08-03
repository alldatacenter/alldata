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

import {
  querySyncTaskInstanceDetail,
  querySyncTaskInstanceDag
} from '@/service/sync-task-instance'
import { useRoute } from 'vue-router'
import { uuid } from '@/common/common'
import { useDagAddShape } from '@/views/task/synchronization-instance/detail/dag/use-dag-add-shape'
import { useDagLayout } from '@/views/task/synchronization-instance/detail/dag/use-dag-layout'
import { Graph } from '@antv/x6'
import { getDefinitionConfig } from '@/service/sync-task-definition'

export function useTaskDefinition(t: any) {
  const route = useRoute()

  const getJobConfig = async () => {
    return await getDefinitionConfig(route.params.taskCode as string)
  }

  const getJobDag = async (graph: Graph) => {
    const dagData = await querySyncTaskInstanceDag({
      jobInstanceId: route.query.jobInstanceId
    })

    const pipelineData = await querySyncTaskInstanceDetail({
      jobInstanceId: route.query.jobInstanceId
    })

    if (Object.keys(dagData).length < 1 || pipelineData.length < 1) {
      return false
    }

    const obj: any = {
      nodes: {},
      edges: []
    }

    // 给每一个节点分配一个唯一id
    for (const i in dagData.vertexInfoMap) {
      dagData.vertexInfoMap[i]['id'] = uuid(String(new Date().getTime()))
    }

    // 通过pipeline中的inputVertexId和targetVertexId查询vertexInfoMap中的节点id
    for (const i in dagData.pipelineEdges) {
      dagData.pipelineEdges[i].forEach((l: any) => {
        obj.edges.push({
          id: uuid(String(new Date().getTime())),
          source: dagData.vertexInfoMap[l.inputVertexId].id,
          target: dagData.vertexInfoMap[l.targetVertexId].id
        })
      })
    }

    // 用pipelineData进行分组整合数据
    pipelineData.forEach((p: any) => {
      const nodes = dagData.pipelineEdges[p.pipelineId]
        .map((p: any) => [p.inputVertexId, p.targetVertexId])
        .flat(2)

      obj.nodes['group-' + p.pipelineId] = {
        ...p,
        child: nodes.map((n: any) => {
          return {
            id: dagData.vertexInfoMap[n].id,
            label: dagData.vertexInfoMap[n].connectorType,
            nodeType: dagData.vertexInfoMap[n].type,
            vertexId: dagData.vertexInfoMap[n].vertexId
          }
        })
      }
    })

    useDagAddShape(graph, obj.nodes, obj.edges, t)
    useDagLayout(graph)
  }

  return {
    getJobConfig,
    getJobDag
  }
}
