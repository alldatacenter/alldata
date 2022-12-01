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

import { AfterViewInit, Component, forwardRef, Host, Inject, OnDestroy } from '@angular/core'
import { Subscription } from 'rxjs'

import { SgLayoutSettings, switchGraphOrientation } from '../../models'
import { SplineGraphComponent } from '../graph/spline-graph.component'


@Component({
    selector: 'sg-control-panel',
    templateUrl: './sg-control-panel.component.html',
})
export class SgControlPanelComponent implements AfterViewInit, OnDestroy {

    readonly zoomLevelStep = 0.1

    currentZoomLevel = 1

    private _subscription = new Subscription

    constructor(@Host() @Inject(forwardRef(() => SplineGraphComponent)) private splineGraph: SplineGraphComponent) {
    }

    ngAfterViewInit(): void {

        this.currentZoomLevel = this.splineGraph.ngxGraphComponent.zoomLevel

        this._subscription.add(
            this.splineGraph.ngxGraphComponent.zoomChange
                .subscribe(
                    zoomLevel => {
                        this.currentZoomLevel = zoomLevel
                    },
                ))
    }

    onCenterGraphBtnClicked(): void {
        this.splineGraph.ngxGraphComponent.zoomToFit()
        this.splineGraph.ngxGraphComponent.center()
    }

    onZoomIn(): void {
        const zoomFactor = 1 + this.zoomLevelStep
        this.splineGraph.ngxGraphComponent.zoom(zoomFactor)
    }

    onZoomOut(): void {
        const zoomFactor = 1 - this.zoomLevelStep
        this.splineGraph.ngxGraphComponent.zoom(zoomFactor)
    }

    onChangeGraphDirectionBtnClicked(): void {
        const orientation = switchGraphOrientation(this.splineGraph.layoutSettings.orientation)

        this.splineGraph.layoutSettings = {
            ...this.splineGraph.layoutSettings,
            orientation,
        } as SgLayoutSettings
    }

    ngOnDestroy(): void {
        this._subscription.unsubscribe()
    }
}
