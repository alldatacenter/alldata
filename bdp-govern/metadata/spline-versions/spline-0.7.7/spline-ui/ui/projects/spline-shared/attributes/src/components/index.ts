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

import { SplineAttributeDataTypeComponent } from './attribute-data-type/spline-attribute-data-type.component'
import { SdWidgetAttributesTreeComponent } from './attributes-tree-widget/sd-widget-attributes-tree.component'
import { SplineAttributesTreeComponent } from './attributes-tree/spline-attributes-tree.component'
import { SplineAttributeSearchComponent } from './search/spline-attribute-search.component'


export * from './public-api'

export const attributesComponents = [
    SplineAttributeSearchComponent,
    SplineAttributeDataTypeComponent,
    SplineAttributesTreeComponent,
    SdWidgetAttributesTreeComponent
]
