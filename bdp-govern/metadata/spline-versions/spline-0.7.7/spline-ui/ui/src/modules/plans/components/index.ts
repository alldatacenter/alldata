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

import { AttributeLineageInfoComponent } from './attribute-lineage-info/attribute-lineage-info.component'
import { AttributeInfoComponent } from './attribute-lineage/attribute-info/attribute-info.component'
import { AttributeLineageDialogComponent } from './attribute-lineage/attribute-lineage-dialog/attribute-lineage-dialog.component'
import { OperationInfoComponent } from './operation-info/operation-info.component'
import { PlanHeaderComponent } from './plan-header/plan-header.component'
import { PlanInfoComponent } from './plan-info/plan-info.component'


export const components: any[] = [
    PlanInfoComponent,
    PlanHeaderComponent,
    OperationInfoComponent,
    AttributeLineageInfoComponent,
    AttributeLineageDialogComponent,
    AttributeInfoComponent
]

export * from './plan-info/plan-info.component'
export * from './operation-info/operation-info.component'
export * from './plan-header/plan-header.component'
export * from './attribute-lineage-info/attribute-lineage-info.component'
export * from './attribute-lineage/attribute-lineage-dialog/attribute-lineage-dialog.component'
export * from './attribute-lineage/attribute-info/attribute-info.component'
