/*
 * Copyright 2020 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { keyBy, omit } from 'lodash-es'
import { AttributeDataType, AttributeDataTypeArray, AttributeDataTypeStruct, AttributeDtType, AttributeSchema } from 'spline-api'


export namespace SplineAttributesTree {

    export type TreeNode = {
        name: string
        dataType: AttributeDataType
        id?: string
        children?: TreeNode[]
        rootAttributeId?: string
    }

    export type Tree = TreeNode[]

    export function toTree(attributesSchema: AttributeSchema[], dataTypes: AttributeDataType[]): Tree {
        const dataTypesMap = keyBy(dataTypes, 'id')
        return attributesSchema
            .map(attrSchema => {
                const dataType = dataTypesMap[attrSchema.dataTypeId]
                return {
                    id: attrSchema.id,
                    name: attrSchema.name,
                    dataType,
                    children: dataType && getDataTypeChildren(dataType, dataTypesMap, attrSchema.id),
                }
            })
    }

    export function calculateTreeHash(tree: Tree): string {
        const treeNoIds = tree
            .map(node => ({
                ...omit<TreeNode>(node, 'id'),
                dataType: omit<AttributeDataType>(node.dataType, 'id'),
                children: node?.children
                    ? calculateTreeHash(node?.children)
                    : undefined
            }))

        return JSON.stringify(treeNoIds)
    }

    function getDataTypeChildren(dataType: AttributeDataType,
                                 dataTypesMap: Record<string, AttributeDataType>,
                                 rootAttributeId: string): TreeNode[] {
        switch (dataType.type) {
            case AttributeDtType.Array:
                return getDataTypeChildren(
                    dataTypesMap[(dataType as AttributeDataTypeArray).elementDataTypeId], dataTypesMap, rootAttributeId
                )

            case AttributeDtType.Struct:
                return (dataType as AttributeDataTypeStruct).fields
                    .map(field => {
                        const currentDataType = dataTypesMap[field.dataTypeId]
                        return {
                            name: field.name,
                            dataType: currentDataType,
                            children: getDataTypeChildren(currentDataType, dataTypesMap, rootAttributeId),
                            rootAttributeId
                        }
                    })
            default:
                return []
        }
    }
}
