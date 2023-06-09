/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import { GlobalState, IHistoryPathInfoItem, UserInfo } from '@/types/common.type'
import { defineStore } from 'pinia'

const state = (): GlobalState => ({
  userInfo: {
    userName: ''
    // token: ''
  } as UserInfo,
  isShowTablesMenu: false,
  historyPathInfo: {
    path: '',
    query: {}
  }
})

const getters = {
  // necessary to display the secondary navigation of tables
  getShowTablesMenu(state: GlobalState): boolean {
    return state.isShowTablesMenu
  }
}

export default defineStore('datalake', {
  state,
  getters,
  actions: {
    updateTablesMenu(data: boolean): void {
      this.isShowTablesMenu = data
    },
    updateUserInfo(data: UserInfo): void {
      this.userInfo = data
    },
    setHistoryPath(data: IHistoryPathInfoItem): void {
      this.historyPathInfo = data
    }
  }
})
