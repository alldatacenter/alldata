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

import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core'

import { SplineIcon } from '../../models/spline-icon.models'


@Component({
    selector: 'spline-icon',
    templateUrl: './spline-icon.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplineIconComponent implements OnChanges {

    @Input() icon: SplineIcon.Icon

    @Input() set color(value: SplineIcon.Color) {
        this.styles = {
            ...this.styles,
            color: value
        }
    }

    @Input() set size(value: string) {
        this.styles = {
            ...this.styles,
            width: value,
            fontSize: value,
            height: value,
            lineHeight: value,
        }
    }

    isCustomIcon = false

    styles: Partial<CSSStyleDeclaration> = {
        cursor: 'default'
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.icon && changes.icon.currentValue) {
            this.isCustomIcon = SplineIcon.ICON_SET.has(changes.icon.currentValue)
        }

    }
}
