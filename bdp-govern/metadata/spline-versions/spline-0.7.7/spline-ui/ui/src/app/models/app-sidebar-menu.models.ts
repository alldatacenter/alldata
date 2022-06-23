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

import { SplineSidebarMenu } from '../components'


export namespace AppSidebarMenu {

    export function getSidebarMenu(): SplineSidebarMenu.MenuItem[] {
        return [
            {
                label: 'APP.MENU__EVENTS',
                icon: 'play_arrow',
                routerLink: '/events/list',
                routerLinkActivePattern: '^$|^\\/$|^\\/events.*'
            },
            {
                label: 'APP.MENU__DATA_SOURCES',
                icon: 'description',
                routerLink: '/data-sources/list',
                routerLinkActivePattern: '^\\/data-sources.*'
            },
            {
                label: 'APP.MENU__PLANS',
                icon: 'cog-transfer-outline',
                routerLink: '/plans/list',
                routerLinkActivePattern: '^$|^\\/$|^\\/plans.*'
            }
        ]
    }

}
