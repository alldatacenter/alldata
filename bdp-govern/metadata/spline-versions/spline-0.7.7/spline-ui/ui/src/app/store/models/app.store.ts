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

import { environment } from '@env/environment'
import moment from 'moment'

import { SplineSidebarMenu } from '../../components'
import { AppSidebarMenu } from '../../models'
import { NamedHref } from 'spline-utils'


export namespace AppStore {

    export interface State {
        isInitialized: boolean
        appVersion: string | null
        buildRevision: string | null
        buildDate: Date | null
        projectPages: NamedHref
        copyright: string
        license: NamedHref
        isEmbeddedMode: boolean
        isSideNavExpanded: boolean
        sidebarMenuItems: SplineSidebarMenu.MenuItem[]
    }

    export function getDefaultState(): State {
        return {
            isInitialized: false,
            appVersion: environment.version,
            buildRevision: environment.buildRevision ?? null,
            buildDate: environment.buildTimestamp ? moment(environment.buildTimestamp).toDate() : null,
            projectPages: {
                name: environment.projectPagesHref,
                href: environment.projectPagesHref
            },
            copyright: environment.copyright,
            license: environment.license,
            isEmbeddedMode: false,
            isSideNavExpanded: false,
            sidebarMenuItems: AppSidebarMenu.getSidebarMenu(),
        }
    }

    export function reduceSideNavExpanded(state: State, isExpanded: boolean): State {
        return {
            ...state,
            isSideNavExpanded: isExpanded
        }
    }


}
