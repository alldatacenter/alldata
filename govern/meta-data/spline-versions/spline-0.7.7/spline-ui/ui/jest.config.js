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

module.exports = {
    'preset': 'jest-preset-angular',
    'setupFilesAfterEnv': [
        '<rootDir>/setup-jest.ts'
    ],
    'transformIgnorePatterns': [
        'node_modules/(?!@ngrx)'
    ],
    "testResultsProcessor": "jest-teamcity-reporter",
    'coverageReporters': [
        'text',
        'html',
        'cobertura',
        'teamcity'
    ],
    'moduleNameMapper': {
        '^lodash-es$': __dirname + '/node_modules/lodash/index.js',
        '/spline-api/': __dirname + '/projects/spline-api/src/public-api',
        '^spline-api$': __dirname + '/projects/spline-api/src/public-api',
        '/spline-common/': __dirname + '/projects/spline-common/main/src/public-api',
        '^spline-common$': __dirname + '/projects/spline-common/main/src/public-api',
        '/spline-common\/data-view/': __dirname + '/projects/spline-common/data-view/src/public-api',
        '^spline-common\/data-view$': __dirname + '/projects/spline-common/data-view/src/public-api',
        '/spline-common\/graph/': __dirname + '/projects/spline-common/graph/src/public-api',
        '^spline-common\/graph$': __dirname + '/projects/spline-common/graph/src/public-api',
        '/spline-common\/layout/': __dirname + '/projects/spline-common/layout/src/public-api',
        '^spline-common\/layout$': __dirname + '/projects/spline-common/layout/src/public-api',
        '/spline-common\/dynamic-table/': __dirname + '/projects/spline-common/dynamic-table/src/public-api',
        '^spline-common\/dynamic-table$': __dirname + '/projects/spline-common/dynamic-table/src/public-api',
        '/spline-common\/dynamic-filter\/filter-controls/': __dirname + '/projects/spline-common/dynamic-filter/filter-controls/src/public-api',
        '^spline-common\/dynamic-filter\/filter-controls$': __dirname + '/projects/spline-common/dynamic-filter/filter-controls/src/public-api',
        '/spline-common\/dynamic-filter/': __dirname + '/projects/spline-common/dynamic-filter/main/src/public-api',
        '^spline-common\/dynamic-filter$': __dirname + '/projects/spline-common/dynamic-filter/main/src/public-api',
        '/spline-shared/': __dirname + '/projects/spline-shared/main/src/public-api',
        '^spline-shared$': __dirname + '/projects/spline-shared/main/src/public-api',
        '/spline-shared\/attributes/': __dirname + '/projects/spline-shared/attributes/src/public-api',
        '^spline-shared\/attributes$': __dirname + '/projects/spline-shared/attributes/src/public-api',
        '/spline-shared\/expression/': __dirname + '/projects/spline-shared/expression/src/public-api',
        '^spline-shared\/expression$': __dirname + '/projects/spline-shared/expression/src/public-api',
        '/spline-shared\/graph/': __dirname + '/projects/spline-shared/graph/src/public-api',
        '^spline-shared\/graph$': __dirname + '/projects/spline-shared/graph/src/public-api',
        '/spline-shared\/events/': __dirname + '/projects/spline-shared/events/src/public-api',
        '^spline-shared\/events$': __dirname + '/projects/spline-shared/events/src/public-api',
        '/spline-shared\/plans/': __dirname + '/projects/spline-shared/plans/src/public-api',
        '^spline-shared\/plans$': __dirname + '/projects/spline-shared/plans/src/public-api',
        '/spline-shared\/dynamic-table/': __dirname + '/projects/spline-shared/dynamic-table/main/src/public-api',
        '^spline-shared\/dynamic-table$': __dirname + '/projects/spline-shared/dynamic-table/main/src/public-api',
        '/spline-utils/': __dirname + '/projects/spline-utils/main/src/public-api',
        '^spline-utils$': __dirname + '/projects/spline-utils/main/src/public-api',
        '/spline-utils\/translate/': __dirname + '/projects/spline-utils/translate/src/public-api',
        '^spline-utils\/translate$': __dirname + '/projects/spline-utils/translate/src/public-api',
    },
    'resolver': null,
    'globals': {
        'ts-jest': {
            tsConfig: '<rootDir>/tsconfig.spec.json'
        }
    }
};
