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

import { defineComponent, onMounted, toRefs, ref } from 'vue'
import { NSpace, NCard } from 'naive-ui'
import { useSidebar } from './use-sidebar'
import { useI18n } from 'vue-i18n'
import styles from './index.module.scss'
import SourceImg from '../images/source.png'
import SinkImg from '../images/sink.png'
import FieldMapperImg from '../images/field-mapper.png'
import FilterEventTypeImg from '../images/filter-event-type.png'
import ReplaceImg from '../images/replace.png'
import SplitImg from '../images/spilt.png'
import CopyImg from '../images/copy.png'
import SqlImg from '../images/sql.png'

const DagSidebar = defineComponent({
  name: 'DagSidebar',
  emits: ['dragstart'],
  setup(props, ctx) {
    const businessModel = ref('data-integration')
    const { variables, getConnectorTransformsTypeList } = useSidebar()
    const { t } = useI18n()
    const handleDragstart = (type: string, name?: string) => {
      ctx.emit('dragstart', type, name)
    }

    onMounted(() => {
      getConnectorTransformsTypeList()
    })

    return {
      ...toRefs(variables),
      businessModel,
      t,
      handleDragstart
    }
  },
  render() {
    return (
      <NCard class={styles['task-container']}>
        <NSpace vertical>
          <h3>
            {this.t('project.synchronization_definition.source_and_sink')}
          </h3>
          <div
            class={styles['task-item']}
            draggable='true'
            onDragstart={() => this.handleDragstart('source', 'Source')}
          >
            <NSpace align='center'>
              <img class={styles['task-image']} src={SourceImg} />
              <span>Source</span>
            </NSpace>
          </div>
          <div
            class={styles['task-item']}
            draggable='true'
            onDragstart={() => this.handleDragstart('sink', 'Sink')}
          >
            <NSpace align='center'>
              <img class={styles['task-image']} src={SinkImg} />
              <span>Sink</span>
            </NSpace>
          </div>
          {this.transforms.length > 0 && (
            <h3>{this.t('project.synchronization_definition.transforms')}</h3>
          )}
          {this.businessModel === 'data-integration' &&
            this.transforms.map((t: any) => {
              const item: any = {
                name: t.pluginIdentifier.pluginName
              }

              if (item.name === 'FieldMapper') {
                item.icon = FieldMapperImg
              } else if (item.name === 'FilterRowKind') {
                item.icon = FilterEventTypeImg
              } else if (item.name === 'Replace') {
                item.icon = ReplaceImg
              } else if (item.name === 'MultiFieldSplit') {
                item.icon = SplitImg
              } else if (item.name === 'Copy') {
                item.icon = CopyImg
              } else if (item.name === 'Sql') {
                item.icon = SqlImg
              }

              return (
                <div
                  class={styles['task-item']}
                  draggable='true'
                  onDragstart={() =>
                    this.handleDragstart(
                      'transform',
                      t.pluginIdentifier.pluginName
                    )
                  }
                >
                  <NSpace align='center'>
                    <img class={styles['task-image']} src={item.icon} />
                    <span>{item.name}</span>
                  </NSpace>
                </div>
              )
            })}
        </NSpace>
      </NCard>
    )
  }
})

export { DagSidebar }
