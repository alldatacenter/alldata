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
import { DtCellDateTime, DtCellElapsedTime, DtCellLink, DynamicTableDataMap } from 'spline-common/dynamic-table'
import { SplineDataSourceSharedDtSchema } from 'spline-shared/dynamic-table'


export namespace EventsListDtSchema {

    export enum Column {
        applicationName = ExecutionEventField.applicationName,
        executionPlanId = ExecutionEventField.executionPlanId,
        dataSourceName = ExecutionEventField.dataSourceName,
        dataSourceType = ExecutionEventField.dataSourceType,
        writeMode = ExecutionEventField.append,
        timestamp = ExecutionEventField.timestamp,
        duration = ExecutionEventField.duration,
    }

    export function getSchema(): DynamicTableDataMap<Column> {
        return [
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.applicationName,
                        routerLink: ['/events/overview', rowData.executionEventId],
                        description: rowData.applicationId
                    }),
                    {
                        style: DtCellLink.LinkStyle.Main
                    }
                ),
                id: Column.applicationName,
                header: 'EVENTS.EVENTS_LIST__COL__APPLICATION',
                isSortable: true
            },
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.executionPlanId,
                        routerLink: ['/plans/overview', rowData.executionPlanId],
                    })
                ),
                id: Column.executionPlanId,
                header: 'EVENTS.EVENTS_LIST__COL__EXECUTION_PLAN',
                isSortable: true,
                layout: {
                    classes: ['d-xxl-flex d-none']
                },
                headerClasses: ['d-xxl-flex d-none'],
            },
            {
                ...SplineDataSourceSharedDtSchema.getWriteModeColSchema(
                    (rowData: ExecutionEvent) => rowData.writeMode
                ),
                id: Column.writeMode,
            },
            {
                id: Column.dataSourceType,
                header: 'EVENTS.EVENTS_LIST__COL__TYPE',
                isSortable: true,
                layout: {
                    styles: {
                        maxWidth: '100px'
                    },
                    classes: ['d-lg-flex d-none']
                },
                headerClasses: ['d-lg-flex d-none'],
            },
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.dataSourceInfo.name,
                        routerLink: ['/data-sources/overview', rowData.dataSourceInfo.id],
                        description: rowData.dataSourceInfo.uri
                    })
                ),
                id: Column.dataSourceName,
                header: 'EVENTS.EVENTS_LIST__COL__DESTINATION',
                isSortable: true,
                layout: {
                    classes: ['d-lg-flex d-none']
                },
                headerClasses: ['d-lg-flex d-none'],
            },
            {
                ...DtCellDateTime.getDefaultColSchema(),
                id: Column.timestamp,
                header: 'EVENTS.EVENTS_LIST__COL__TIMESTAMP',
                isSortable: true
            },
            {
                ...DtCellElapsedTime.getDefaultColSchema(),
                id: Column.duration,
                header: 'EVENTS.EVENTS_LIST__COL__ELAPSED_TIME',
                isSortable: true,
            },
        ]
    }

}
