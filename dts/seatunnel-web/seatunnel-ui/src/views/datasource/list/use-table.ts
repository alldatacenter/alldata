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
  datasourceList,
  datasourceDelete
} from '@/service/data-source'
import type { DatasourceList } from '@/service/data-source/types'
import type { ResponseTable } from '@/service/types'

export function useTable() {
  const data = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0,
    searchVal: '',
    list: []
  })

  const getList = (pluginName = '') => {
    datasourceList({
      pageNo: data.page,
      pageSize: data.pageSize,
      searchVal: data.searchVal,
      pluginName
    }).then((res: any) => {
      data.list = res.data
      data.itemCount = res.totalCount
    })
  }

  const updateList = () => {
    if (data.list.length === 1 && data.page > 1) {
      --data.page
    }
    getList()
  }

  const deleteRecord = async (id: string) => {
    await datasourceDelete(id)
    updateList()
  }

  const changePage = (page: number) => {
    data.page = page
    getList()
  }

  const changePageSize = (pageSize: number) => {
    data.page = 1
    data.pageSize = pageSize
    getList()
  }

  return { data, getList, changePage, changePageSize, deleteRecord, updateList }
}
