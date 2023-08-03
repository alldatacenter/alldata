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

import { reactive } from 'vue'
import {
  getDefinitionConfig,
  getDefinitionDetail,
  getDefinitionNodesAndEdges,
  saveTaskDefinitionDag,
  deleteTaskDefinitionDag,
  checkDatabaseAndTable
} from '@/service/sync-task-definition'
import { useRoute, useRouter } from 'vue-router'
import { useSynchronizationDefinitionStore } from '@/store/synchronization-definition'
import { useMessage } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import _ from 'lodash'
import type { InputPlugin, InputEdge } from './types'

export const useDagDetail = () => {
  const route = useRoute()
  const router = useRouter()
  const message = useMessage()
  const { t } = useI18n()
  const dagStore = useSynchronizationDefinitionStore()
  const state = reactive({
    loading: false
  })

  const detailInit = async () => {
    if (state.loading) return
    state.loading = true
    try {
      const [nodesAndEdges, dagConfig, dagInfo] = await Promise.all([
        getDefinitionNodesAndEdges(route.params.jobDefinitionCode as string),
        getDefinitionConfig(route.params.jobDefinitionCode as string),
        getDefinitionDetail(route.params.jobDefinitionCode as string)
      ])

      const checkedNodesAndEdges = nodesAndEdges
        ? await Promise.all(
            nodesAndEdges.plugins.map(async (p: any) => {
              if (p.type === 'SOURCE') {
                return {
                  ...(await checkDatabaseAndTable(
                    String(p.dataSourceId),
                    p.tableOption
                  )),
                  pluginId: p.pluginId,
                  name: p.name
                }
              }
            })
          )
        : null

      checkedNodesAndEdges
        ? (nodesAndEdges.plugins = nodesAndEdges.plugins.map((pn: any) => {
            if (pn.type === 'SOURCE') {
              checkedNodesAndEdges.map((pc: any) => {
                if (pc && pn.pluginId === pc.pluginId) {
                  if (pc.databases.length > 0) {
                    message.warning(
                      `(${pc.name}-${
                        pc.pluginId
                      })[${pc.databases.toString()}] ${t(
                        'project.synchronization_definition.database_exception_message'
                      )}`,
                      { closable: true, duration: 0 }
                    )
                  }
                  if (pc.tables.length > 0) {
                    message.warning(
                      `(${pc.name}-${pc.pluginId})[${pc.tables.toString()}] ${t(
                        'project.synchronization_definition.table_exception_message'
                      )}`,
                      { closable: true, duration: 0 }
                    )
                  }
                  _.pullAll(pn.tableOption.databases, pc.databases)
                  _.pullAll(pn.tableOption.tables, pc.tables)
                }
              })
            }
            return pn
          }))
        : null

      dagStore.setDagInfo({ ...dagConfig, ...dagInfo })
      return { nodesAndEdges: nodesAndEdges }
    } finally {
      state.loading = false
    }
  }

  const onDelete = async (id: string): Promise<boolean> => {
    try {
      await deleteTaskDefinitionDag(
        route.params.jobDefinitionCode as string,
        id
      )
      return true
    } catch (err) {
      return false
    }
  }

  const onSave = async (
    jobDag: {
      plugins: InputPlugin[]
      edges: InputEdge[]
    },
    graph: any
  ): Promise<boolean> => {
    if (state.loading) return false
    state.loading = true
    try {
      const result = await saveTaskDefinitionDag(
        route.params.jobDefinitionCode as string,
        jobDag
      )

      if (result) {
        const node = graph.getCellById(result.pluginId)
        node.getData().isError = true
        node.getData().schemaError = result.schemaError
        graph.resetCells(graph.getCells())

        window.$message.error(JSON.stringify(result.schemaError || ''), {
          closable: true,
          duration: 0
        })
      } else {
        router.push({
          name: 'synchronization-definition',
          query: {
            project: route.query.project,
            global: route.query.global
          }
        })
      }

      state.loading = false
      return true
    } catch (err) {
      state.loading = false
      return false
    }
  }

  return { state, detailInit, onDelete, onSave }
}
