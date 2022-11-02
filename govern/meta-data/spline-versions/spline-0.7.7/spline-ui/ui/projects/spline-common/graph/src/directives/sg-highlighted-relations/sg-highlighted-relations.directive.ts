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


import { AfterViewInit, Directive, forwardRef, Host, Inject, Input, OnChanges, SimpleChanges } from '@angular/core'
import { GraphComponent } from '@swimlane/ngx-graph'

import { SplineGraphComponent } from '../../components'
import { getLinkDomSelector, getNodeDomSelector, SgData } from '../../models'


@Directive({
    selector: '[sgHighlightedRelations]spline-graph',
})
export class SgHighlightedRelationsDirective implements AfterViewInit, OnChanges {

    @Input() graphData: SgData
    @Input() sgHighlightedRelations: string[] | null = null // node ids

    constructor(
        @Host() @Inject(forwardRef(() => SplineGraphComponent)) private splineGraph: SplineGraphComponent,
    ) {
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.sgHighlightedRelations && !changes.sgHighlightedRelations.isFirstChange()) {
            this.applyNodeStyles(changes.sgHighlightedRelations.currentValue, this.splineGraph.ngxGraphComponent)
        }
    }

    ngAfterViewInit(): void {
        this.applyNodeStyles(this.sgHighlightedRelations, this.splineGraph.ngxGraphComponent)
    }

    private applyNodeStyles(highlightedNodes: string[] | null, ngxGraphComponent: GraphComponent): void {

        const highlightedNodesSet = new Set<string>(highlightedNodes)

        const cssClassesMap = {
            highlighted: 'sg-node-relation--highlighted',
            hidden: 'sg-node-relation--hidden',
        }

        const cssClassesList = Object.values(cssClassesMap)

        function applyStylesToDomElm(selector: string,
                                     rootElmRef: HTMLElement,
                                     ifHighlighted: boolean | null,
        ): void {
            const elementRef = rootElmRef.querySelector(selector)
            if (elementRef) {
                // remove all styles
                if (ifHighlighted === null) {
                    cssClassesList
                        .forEach(
                            className => elementRef.classList.remove(className)
                        )
                }
                else {
                    elementRef.classList.add(ifHighlighted ? cssClassesMap.highlighted : cssClassesMap.hidden)
                    elementRef.classList.remove(!ifHighlighted ? cssClassesMap.highlighted : cssClassesMap.hidden)
                }
            }
        }

        ngxGraphComponent.nodes
            .forEach(item =>
                applyStylesToDomElm(
                    getNodeDomSelector(item.id),
                    ngxGraphComponent.chart.nativeElement,
                    highlightedNodes
                        ? highlightedNodesSet.has(item.id)
                        : null
                )
            )

        ngxGraphComponent.links
            .forEach(item =>
                applyStylesToDomElm(
                    getLinkDomSelector(item.id),
                    ngxGraphComponent.chart.nativeElement,
                    highlightedNodes
                        ? highlightedNodesSet.has(item.source) && highlightedNodesSet.has(item.target)
                        : null,
                ),
            )
    }

}
