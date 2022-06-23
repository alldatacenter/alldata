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
import { CommonDtCellValueControlEvent, DtCellLink, DtLayoutBuilder, DtLayoutSize, DynamicTableDataMap } from 'spline-common/dynamic-table'
import { SplineDataSourceSharedDtSchema } from 'spline-shared/dynamic-table'
import { DateTimeHelpers } from 'spline-utils'
import LinkStyle = DtCellLink.LinkStyle


export namespace DsStateHistoryDtSchema {

    export enum Column {
        applicationName = ExecutionEventField.applicationName,
        executionPlanId = ExecutionEventField.executionPlanId,
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
                        title: DateTimeHelpers.toString(rowData.executedAt, DateTimeHelpers.FULL_DATE),
                        emitCellEvent: (currentRowData: ExecutionEvent) =>
                            new OpenDsStateDetailsEvent({
                                id: currentRowData.dataSourceInfo.id
                            }),
                        description: DateTimeHelpers.toString(rowData.executedAt, DateTimeHelpers.FULL_TIME),
                    }),
                    {
                        style: LinkStyle.Main
                    }
                ),
                id: Column.timestamp,
                header: 'DATA_SOURCES.DS_STATE_HISTORY__COL__TIMESTAMP',
                isSortable: true,
                layout: (new DtLayoutBuilder)
                    .alignCenter()
                    .setWidth('160px')
                    .toLayout()
            },
            {
                ...SplineDataSourceSharedDtSchema.getWriteModeColSchema(
                    (rowData: ExecutionEvent) => rowData.writeMode
                ),
                id: Column.writeMode,
                layout: (new DtLayoutBuilder(SplineDataSourceSharedDtSchema.getWriteModeDefaultLayout()))
                    .setCSSClass([])
                    .toLayout()
            },
            {
                id: Column.dataSourceType,
                header: 'DATA_SOURCES.DS_STATE_HISTORY__COL__TYPE',
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
                        title: rowData.applicationName,
                        routerLink: ['/events/overview', rowData.executionEventId],
                        description: rowData.applicationId
                    })
                ),
                id: Column.applicationName,
                header: 'DATA_SOURCES.DS_STATE_HISTORY__COL__APPLICATION',
                isSortable: true,
            },
            {
                ...DtCellLink.getColSchema(
                    (rowData: ExecutionEvent) => ({
                        title: rowData.executionPlanId,
                        routerLink: ['/plans/overview', rowData.executionPlanId],
                    })
                ),
                id: Column.executionPlanId,
                header: 'DATA_SOURCES.DS_STATE_HISTORY__COL__EXECUTION_PLAN',
                layout: (new DtLayoutBuilder())
                    .visibleAfter(DtLayoutSize.lg)
                    .toLayout(),
                isSortable: true,
            },
        ]
    }

}
