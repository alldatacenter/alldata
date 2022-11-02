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

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core'
import { BehaviorSubject } from 'rxjs'

import { DtSecondaryHeaderCellBaseComponent } from '../dt-secondary-header-cell-base.component'


@Component({
    selector: 'dt-secondary-header-cell-default',
    templateUrl: './dt-secondary-header-cell-default.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DtSecondaryHeaderCellDefaultComponent extends DtSecondaryHeaderCellBaseComponent<string, {}> implements OnInit {
    value$ = new BehaviorSubject<string>('');
}

