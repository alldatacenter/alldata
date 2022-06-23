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

import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'
import { MatIconModule, MatIconRegistry } from '@angular/material/icon'
import { DomSanitizer } from '@angular/platform-browser'

import { SplineIconWithTextComponent } from './components/icon-with-text/spline-icon-with-text.component'
import { SplineIconComponent } from './components/icon/spline-icon.component'
import { SplineIcon } from './models/spline-icon.models'


@NgModule({
    imports: [
        CommonModule,
        MatIconModule,
    ],
    declarations: [
        SplineIconComponent,
        SplineIconWithTextComponent
    ],
    exports: [
        MatIconModule,
        SplineIconComponent,
        SplineIconWithTextComponent
    ],
})
export class SplineIconModule {

    constructor(private readonly matIconRegistry: MatIconRegistry,
                private readonly sanitizer: DomSanitizer) {

        SplineIcon.ICONS.forEach(
            iconName => this.addIcon(iconName),
        )
    }

    private addIcon(iconName: string): void {
        // TODO: move base href calculation to some shared helper
        const BASE_HREF = document?.getElementsByTagName('base')[0]?.attributes['href']?.value || '/'
        const iconsBasePath = `${BASE_HREF}assets/images/icons`


        this.matIconRegistry.addSvgIcon(
            iconName,
            this.sanitizer.bypassSecurityTrustResourceUrl(`${iconsBasePath}/${iconName}.svg`),
        )
    }

}
