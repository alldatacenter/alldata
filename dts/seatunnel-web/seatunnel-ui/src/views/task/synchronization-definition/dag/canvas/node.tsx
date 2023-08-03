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

import { defineComponent, inject, ref } from 'vue'
import { NTooltip } from 'naive-ui'
import styles from './index.module.scss'
import SourceImg from '../images/source.png'
import SinkImg from '../images/sink.png'
import FieldMapperImg from '../images/field-mapper.png'
import FilterEventTypeImg from '../images/filter-event-type.png'
import ReplaceImg from '../images/replace.png'
import SplitImg from '../images/spilt.png'
import CopyImg from '../images/copy.png'
import SqlImg from '../images/sql.png'

const Node = defineComponent({
  name: 'Node',
  setup() {
    const getNode = inject('getNode') as any
    const node = getNode()
    const { name, unsaved, type, connectorType, isError } = node.getData()

    const icon = ref('')

    if (type === 'source') {
      icon.value = SourceImg
    } else if (type === 'sink') {
      icon.value = SinkImg
    } else if (type === 'transform' && connectorType === 'FieldMapper') {
      icon.value = FieldMapperImg
    } else if (type === 'transform' && connectorType === 'FilterRowKind') {
      icon.value = FilterEventTypeImg
    } else if (type === 'transform' && connectorType === 'Replace') {
      icon.value = ReplaceImg
    } else if (type === 'transform' && connectorType === 'MultiFieldSplit') {
      icon.value = SplitImg
    } else if (type === 'transform' && connectorType === 'Copy') {
      icon.value = CopyImg
    } else if (type === 'transform' && connectorType === 'Sql') {
      icon.value = SqlImg
    }

    return () => (
      <div
        class={styles['dag-node']}
        style={{
          borderLeft: isError ? '4px solid #ff4d4f' : (unsaved ? '4px solid #faad14' : '4px solid #1890ff')
        }}
      >
        <img src={icon.value} class={styles['dag-node-icon']} />
        <NTooltip trigger='hover'>
          {{
            trigger: () => <div class={styles['dag-node-label']}>{name}</div>,
            default: () => name
          }}
        </NTooltip>
      </div>
    )
  }
})

export default Node
