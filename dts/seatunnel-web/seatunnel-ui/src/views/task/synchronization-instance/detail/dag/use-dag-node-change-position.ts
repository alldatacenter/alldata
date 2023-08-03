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

import { Graph } from '@antv/x6'

const ctrlPressed = false
const embedPadding = 20

export function useDagNodeChangePosition(graph: Graph) {
  graph.on('node:change:position', ({ node, options }) => {
    if (options.skipParentHandler || ctrlPressed) {
      return
    }

    const children = node.getChildren()
    if (children && children.length) {
      node.prop('originPosition', node.getPosition())
    }

    const parent = node.getParent()
    if (parent && parent.isNode()) {
      let originSize = parent.prop('originSize')
      if (originSize == null) {
        originSize = parent.getSize()
        parent.prop('originSize', originSize)
      }

      let originPosition = parent.prop('originPosition')
      if (originPosition == null) {
        originPosition = parent.getPosition()
        parent.prop('originPosition', originPosition)
      }

      let x = originPosition.x
      let y = originPosition.y
      let cornerX = originPosition.x + originSize.width
      let cornerY = originPosition.y + originSize.height
      let hasChange = false

      const children = parent.getChildren()
      if (children) {
        children.forEach((child) => {
          const bbox = child.getBBox().inflate(embedPadding)
          const corner = bbox.getCorner()

          if (bbox.x < x) {
            x = bbox.x
            hasChange = true
          }

          if (bbox.y < y) {
            y = bbox.y
            hasChange = true
          }

          if (corner.x > cornerX) {
            cornerX = corner.x
            hasChange = true
          }

          if (corner.y > cornerY) {
            cornerY = corner.y
            hasChange = true
          }
        })
      }

      if (hasChange) {
        parent.prop(
          {
            position: { x, y },
            size: { width: cornerX - x, height: cornerY - y }
          },
          { skipParentHandler: true }
        )
      }
    }
  })
}
