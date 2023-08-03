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
// import {
//   getUauthorizedClientConfig,
//   getAuthorizedClientConfig
// } from '@/service/client'
// import { useTaskTypeStore } from '@/store/project/task-type'
// import { useClientStore } from '@/store/client'

// export const useClientConfig = () => {
//   const taskTypeStore = useTaskTypeStore()
//   const clientStore = useClientStore()

//   const getUnauthorizedClientInfo = async () => {
//     if (clientStore.getLoaded) {
//       document.title = clientStore.getTitle
//       return
//     }
//     const result = await getUauthorizedClientConfig()

//     clientStore.init({
//       casModel: result['cas.anon.enabled'] === true ? 'cas' : 'common',
//       casLoginUrl: result['cas.anon.login.address'],
//       casLogoutUrl: result['cas.anon.logout.address'],
//       title: result['title.anon.context']
//     })

//     if (result['title.anon.context'])
//       document.title = result['title.anon.context']
//   }

//   const getAuthorizedClientInfo = async () => {
//     const result = await getAuthorizedClientConfig()
//     if (result.task) {
//       taskTypeStore.setTaskTypes(result.task)
//     }
//   }

//   return {
//     getAuthorizedClientInfo,
//     getUnauthorizedClientInfo
//   }
// }
