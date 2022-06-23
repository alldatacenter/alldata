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
import { ExecutionEvent, OperationDetails } from 'spline-api'


export namespace DsOverviewDetailsStoreActions {

    export enum ActionTypes {
        Init = '[DS Overview :: Details] :: Init',
        InitSuccess = '[DS Overview :: Details] :: Init :: Success',
        InitError = '[DS Overview :: Details] :: Init :: Error',
        ResetState = '[DS Overview] :: ResetState'
    }

    export class Init implements Action {

        readonly type = ActionTypes.Init

        constructor(public payload: { executionEvent: ExecutionEvent }) {

        }
    }

    export class InitSuccess implements Action {

        readonly type = ActionTypes.InitSuccess

        constructor(public payload: { operationsDetails: OperationDetails[]; executionEvent: ExecutionEvent }) {

        }
    }

    export class InitError implements Action {

        readonly type = ActionTypes.InitError

        constructor(public payload: { error: any }) {

        }
    }

    export class ResetState implements Action {

        readonly type = ActionTypes.ResetState;

        constructor() {

        }
    }

    export type Actions =
        | Init
        | InitSuccess
        | InitError
        | ResetState
}
