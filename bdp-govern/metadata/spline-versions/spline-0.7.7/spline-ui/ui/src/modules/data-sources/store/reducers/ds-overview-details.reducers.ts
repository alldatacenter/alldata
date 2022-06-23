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

import { DataSourceWriteMode, ExecutionEvent, ExecutionEventField, OperationDetails } from 'spline-api'
import { SplineColors } from 'spline-common'
import { SdWidgetCard, SdWidgetExpansionPanel, SdWidgetSimpleRecord, SdWidgetTitle, SplineDataViewSchema } from 'spline-common/data-view'
import { SdWidgetAttributesTree } from 'spline-shared/attributes'
import { SgEventNodeInfoShared } from 'spline-shared/data-view'
import { DateTimeHelpers, ProcessingStore } from 'spline-utils'

import { DsOverviewDetailsStoreActions } from '../actions'
import ActionTypes = DsOverviewDetailsStoreActions.ActionTypes


export namespace DsOverviewDetailsStore {

    export const STORE_FEATURE_NAME = 'overviewDetails'

    export type State = {
        loading: ProcessingStore.EventProcessingState
        executionEvent: ExecutionEvent
        operationDetails: OperationDetails[]
        attributesSchemasDvs: SplineDataViewSchema
        dataSourceInfoDvs: SplineDataViewSchema
    }

    export function getInitState(): State {
        return {
            loading: ProcessingStore.getDefaultProcessingState(true),
            executionEvent: null,
            operationDetails: [],
            attributesSchemasDvs: [],
            dataSourceInfoDvs: [],
        }
    }

    export function reducer(state = getInitState(),
                            action: DsOverviewDetailsStoreActions.Actions): State {

        switch (action.type) {
            case ActionTypes.Init:
                return {
                    ...state,
                    loading: ProcessingStore.eventProcessingStart(state.loading)
                }

            case ActionTypes.InitSuccess:
                return {
                    ...state,
                    executionEvent: action.payload.executionEvent,
                    operationDetails: action.payload.operationsDetails,
                    attributesSchemasDvs: operationDetailsToAttributesSchemasDvs(action.payload.operationsDetails),
                    dataSourceInfoDvs: toDataSourceInfoSchemasDvs(action.payload.executionEvent),
                    loading: ProcessingStore.eventProcessingFinish(state.loading)
                }

            case ActionTypes.InitError:
                return {
                    ...state,
                    loading: ProcessingStore.eventProcessingFinish(state.loading, action.payload.error)
                }

            case ActionTypes.ResetState:
                return {
                    ...getInitState()
                }
            default:
                return { ...state }
        }

    }

    export function operationDetailsToAttributesSchemasDvs(operationDetails: OperationDetails[]): SplineDataViewSchema {

        const operationsDetailsTreeMap = SgEventNodeInfoShared.toOperationDetailsAttrTreeMap(operationDetails)

        const attrTreeDvs = Object.values(operationsDetailsTreeMap)
            .map(currentOperationsInfoList => {

                // as all schemas are the same we can take just the first operation as a reference to build schema.
                const treeData = currentOperationsInfoList[0].attrTree

                return SdWidgetCard.toContentOnlySchema(
                    SdWidgetAttributesTree.toSchema(
                        treeData,
                        {
                            allowAttrSelection: false,
                        }
                    ),
                )
            })

        return [
            SdWidgetTitle.toSchema('Attributes Schema'),
            ...attrTreeDvs
        ]
    }

    export function toDataSourceInfoSchemasDvs(executionEvent: ExecutionEvent): SplineDataViewSchema {
        return SdWidgetExpansionPanel.toSchema(
            {
                icon: 'description',
                iconColor: SplineColors.BLUE,
                title: 'Details'
            },
            [
                SdWidgetSimpleRecord.toSchema([
                    {
                        label: 'DATA_SOURCES.DETAILS__CREATED_AT',
                        value: DateTimeHelpers.toString(executionEvent.executedAt)
                    },
                    {
                        label: 'DATA_SOURCES.DETAILS__WRITE_MODE',
                        value: executionEvent.writeMode === DataSourceWriteMode.Append
                            ? 'Append'
                            : 'Overwrite',
                    },
                    {
                        label: 'DATA_SOURCES.DETAILS__EXECUTION_EVENT',
                        value: executionEvent[ExecutionEventField.applicationName],
                        routerLink: ['/events/overview', executionEvent[ExecutionEventField.executionEventId]]
                    },
                    {
                        label: 'DATA_SOURCES.DETAILS__EXECUTION_PLAN',
                        value: executionEvent[ExecutionEventField.executionPlanId],
                        routerLink: ['/plans/overview', executionEvent[ExecutionEventField.executionPlanId]]
                    }
                ])
            ]
        )
    }
}
