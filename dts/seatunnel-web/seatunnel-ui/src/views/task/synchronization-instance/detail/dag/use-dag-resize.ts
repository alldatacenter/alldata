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

import { debounce } from 'lodash'
import { useResizeObserver } from '@vueuse/core'
import type { Graph } from '@antv/x6'
import type { Ref } from 'vue'

export function useDagResize(container: Ref<HTMLElement>, graph: Ref<Graph>) {
  const resize = debounce(() => {
    if (container.value) {
      const w = container.value.offsetWidth
      const h = container.value.offsetHeight
      graph.value?.resize(w, h)
    }
  }, 200)

  useResizeObserver(container, resize)
}