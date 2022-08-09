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

import { SlBreadcrumbsComponent } from './breadcrumbs/sl-breadcrumbs.component'
import { SlContentHeaderComponent } from './content-header/sl-content-header.component'
import { SlContentSidebarDialogComponent } from './content-sidebar-dialog/sl-content-sidebar-dialog.component'
import { SlEntityHeaderComponent } from './entity-header/sl-entity-header.component'
import { SplineFakePageContentComponent } from './fake-page-content/spline-fake-page-content.component'
import { SplineLayoutHeaderLogoComponent } from './header-logo/spline-layout-header-logo.component'
import { SplineLayoutCommonComponent } from './layout/spline-layout-common.component'
import { SlSidebarLogoComponent } from './sidebar-logo/sl-sidebar-logo.component'


export const layoutComponents: any[] = [
    SplineLayoutCommonComponent,
    SplineLayoutHeaderLogoComponent,
    SlEntityHeaderComponent,
    SlContentSidebarDialogComponent,
    SplineFakePageContentComponent,
    SlContentHeaderComponent,
    SlSidebarLogoComponent,
    SlBreadcrumbsComponent
]

export * from './public-api'
