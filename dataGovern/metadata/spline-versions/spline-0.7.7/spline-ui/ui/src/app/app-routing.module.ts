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

import { NgModule } from '@angular/core'
import { RouterModule, Routes } from '@angular/router'
import { SplineConfigResolver } from 'spline-shared'

import { AppNotFoundComponent } from './pages/not-found/not-found.component'


const routes: Routes = [
    {
        path: '',
        resolve: [
            SplineConfigResolver
        ],
        children: [
            {
                path: 'events',
                loadChildren: () => import('../modules/events/spline-events.module').then(m => m.SplineEventsModule),
            },
            {
                path: 'plans',
                loadChildren: () => import('../modules/plans/spline-plans.module').then(m => m.SplinePlansModule),
            },
            {
                path: 'data-sources',
                loadChildren: () => import('../modules/data-sources/spline-data-sources.module').then(m => m.SplineDataSourcesModule),
            },
            {
                path: '',
                redirectTo: 'events',
                pathMatch: 'full',
            },
            {
                path: '**',
                redirectTo: '404',
            },
            {
                path: '404',
                component: AppNotFoundComponent,
            },
        ]
    },
    {
        path: '**',
        redirectTo: '404',
    },
]

@NgModule({
    // useHash supports github.io demo page, remove in your app
    imports: [
        RouterModule.forRoot(routes, {
            useHash: false,
            scrollPositionRestoration: 'top',
            relativeLinkResolution: 'legacy',
            onSameUrlNavigation: 'reload'
        }),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule {
}
