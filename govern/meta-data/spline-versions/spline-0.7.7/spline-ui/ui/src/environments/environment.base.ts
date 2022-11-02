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

import * as config from '../../package.json'

import { DEPLOYMENT_PREFIX, Environment, RELATIVE_URL } from './shared'


declare const __SPLINE_UI_BUILD_REVISION__: string
declare const __SPLINE_UI_BUILD_TIMESTAMP__: string

const URL = RELATIVE_URL + DEPLOYMENT_PREFIX

export const environmentBase: Environment = {
    production: false,
    version: config.version,
    buildRevision: __SPLINE_UI_BUILD_REVISION__,
    buildTimestamp: __SPLINE_UI_BUILD_TIMESTAMP__,
    projectPagesHref: 'https://absaoss.github.io/spline',
    copyright: '© 2019 ABSA Group Limited',
    license: {
        name: 'Apache License, Version 2.0.',
        href: 'https://www.apache.org/licenses/LICENSE-2.0'
    },
    deploymentPrefix: DEPLOYMENT_PREFIX,
    url: URL,
    key: null,
}
