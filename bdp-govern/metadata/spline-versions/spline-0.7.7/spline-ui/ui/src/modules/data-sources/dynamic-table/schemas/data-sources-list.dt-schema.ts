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

import { ExecutionEvent, ExecutionEventField } from 'spline-api'
import {
    CommonDtCellValueControlEvent,
    DtCellDateTime,
    DtCellLink,
    DtLayoutBuilder,
    DtLayoutSize,
    DynamicTableDataMap
} from 'spline-common/dynamic-table'
import { SplineDataSourceSharedDtSchema } from 'spline-shared/dynamic-table'


export namespace DataSourcesListDtSchema {

    export enum Column {
        applicationName = ExecutionEventField.applicationName,
        executionPlanId = ExecutionEventField.executionPlanId,
        dataSourceUri = ExecutionEventField.dataSourceUri,
        dataSourceName = ExecutionEventField.dataSourceName,
        dataSourceType = ExecutionEventField.dataSourceType,
        writeMode = ExecutionEventField.append,
        timestamp = ExecutionEventField.timestamp,
    }

    export enum CellEvent {
        OpenDsStateDetails = 'OpenDsStateDetails'
    }

    export class OpenDsStateDetailsEvent extends CommonDtCellValueControlEvent<{ id: string }> {
        readonly type: CellEvent.OpenDsStateDetails
    }

    export function getSchema(): DynamicTableDataMap<Column> {
        return [
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.dataSourceInfo.name,
                        routerLink: ['/data-sources/overview', rowData.dataSourceInfo.id],
                        description: rowData.dataSourceInfo.uri
                    }),
                    {
                        style: DtCellLink.LinkStyle.Main
                    }
                ),
                id: Column.dataSourceName,
                header: 'DATA_SOURCES.DS_LIST__COL__DATA_SOURCE',
                isSortable: true,
            },
            {
                ...SplineDataSourceSharedDtSchema.getWriteModeColSchema(
                    (rowData: ExecutionEvent) => rowData.writeMode
                ),
                id: Column.writeMode,
            },
            {
                id: Column.dataSourceType,
                header: 'DATA_SOURCES.DS__COL__TYPE',
                isSortable: true,
                layout: (new DtLayoutBuilder)
                    .setWidth('100px')
                    .visibleAfter(DtLayoutSize.lg)
                    .toLayout()
            },
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.applicationName,
                        routerLink: ['/events/overview', rowData.executionEventId],
                        description: rowData.applicationId
                    })
                ),
                id: Column.applicationName,
                header: 'DATA_SOURCES.DS_LIST__COL__EXECUTION_EVENT',
                isSortable: true,
                layout: (new DtLayoutBuilder())
                    .visibleAfter(DtLayoutSize.lg)
                    .setWidth('250px')
                    .toLayout(),
            },
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.executionPlanId,
                        routerLink: ['/plans/overview', rowData.executionPlanId],
                    })
                ),
                id: Column.executionPlanId,
                header: 'DATA_SOURCES.DS_LIST__COL__EXECUTION_PLAN',
                layout: (new DtLayoutBuilder())
                    .visibleAfter(DtLayoutSize.xl)
                    .setWidth('150px')
                    .toLayout(),
                isSortable: true,
            },
            {
                ...DtCellDateTime.getDefaultColSchema(),
                id: Column.timestamp,
                header: 'DATA_SOURCES.DS_LIST__COL__LAST_MODIFIED_AT',
                isSortable: true,
            },
        ]
    }

}
