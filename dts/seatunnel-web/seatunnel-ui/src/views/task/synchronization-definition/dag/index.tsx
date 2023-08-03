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
import { defineComponent, ref, onMounted } from 'vue'
import { DagSidebar } from './sidebar'
import { DagCanvas } from './canvas'
import { DagToolbar } from './toolbar'
import { NSpace, NSpin } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useDagDetail } from './use-dag-detail'
import styles from './index.module.scss'

const SynchronizationDefinitionDag = defineComponent({
  name: 'SynchronizationDefinitionDag',
  setup() {
    const dagRef = ref()

    const tempNode = {
      type: '',
      name: ''
    }
    const { t } = useI18n()
    const { state, detailInit, onDelete, onSave } = useDagDetail()
    const handelDragstart = (type: any, name: any) => {
      tempNode.type = type
      tempNode.name = name
    }

    const handelDrop = (e: DragEvent) => {
      if (!tempNode.type) return
      dagRef.value.addNode({
        x: e.offsetX,
        y: e.offsetY,
        label: tempNode.name || tempNode.type,
        node: tempNode.type
      })
    }

    const handleDelete = async () => {
      const cells = dagRef.value.getSelectedCells()
      if (!cells.length) {
        window.$message.warning(
          t('project.synchronization_definition.delete_empty_tips')
        )
        return
      }
      let result = true
      if (cells[0].isNode() && !cells[0].getData().unsaved) {
        result = await onDelete(cells[0].getData().pluginId)
      }
      if (result) dagRef.value.removeCell(cells[0].id)
    }

    const handleSave = () => {
      const result = dagRef.value.getDagData()
      result && onSave(dagRef.value.getDagData(), dagRef.value.getGraph())
    }

    const handleLayout = (
      layoutType: 'grid' | 'dagre',
      cols: number,
      rows: number
    ) => {
      dagRef.value.layoutDag(layoutType, cols, rows)
    }

    onMounted(async () => {
      const result = await detailInit()
      if (result?.nodesAndEdges) {
        dagRef.value.addNodesAndEdges(
          result.nodesAndEdges.plugins,
          result.nodesAndEdges.edges
        )
      }
    })

    return () => (
      <NSpin show={state.loading}>
        <NSpace vertical>
          <DagToolbar
            onDelete={handleDelete}
            onSave={handleSave}
            onLayout={handleLayout}
          />
          <div class={styles['workflow-dag']}>
            <DagSidebar onDragstart={handelDragstart} />
            <DagCanvas onDrop={handelDrop} ref={dagRef} />
          </div>
        </NSpace>
      </NSpin>
    )
  }
})

export default SynchronizationDefinitionDag
