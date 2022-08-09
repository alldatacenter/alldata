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

import { Component, OnDestroy, OnInit } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { Observable } from 'rxjs'
import { distinctUntilChanged, map } from 'rxjs/operators'
import { SplineTabsNavBar } from 'spline-common'
import { SlBreadcrumbs } from 'spline-common/layout'
import { BaseComponent } from 'spline-utils'

import { DsOverviewStoreFacade } from '../../services'
import { DsOverviewStore } from '../../store'

import NavTabInfo = SplineTabsNavBar.NavTabInfo


@Component({
    selector: 'data-sources-overview-page',
    templateUrl: './data-source-overview.page.component.html',
    styleUrls: ['./data-source-overview.page.component.scss'],

})
export class DataSourceOverviewPageComponent extends BaseComponent implements OnInit, OnDestroy {

    readonly state$: Observable<DsOverviewStore.State>
    readonly breadcrumbs$: Observable<SlBreadcrumbs.Breadcrumbs>

    readonly headerNavTabs: NavTabInfo[] = [
        {
            label: 'Overview',
            routeLink: './overview',
            icon: 'description'
        },
        {
            label: 'DATA_SOURCES.DS_OVERVIEW__NAV_TAB__HISTORY',
            routeLink: './history',
            icon: 'history'
        },
        {
            label: 'Lineage',
            routeLink: './lineage',
            icon: 'timeline'
        },
        {
            label: 'Impact',
            routeLink: './impact',
            icon: 'air'
        },
    ]


    constructor(private readonly activatedRoute: ActivatedRoute,
                private readonly store: DsOverviewStoreFacade) {
        super()

        this.state$ = store.state$

        // TODO: Use some generic system for breadcrumbs definition.
        this.breadcrumbs$ = store.state$
            .pipe(
                map(state => state.dataSourceInfo.name),
                distinctUntilChanged(),
                map(name => [
                    {
                        label: 'Data Sources'
                    },
                    {
                        label: name
                    },
                    {
                        label: 'History'
                    },
                ])
            )
    }

    ngOnInit(): void {
        const dataSourceId = this.activatedRoute.snapshot.params['dataSourceId']
        this.store.init(dataSourceId)
    }

    ngOnDestroy(): void {
        super.ngOnDestroy()
        this.store.unInit()
    }
}
