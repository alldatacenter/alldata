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

import { Action } from '@ngrx/store'
import { SplineDataSourceInfo } from 'spline-api'


export namespace DsOverviewStoreActions {

    export enum ActionTypes {
        InitRequest = '[DS Overview] :: Init :: Request',
        InitSuccess = '[DS Overview] :: Init :: Success',
        InitError = '[DS Overview] :: Init :: Error',
        ResetState = '[DS Overview] :: ResetState'
    }

    export class InitRequest implements Action {

        readonly type = ActionTypes.InitRequest;

        constructor(public payload: { dataSourceId: string }) {

        }
    }

    export class InitSuccess implements Action {

        readonly type = ActionTypes.InitSuccess;

        constructor(public payload: { dataSourceInfo: SplineDataSourceInfo }) {

        }
    }

    export class InitError implements Action {

        readonly type = ActionTypes.InitError;

        constructor(public payload: { dataSourceId: string; error: any }) {

        }
    }

    export class ResetState implements Action {

        readonly type = ActionTypes.ResetState;

        constructor() {

        }
    }

    export type Actions =
        | InitRequest
        | InitSuccess
        | InitError
        | ResetState
}
