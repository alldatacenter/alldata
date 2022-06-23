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

import { OperationAttributeLineageType } from 'spline-api'
import { SgLegend } from 'spline-shared/graph'


import { LINEAGE_TYPE_COLOR_MAP } from './attribute-lineage.models'


export const LINAGE_TYPE_LEGENDS_MAP: Readonly<Record<OperationAttributeLineageType, SgLegend>>
    = Object.freeze<Record<OperationAttributeLineageType, SgLegend>>({
        [OperationAttributeLineageType.Usage]: {
            id: OperationAttributeLineageType.Usage,
            title: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__USAGE__TITLE',
            description: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__USAGE__DESCRIPTION',
            color: LINEAGE_TYPE_COLOR_MAP[OperationAttributeLineageType.Usage],
        },
        [OperationAttributeLineageType.Lineage]: {
            id: OperationAttributeLineageType.Lineage,
            title: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__LINEAGE__TITLE',
            description: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__LINEAGE__DESCRIPTION',
            color: LINEAGE_TYPE_COLOR_MAP[OperationAttributeLineageType.Lineage],
        },
        [OperationAttributeLineageType.Impact]: {
            id: OperationAttributeLineageType.Impact,
            title: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__IMPACT__TITLE',
            description: 'PLANS.ATTRIBUTE_LINEAGE__LEGEND__IMPACT__DESCRIPTION',
            color: LINEAGE_TYPE_COLOR_MAP[OperationAttributeLineageType.Impact],
        },
    })

export function getAttributeLineageTypeLegend(type: OperationAttributeLineageType): SgLegend {
    return LINAGE_TYPE_LEGENDS_MAP[type]
}
