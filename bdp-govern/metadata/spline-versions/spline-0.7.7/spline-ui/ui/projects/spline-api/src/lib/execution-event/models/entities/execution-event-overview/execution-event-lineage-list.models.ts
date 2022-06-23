/*
 * Copyright 2021 ABSA Group Limited
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


import { DataSourceWriteMode, SplineDataSourceInfo, uriToDatasourceInfo } from '../data-source'

import { ExecutionEventLineageNodeType } from './execution-event-lineage-node-type.models'
import { ExecutionEventLineageNode } from './execution-event-lineage-node.models'
import { ExecutionEventLineage } from './execution-event-lineage-overview.models'


export namespace ExecutionEventLineageList {

    export enum LineageRecordField {
        id = 'id',
        dataSource = 'dataSource',
        executionEventName = 'executionEventName',
        writeMode = 'writeMode',
        executedAt = 'executedAt',
        executionPlanId = 'executionPlanId',
        dataSourceType = 'dataSourceType',
    }

    export type BaseRecord = {
        [LineageRecordField.id]: string
        [LineageRecordField.dataSource]: SplineDataSourceInfo
    }

    export type DataSourceRecord = BaseRecord

    export type EventRecord =
        & BaseRecord
        &
        {
            [LineageRecordField.executionPlanId]?: string
            [LineageRecordField.executionEventName]?: string
            [LineageRecordField.writeMode]?: DataSourceWriteMode
            [LineageRecordField.executedAt]?: Date
        }

    export type LineageRecord = DataSourceRecord & EventRecord

    export function toLineageList(lineage: ExecutionEventLineage, showInputs: boolean = true): LineageRecord[] {
        const executionEventNodes = lineage.nodes.filter(node => node.type === ExecutionEventLineageNodeType.Execution)
        return executionEventNodes
            .sort()
            .reduce(
                (acc, currentNode) => [...acc, ...extractEventLineageList(lineage, currentNode, showInputs)],
                []
            )
    }

    export function toDataSourceRecord(node: ExecutionEventLineageNode): DataSourceRecord {
        return {
            id: node.id,
            dataSource: uriToDatasourceInfo(node.name)
        }
    }

    export function toEventRecord(eventNode: ExecutionEventLineageNode, targetDataSourceNode: ExecutionEventLineageNode): EventRecord {
        return {
            ...toDataSourceRecord(targetDataSourceNode),
            [LineageRecordField.id]: eventNode.id,
            [LineageRecordField.executionPlanId]: eventNode.id,
            [LineageRecordField.executionEventName]: eventNode.name,
            [LineageRecordField.writeMode]: DataSourceWriteMode.Append,
            [LineageRecordField.executedAt]: new Date()
        }
    }

    export function extractEventLineageList(lineage: ExecutionEventLineage,
                                            eventNode: ExecutionEventLineageNode,
                                            showInputs: boolean = true): LineageRecord[] {
        const targetDataSourceNodeId = lineage.links.find(x => x.source === eventNode.id).target
        const targetDataSourceNode = lineage.nodes.find(x => x.id === targetDataSourceNodeId)

        const eventRecord = toEventRecord(eventNode, targetDataSourceNode)

        if (showInputs) {
            const inputDataSourceNodesIds = lineage.links
                .filter(x => x.target === eventNode.id)
                .map(link => link.source)

            const inputDataSourceNodes = lineage.nodes.filter(x => inputDataSourceNodesIds.includes(x.id))

            return [
                ...inputDataSourceNodes.map(toDataSourceRecord),
                eventRecord
            ]
        }

        return [eventRecord]
    }

}
