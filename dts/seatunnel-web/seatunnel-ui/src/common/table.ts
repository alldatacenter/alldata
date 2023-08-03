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

import { useSettingStore } from '@/store/setting'

const getTableColumn = (data: Array<{ key: string; title: string }>) => {
  const tableColumn = []
  const settingStore = useSettingStore()

  settingStore.getSequenceColumn &&
    tableColumn.push({
      title: '#',
      key: 'index',
      render: (row: any, index: number) => index + 1
    })

  settingStore.getDataUniqueValue &&
    tableColumn.push(
      ...data.map((i) => {
        return {
          title: i.title,
          key: i.key
        }
      })
    )

  return tableColumn
}

export { getTableColumn }
