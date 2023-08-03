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

import { defineComponent, onMounted, Ref, ref } from 'vue'
import { NGi, NGrid, NCard, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { Graph } from '@antv/x6'
import { useDagGraph } from './dag/use-dag-graph'
import { useDagResize } from './dag/use-dag-resize'
import { useDagNodeChangePosition } from './dag/use-dag-node-change-position'
import { DagEdgeName, DagNodeName } from './dag/dag-setting'
import { useDagEdge } from './dag/use-dag-edge'
import { useTaskDefinition } from './use-task-definition'
import styles from './task-definition.module.scss'
import { useDagNode } from './dag/use-dag-node'

interface IJobConfig {
  name: string
  engine: string
  description: string
  env: Array<{ [key: string]: string }>
}

const TaskDefinition = defineComponent({
  name: 'TaskDefinition',
  setup() {
    const { t } = useI18n()
    const container = ref()
    const dagContainer = ref()
    const minimapContainer = ref()
    const jobConfig = ref<IJobConfig>({} as IJobConfig)
    const graph = ref<Graph>()
    const { getJobConfig, getJobDag } = useTaskDefinition(t)

    const initGraph = () => {
      graph.value = useDagGraph(
        graph,
        dagContainer.value,
        minimapContainer.value
      )
    }

    useDagResize(container, graph as Ref<Graph>)

    const registerNode = () => {
      Graph.unregisterNode(DagNodeName)
      Graph.registerNode(DagNodeName, useDagNode())
    }

    const registerEdge = () => {
      Graph.unregisterEdge(DagEdgeName)
      Graph.registerEdge(DagEdgeName, useDagEdge())
    }

    const formatData = () => {
      const obj = jobConfig.value.env
      const arr = []
      for (const i in obj) {
        arr.push({ label: String(i), value: obj[i] })
      }
      return arr
    }

    onMounted(async () => {
      initGraph()
      registerNode()
      registerEdge()
      useDagNodeChangePosition(graph.value as Graph)
      jobConfig.value = (await getJobConfig()) || {}

      await getJobDag(graph.value as Graph)
    })

    return {
      t,
      container,
      'dag-container': dagContainer,
      'minimap-container': minimapContainer,
      formatData,
      jobConfig
    }
  },
  render() {
    return (
      <NGrid x-gap='12'>
        <NGi span='20'>
          <NCard class={styles['left-panel']}>
            <div class={styles['workflow-dag']}>
              <div ref='container' class={styles.container}>
                <div ref='dag-container' class={styles['dag-container']} />
                <div ref='minimap-container' class={styles.minimap} />
              </div>
            </div>
          </NCard>
        </NGi>
        <NGi span='4'>
          <NCard class={styles['right-panel']}>
            <NSpace vertical>
              <div>
                <h4>{this.t('project.synchronization_instance.task_name')}</h4>
                <p>{this.jobConfig.name}</p>
              </div>
              <div>
                <h4>
                  {this.t('project.synchronization_instance.description')}
                </h4>
                <p>{this.jobConfig.description}</p>
              </div>
              <div>
                <h4>{this.t('project.synchronization_instance.engine')}</h4>
                <p>{this.jobConfig.engine}</p>
              </div>
              {this.formatData().map((i: any) => (
                <div>
                  <h4>{i.label}</h4>
                  <p>{i.value}</p>
                </div>
              ))}
            </NSpace>
          </NCard>
        </NGi>
      </NGrid>
    )
  }
})

export { TaskDefinition }
